package com.agrogestao.pro.data.report

import android.content.Context
import com.agrogestao.pro.data.local.entities.ReportHistoryEntity
import com.agrogestao.pro.domain.CreditReportSnapshot
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.UUID

enum class ReportIntegrityStatus {
    VALID,
    MISSING,
    MODIFIED
}

data class ArchivedCreditReport(
    val history: ReportHistoryEntity,
    val file: File
)

object CreditReportArchiveStore {
    fun create(
        context: Context,
        ownerUserId: String,
        report: CreditReportSnapshot,
        createdAtEpochMillis: Long = System.currentTimeMillis()
    ): ArchivedCreditReport {
        require(ownerUserId.isNotBlank()) { "Conta do relatório não identificada." }
        val consent = requireNotNull(report.consentProof) {
            "Consentimento do produtor não registrado."
        }
        require(consent.isValid) { "Consentimento do produtor inválido ou desatualizado." }
        val reportId = UUID.randomUUID().toString()
        val ownerDirectoryName = sha256(ownerUserId.toByteArray()).take(24)
        val relativeDirectory = ownerDirectoryName
        val outputDirectory = safeFile(context, relativeDirectory)
        check(outputDirectory.exists() || outputDirectory.mkdirs()) {
            "Não foi possível preparar o histórico de relatórios."
        }

        val datePart = report.generatedDate.replace(Regex("[^0-9-]"), "")
            .ifBlank { "atual" }
        val fileName = "relatorio-credito-rural-$datePart-${reportId.take(8)}.pdf"
        val file = CreditReportPdfGenerator.generate(
            context = context,
            report = report,
            outputDirectory = outputDirectory,
            outputFileName = fileName
        )
        val digest = sha256(file)

        return ArchivedCreditReport(
            history = ReportHistoryEntity(
                reportId = reportId,
                ownerUserId = ownerUserId,
                fileName = fileName,
                relativePath = "$relativeDirectory/$fileName",
                createdAtEpochMillis = createdAtEpochMillis,
                generatedDate = report.generatedDate,
                fromDate = report.criteria.fromDate,
                toDate = report.criteria.toDate,
                income = report.financialSummary.income,
                expenses = report.financialSummary.expenses,
                balance = report.financialSummary.balance,
                isComplete = report.completeness.isComplete,
                missingItems = report.completeness.missingItems.joinToString("\n"),
                sha256 = digest,
                fileSizeBytes = file.length(),
                reportFormatVersion = report.formatVersion,
                consentVersion = consent.consentVersion,
                consentAcceptedAtEpochMillis = consent.acceptedAtEpochMillis
            ),
            file = file
        )
    }

    fun resolve(context: Context, history: ReportHistoryEntity): File =
        safeFile(context, history.relativePath)

    fun verify(context: Context, history: ReportHistoryEntity): ReportIntegrityStatus {
        val file = resolve(context, history)
        if (!file.isFile) return ReportIntegrityStatus.MISSING
        if (file.length() != history.fileSizeBytes) return ReportIntegrityStatus.MODIFIED
        return if (sha256(file).equals(history.sha256, ignoreCase = true)) {
            ReportIntegrityStatus.VALID
        } else {
            ReportIntegrityStatus.MODIFIED
        }
    }

    fun deleteFile(context: Context, history: ReportHistoryEntity): Boolean {
        val file = resolve(context, history)
        return !file.exists() || file.delete()
    }

    private fun safeFile(context: Context, relativePath: String): File {
        require(relativePath.isNotBlank()) { "Caminho do relatório inválido." }
        val root = File(context.filesDir, REPORTS_DIRECTORY).canonicalFile
        val candidate = File(root, relativePath).canonicalFile
        val rootPrefix = root.path + File.separator
        require(candidate.path.startsWith(rootPrefix)) {
            "Caminho do relatório fora da área protegida."
        }
        return candidate
    }

    private fun sha256(file: File): String = FileInputStream(file).use { input ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (count > 0) digest.update(buffer, 0, count)
        }
        digest.digest().toHex()
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).toHex()

    private fun ByteArray.toHex(): String = joinToString("") { byte ->
        "%02x".format(byte.toInt() and 0xff)
    }

    private const val REPORTS_DIRECTORY = "reports"
}

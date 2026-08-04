package com.agrogestao.pro.data.report

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.agrogestao.pro.data.local.entities.TransactionType
import com.agrogestao.pro.domain.CreditReportSnapshot
import com.agrogestao.pro.domain.formatDateForDisplay
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CreditReportPdfGenerator {
    fun generate(
        context: Context,
        report: CreditReportSnapshot,
        outputDirectory: File = File(context.cacheDir, "reports"),
        outputFileName: String? = null
    ): File {
        require(report.criteria.isValid) { "Período do relatório inválido." }
        check(outputDirectory.exists() || outputDirectory.mkdirs()) {
            "Não foi possível preparar a pasta do relatório."
        }

        val safeDate = report.generatedDate.replace(Regex("[^0-9-]"), "")
            .ifBlank { "atual" }
        val fileName = outputFileName ?: "relatorio-credito-rural-$safeDate.pdf"
        require(
            fileName.endsWith(".pdf", ignoreCase = true) &&
                fileName == File(fileName).name
        ) { "Nome de arquivo do relatório inválido." }
        val destination = File(outputDirectory, fileName)
        val document = PdfDocument()

        try {
            CreditReportPdfRenderer(document).render(report)
            FileOutputStream(destination).use(document::writeTo)
        } catch (error: Throwable) {
            destination.delete()
            throw error
        } finally {
            document.close()
        }

        check(destination.isFile && destination.length() > 0L) {
            "O PDF não foi criado corretamente."
        }
        return destination
    }
}

private class CreditReportPdfRenderer(
    private val document: PdfDocument
) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val currency = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private var currentPage: PdfDocument.Page? = null
    private lateinit var canvas: Canvas
    private var pageNumber = 0
    private var cursorY = CONTENT_TOP

    fun render(report: CreditReportSnapshot) {
        require(report.formatVersion > 0) { "Versão do relatório inválida." }
        val consent = requireNotNull(report.consentProof) {
            "Consentimento do produtor não registrado."
        }
        require(consent.isValid) { "Consentimento do produtor inválido ou desatualizado." }
        startPage()
        drawDocumentTitle(report)
        drawCompleteness(report)
        drawConsent(report)
        drawProducer(report)
        drawCrops(report)
        drawFinancialSummary(report)
        drawTransactions(report)
        drawTraceability(report)
        drawDisclaimer(report)
        finishPage()
    }

    private fun drawDocumentTitle(report: CreditReportSnapshot) {
        drawText(
            "RESUMO PRODUTIVO E FINANCEIRO",
            size = 16f,
            color = GREEN,
            bold = true,
            bottomSpacing = 5f
        )
        drawText(
            "Apoio informativo para conversa sobre crédito rural",
            size = 10.5f,
            color = DARK_GRAY,
            bottomSpacing = 11f
        )
        drawLabelValue("Emitido em", formatDateForDisplay(report.generatedDate))
        drawLabelValue(
            "Período financeiro",
            "${formatDateForDisplay(report.criteria.fromDate)} a " +
                formatDateForDisplay(report.criteria.toDate)
        )
        drawLabelValue("Versão do formato", report.formatVersion.toString())
        spacer(8f)
    }

    private fun drawCompleteness(report: CreditReportSnapshot) {
        val complete = report.completeness.isComplete
        ensureSpace(if (complete) 48f else 78f)
        val boxTop = cursorY
        val statusColor = if (complete) GREEN else ORANGE
        paint.color = if (complete) PALE_GREEN else PALE_ORANGE
        canvas.drawRoundRect(
            MARGIN,
            boxTop,
            PAGE_WIDTH - MARGIN,
            boxTop + if (complete) 42f else 70f,
            8f,
            8f,
            paint
        )
        cursorY += 17f
        drawText(
            if (complete) "CADASTRO COMPLETO PARA ESTE RESUMO" else "CADASTRO INCOMPLETO",
            size = 10.5f,
            color = statusColor,
            bold = true,
            horizontalPadding = 10f,
            bottomSpacing = 3f,
            skipSpaceCheck = true
        )
        if (complete) {
            drawText(
                "Todos os campos usados pelo relatório estão preenchidos.",
                size = 9f,
                color = DARK_GRAY,
                horizontalPadding = 10f,
                bottomSpacing = 8f,
                skipSpaceCheck = true
            )
        } else {
            drawText(
                "Faltam: ${report.completeness.missingItems.joinToString(", ")}.",
                size = 9f,
                color = DARK_GRAY,
                horizontalPadding = 10f,
                bottomSpacing = 8f,
                skipSpaceCheck = true
            )
        }
        cursorY = maxOf(cursorY, boxTop + if (complete) 48f else 76f)
    }

    private fun drawProducer(report: CreditReportSnapshot) {
        drawSection("2. PRODUTOR E PROPRIEDADE")
        val producer = report.producer
        drawLabelValue("Produtor", producer?.nomeProdutor.orMissing())
        drawLabelValue("Propriedade", producer?.nomePropriedade.orMissing())
        drawLabelValue("Município / estado", producer?.municipioUF.orMissing())
        drawLabelValue("CAF / DAP", producer?.dAPouCAF.orMissing())
        drawLabelValue(
            "Área total",
            producer?.areaTotalHectares
                ?.takeIf { it > 0.0 }
                ?.let { "${formatDecimal(it)} ha" }
                ?: "Não informada"
        )
    }

    private fun drawCrops(report: CreditReportSnapshot) {
        drawSection("3. SAFRAS CADASTRADAS")
        if (report.crops.isEmpty()) {
            drawText("Nenhuma safra cadastrada.", color = DARK_GRAY)
            return
        }

        report.crops.sortedBy { it.nomeCultura.lowercase() }.forEach { crop ->
            ensureSpace(34f)
            drawText(
                "${crop.nomeCultura} - ${formatDecimal(crop.areaHectares)} ha",
                size = 10f,
                color = BLACK,
                bold = true,
                bottomSpacing = 2f
            )
            drawText(
                "Início: ${formatDateForDisplay(crop.dataInicio)} | " +
                    "Colheita prevista: ${formatDateForDisplay(crop.previsaoColheita)} | " +
                    "Progresso informado: ${crop.progressoPercentual}%",
                size = 8.5f,
                color = DARK_GRAY,
                bottomSpacing = 7f
            )
        }
    }

    private fun drawFinancialSummary(report: CreditReportSnapshot) {
        drawSection("4. RESUMO FINANCEIRO DO PERÍODO")
        drawMoney("Receitas registradas", report.financialSummary.income, GREEN)
        drawMoney("Despesas registradas", report.financialSummary.expenses, RED)
        drawMoney(
            "Saldo operacional registrado",
            report.financialSummary.balance,
            if (report.financialSummary.balance >= 0.0) GREEN else RED
        )
        drawText(
            "O saldo é a diferença entre entradas e saídas cadastradas. Não representa renda " +
                "comprovada nem lucro contábil.",
            size = 8.5f,
            color = DARK_GRAY,
            bottomSpacing = 5f
        )
    }

    private fun drawTransactions(report: CreditReportSnapshot) {
        drawSection("5. MOVIMENTAÇÕES INCLUÍDAS")
        if (report.transactions.isEmpty()) {
            drawText("Nenhuma movimentação encontrada no período selecionado.", color = DARK_GRAY)
            return
        }

        report.transactions.sortedWith(compareBy({ it.data }, { it.id })).forEach { transaction ->
            ensureSpace(35f)
            val type = if (transaction.tipo == TransactionType.ENTRADA) "Entrada" else "Saída"
            val color = if (transaction.tipo == TransactionType.ENTRADA) GREEN else RED
            drawText(
                "${formatDateForDisplay(transaction.data)} | $type | ${currency.format(transaction.valor)}",
                size = 9.5f,
                color = color,
                bold = true,
                bottomSpacing = 2f
            )
            drawText(
                "${transaction.descricao.ifBlank { "Sem descrição" }} - " +
                    transaction.categoria.ifBlank { "Sem categoria" },
                size = 8.5f,
                color = DARK_GRAY,
                bottomSpacing = 7f
            )
        }
    }

    private fun drawTraceability(report: CreditReportSnapshot) {
        drawSection("6. ORIGEM E SINCRONIZAÇÃO DOS DADOS")
        drawLabelValue("Origem", report.dataOrigin)
        drawLabelValue(
            "Registros incluídos",
            "${report.syncSummary.totalRecords} no total; " +
                "${report.syncSummary.syncedRecords} sincronizado(s) com a nuvem; " +
                "${report.syncSummary.localOrPendingRecords} local(is) ou pendente(s)"
        )
        drawText(
            "A sincronização informa onde o registro está salvo, mas não comprova a veracidade " +
                "do conteúdo informado.",
            size = 8.5f,
            color = DARK_GRAY,
            bottomSpacing = 6f
        )
    }

    private fun drawDisclaimer(report: CreditReportSnapshot) {
        drawSection("AVISO IMPORTANTE")
        drawText(
            report.disclaimer,
            size = 9f,
            color = RED,
            bold = true,
            bottomSpacing = 8f
        )
    }

    private fun drawConsent(report: CreditReportSnapshot) {
        val consent = requireNotNull(report.consentProof)
        drawSection("1. CONSENTIMENTO E FINALIDADE")
        drawLabelValue("Versão do consentimento", consent.consentVersion.toString())
        drawLabelValue(
            "Consentimento registrado em",
            SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR"))
                .format(Date(consent.acceptedAtEpochMillis))
        )
        drawText(
            consent.purpose,
            size = 8.8f,
            color = DARK_GRAY,
            bottomSpacing = 6f
        )
    }

    private fun drawSection(title: String) {
        ensureSpace(36f)
        cursorY += 5f
        paint.color = PALE_GREEN
        canvas.drawRoundRect(
            MARGIN,
            cursorY,
            PAGE_WIDTH - MARGIN,
            cursorY + 24f,
            5f,
            5f,
            paint
        )
        cursorY += 16f
        drawText(
            title,
            size = 10.5f,
            color = GREEN,
            bold = true,
            horizontalPadding = 8f,
            bottomSpacing = 10f,
            skipSpaceCheck = true
        )
    }

    private fun drawLabelValue(label: String, value: String) {
        drawText(
            "$label: $value",
            size = 9.5f,
            color = BLACK,
            bottomSpacing = 3f
        )
    }

    private fun drawMoney(label: String, amount: Double, color: Int) {
        drawText(
            "$label: ${currency.format(amount)}",
            size = 11f,
            color = color,
            bold = true,
            bottomSpacing = 4f
        )
    }

    private fun drawText(
        text: String,
        size: Float = 9.5f,
        color: Int = BLACK,
        bold: Boolean = false,
        horizontalPadding: Float = 0f,
        bottomSpacing: Float = 4f,
        skipSpaceCheck: Boolean = false
    ) {
        configurePaint(size, color, bold)
        val lineHeight = size * 1.35f
        val maxWidth = CONTENT_WIDTH - horizontalPadding * 2
        val lines = wrapText(text, maxWidth)
        lines.forEach { line ->
            if (!skipSpaceCheck) ensureSpace(lineHeight + bottomSpacing)
            canvas.drawText(line, MARGIN + horizontalPadding, cursorY, paint)
            cursorY += lineHeight
        }
        cursorY += bottomSpacing
    }

    private fun wrapText(text: String, maxWidth: Float): List<String> {
        if (text.isBlank()) return listOf("")
        val result = mutableListOf<String>()
        var remaining = text.trim()
        while (remaining.isNotEmpty()) {
            val fitting = paint.breakText(remaining, true, maxWidth, null)
                .coerceAtLeast(1)
            if (fitting >= remaining.length) {
                result += remaining
                break
            }
            val candidate = remaining.substring(0, fitting)
            val split = candidate.lastIndexOf(' ').takeIf { it > 0 } ?: fitting
            result += remaining.substring(0, split).trimEnd()
            remaining = remaining.substring(split).trimStart()
        }
        return result
    }

    private fun configurePaint(size: Float, color: Int, bold: Boolean) {
        paint.style = Paint.Style.FILL
        paint.textSize = size
        paint.color = color
        paint.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    private fun ensureSpace(required: Float) {
        if (cursorY + required <= CONTENT_BOTTOM) return
        finishPage()
        startPage()
    }

    private fun spacer(height: Float) {
        ensureSpace(height)
        cursorY += height
    }

    private fun startPage() {
        pageNumber += 1
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        currentPage = document.startPage(pageInfo)
        canvas = requireNotNull(currentPage).canvas

        paint.color = GREEN
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 72f, paint)
        configurePaint(18f, Color.WHITE, bold = true)
        canvas.drawText("AgroGestão Pro", MARGIN, 31f, paint)
        configurePaint(10f, Color.WHITE, bold = false)
        canvas.drawText("Relatório informativo de crédito rural", MARGIN, 51f, paint)
        cursorY = CONTENT_TOP
    }

    private fun finishPage() {
        val page = currentPage ?: return
        paint.color = LIGHT_GRAY
        canvas.drawRect(MARGIN, 805f, PAGE_WIDTH - MARGIN, 806f, paint)
        configurePaint(8f, DARK_GRAY, bold = false)
        canvas.drawText("Página $pageNumber | AgroGestão Pro", MARGIN, 822f, paint)
        document.finishPage(page)
        currentPage = null
    }

    private fun formatDecimal(value: Double): String =
        String.format(Locale("pt", "BR"), "%.2f", value)

    private fun String?.orMissing(): String = this?.takeIf(String::isNotBlank) ?: "Não informado"

    private companion object {
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val MARGIN = 42f
        const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2
        const val CONTENT_TOP = 96f
        const val CONTENT_BOTTOM = 790f

        val GREEN: Int = Color.rgb(34, 112, 67)
        val PALE_GREEN: Int = Color.rgb(231, 244, 235)
        val ORANGE: Int = Color.rgb(184, 101, 20)
        val PALE_ORANGE: Int = Color.rgb(255, 242, 224)
        val RED: Int = Color.rgb(170, 45, 45)
        val BLACK: Int = Color.rgb(35, 43, 38)
        val DARK_GRAY: Int = Color.rgb(82, 92, 86)
        val LIGHT_GRAY: Int = Color.rgb(210, 216, 212)
    }
}

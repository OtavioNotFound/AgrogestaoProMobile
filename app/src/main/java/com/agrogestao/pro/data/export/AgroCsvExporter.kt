package com.agrogestao.pro.data.export

import com.agrogestao.pro.BuildConfig
import com.agrogestao.pro.data.local.entities.FinancialEntity
import com.agrogestao.pro.data.local.entities.ProducerEntity
import com.agrogestao.pro.data.local.entities.TransactionType
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class FinancialCsvExport(
    val producer: ProducerEntity?,
    val fromDate: String,
    val toDate: String,
    val transactions: List<FinancialEntity>,
    val generatedAtEpochMillis: Long = System.currentTimeMillis(),
    val appVersion: String = BuildConfig.VERSION_NAME
)

object AgroCsvExporter {
    const val MIME_TYPE = "text/csv"

    fun encode(export: FinancialCsvExport): ByteArray {
        require(export.fromDate <= export.toDate) { "Período inválido para exportação." }
        val selected = export.transactions
            .asSequence()
            .filterNot(FinancialEntity::isDeleted)
            .filter { it.data in export.fromDate..export.toDate }
            .sortedWith(compareBy(FinancialEntity::data, FinancialEntity::id))
            .toList()
        val builder = StringBuilder("\uFEFF")
        row(builder, "AgroGestão Pro", "Exportação financeira")
        row(builder, "versao_aplicativo", export.appVersion)
        row(builder, "gerado_em_utc", utcTimestamp(export.generatedAtEpochMillis))
        row(builder, "proprietario", export.producer?.nomeProdutor.orEmpty())
        row(builder, "propriedade", export.producer?.nomePropriedade.orEmpty())
        row(builder, "municipio_uf", export.producer?.municipioUF.orEmpty())
        row(builder, "periodo_inicial", export.fromDate)
        row(builder, "periodo_final", export.toDate)
        builder.appendLine()
        row(
            builder,
            "data",
            "tipo",
            "categoria",
            "descricao",
            "valor_centavos",
            "valor_reais",
            "safra_id",
            "estado_sincronizacao",
            "identificador"
        )
        selected.forEach { transaction ->
            row(
                builder,
                transaction.data,
                if (transaction.tipo == TransactionType.ENTRADA) "entrada" else "saida",
                transaction.categoria,
                transaction.descricao,
                transaction.valorCentavos.toString(),
                String.format(Locale.ROOT, "%.2f", transaction.valor),
                transaction.cropCloudId.orEmpty(),
                transaction.syncStatus,
                transaction.cloudId
            )
        }
        return builder.toString().toByteArray(StandardCharsets.UTF_8)
    }

    private fun row(builder: StringBuilder, vararg values: String) {
        builder.append(values.joinToString(";") { escape(it) }).append("\r\n")
    }

    private fun escape(value: String): String {
        val normalized = value.replace("\r\n", " ").replace('\r', ' ').replace('\n', ' ')
        val spreadsheetSafe = if (
            normalized.isNotEmpty() && normalized.first() in charArrayOf('=', '+', '-', '@', '\t')
        ) {
            "'$normalized"
        } else {
            normalized
        }
        return if (spreadsheetSafe.any { it == ';' || it == '"' }) {
            "\"${spreadsheetSafe.replace("\"", "\"\"")}\""
        } else {
            spreadsheetSafe
        }
    }

    private fun utcTimestamp(epochMillis: Long): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(epochMillis))
}

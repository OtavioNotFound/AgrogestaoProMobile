package com.agrogestao.pro.data.export

import com.agrogestao.pro.data.local.entities.FinancialEntity
import com.agrogestao.pro.data.local.entities.TransactionType
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgroCsvExporterTest {
    @Test
    fun `csv filters period keeps cents and escapes spreadsheet delimiters`() {
        val bytes = AgroCsvExporter.encode(
            FinancialCsvExport(
                producer = null,
                fromDate = "2026-08-01",
                toDate = "2026-08-31",
                generatedAtEpochMillis = 1_800_000_000_000,
                appVersion = "teste",
                transactions = listOf(
                    transaction("2026-08-02", "=CMD(); Adubo; lote \"A\"", 12_355),
                    transaction("2026-09-01", "Fora", 100)
                )
            )
        )
        val csv = String(bytes, StandardCharsets.UTF_8)

        assertTrue(csv.startsWith("\uFEFF"))
        assertTrue(csv.contains("12355;123.55"))
        assertTrue(csv.contains("\"'=CMD(); Adubo; lote \"\"A\"\"\""))
        assertFalse(csv.contains("Fora"))
    }

    private fun transaction(date: String, description: String, cents: Long) = FinancialEntity(
        descricao = description,
        valorCentavos = cents,
        tipo = TransactionType.SAIDA,
        data = date,
        categoria = "Insumos"
    )
}

package com.agrogestao.pro.data.report

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.agrogestao.pro.data.local.entities.CropEntity
import com.agrogestao.pro.data.local.entities.FinancialEntity
import com.agrogestao.pro.data.local.entities.ProducerEntity
import com.agrogestao.pro.data.local.entities.TransactionType
import com.agrogestao.pro.data.remote.SupabaseConfig
import com.agrogestao.pro.domain.CreditReportCriteria
import com.agrogestao.pro.domain.CURRENT_CREDIT_REPORT_CONSENT_VERSION
import com.agrogestao.pro.domain.buildCreditReportSnapshot
import com.agrogestao.pro.domain.withConsentProof
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CreditReportPdfGeneratorTest {
    @Test
    fun generatesReadableMultipagePdfAndShareableUri() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val report = sampleReport()

        val file = CreditReportPdfGenerator.generate(context, report)
        val signature = file.inputStream().use { input ->
            ByteArray(4).also { assertEquals(4, input.read(it)) }
        }

        assertEquals("%PDF", signature.toString(Charsets.US_ASCII))
        assertTrue(file.length() > 5_000L)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        assertEquals("content", uri.scheme)

        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                assertTrue(renderer.pageCount >= 2)
                renderer.openPage(0).use { page ->
                    val bitmap = Bitmap.createBitmap(
                        page.width,
                        page.height,
                        Bitmap.Config.ARGB_8888
                    )
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    assertTrue(bitmap.getPixel(20, 20) != Color.WHITE)
                    bitmap.recycle()
                }
            }
        }

        val qaDirectory = File(requireNotNull(context.externalCacheDir), "pdf-qa")
        assertTrue(qaDirectory.exists() || qaDirectory.mkdirs())
        file.copyTo(File(qaDirectory, "relatorio-credito-rural-amostra.pdf"), overwrite = true)
    }

    @Test
    fun refusesPdfWithoutRecordedConsent() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val reportWithoutConsent = sampleReport().copy(consentProof = null)

        val error = assertThrows(IllegalArgumentException::class.java) {
            CreditReportPdfGenerator.generate(context, reportWithoutConsent)
        }

        assertTrue(error.message.orEmpty().contains("Consentimento"))
    }

    @Test
    fun archivedReportDetectsModificationAndRejectsPathTraversal() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val owner = "00000000-0000-4000-8000-000000000001"
        val archived = CreditReportArchiveStore.create(
            context = context,
            ownerUserId = owner,
            report = sampleReport(),
            createdAtEpochMillis = 1234L
        )
        try {
            assertEquals(ReportIntegrityStatus.VALID, CreditReportArchiveStore.verify(context, archived.history))
            assertEquals(archived.file.length(), archived.history.fileSizeBytes)
            assertEquals(64, archived.history.sha256.length)
            assertEquals(2, archived.history.reportFormatVersion)
            assertEquals(
                CURRENT_CREDIT_REPORT_CONSENT_VERSION,
                archived.history.consentVersion
            )
            assertTrue(archived.history.consentAcceptedAtEpochMillis > 0L)

            archived.file.appendBytes(byteArrayOf(1))
            assertEquals(
                ReportIntegrityStatus.MODIFIED,
                CreditReportArchiveStore.verify(context, archived.history)
            )

            assertThrows(IllegalArgumentException::class.java) {
                CreditReportArchiveStore.resolve(
                    context,
                    archived.history.copy(relativePath = "../arquivo-fora.pdf")
                )
            }
        } finally {
            archived.file.delete()
        }
    }

    private fun sampleReport() = buildCreditReportSnapshot(
        producer = ProducerEntity(
            nomeProdutor = "Maria da Silva",
            email = "maria@example.com",
            nomePropriedade = "Sítio Esperança",
            municipioUF = "Petrolina - PE",
            dAPouCAF = "CAF-2026-12345",
            areaTotalHectares = 18.75,
            remoteUserId = "00000000-0000-4000-8000-000000000001",
            syncStatus = SupabaseConfig.STATUS_SYNCED_CLOUD
        ),
        crops = listOf(
            crop(1, "Milho irrigado", 7.25, 65),
            crop(2, "Feijão-caupi", 4.50, 40),
            crop(3, "Mandioca", 7.00, 20)
        ),
        transactions = (1L..30L).map { id ->
            val income = id % 3L == 0L
            FinancialEntity(
                id = id,
                descricao = if (income) {
                    "Venda registrada da produção para cooperativa regional"
                } else {
                    "Compra de insumos e materiais para manejo da lavoura"
                },
                valor = if (income) 2_450.0 + id else 185.50 + id,
                tipo = if (income) TransactionType.ENTRADA else TransactionType.SAIDA,
                data = "2026-07-${((id - 1) % 28 + 1).toString().padStart(2, '0')}",
                categoria = if (income) "Venda" else "Insumos",
                syncStatus = if (id == 29L) {
                    SupabaseConfig.STATUS_LOCAL_OFFLINE
                } else {
                    SupabaseConfig.STATUS_SYNCED_CLOUD
                }
            )
        },
        criteria = CreditReportCriteria("2026-07-01", "2026-07-31"),
        generatedDate = "2026-08-02"
    ).withConsentProof(
        consentVersion = CURRENT_CREDIT_REPORT_CONSENT_VERSION,
        acceptedAtEpochMillis = 1_785_700_000_000L
    )

    private fun crop(id: Long, name: String, area: Double, progress: Int) = CropEntity(
        id = id,
        nomeCultura = name,
        areaHectares = area,
        dataInicio = "2026-03-10",
        previsaoColheita = "2026-09-20",
        progressoPercentual = progress,
        statusManejo = "Manejo em andamento",
        syncStatus = SupabaseConfig.STATUS_SYNCED_CLOUD
    )
}

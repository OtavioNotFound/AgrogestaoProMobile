package com.agrogestao.pro.data.local

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AgroMigrationTest {
    private val databaseName = "agro-migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AgroDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrateUnknownVersion1SalvagesRecognizedDataWithoutDestructiveFallback() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "agro-legacy-v1-salvage"
        context.deleteDatabase(name)
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            db.execSQL("CREATE TABLE produtor (id INTEGER PRIMARY KEY, nomeProdutor TEXT, email TEXT)")
            db.execSQL("INSERT INTO produtor VALUES (1, 'Produtora antiga', 'antiga@example.com')")
            db.execSQL("CREATE TABLE financeiro (id INTEGER PRIMARY KEY, descricao TEXT, valor REAL, tipo TEXT)")
            db.execSQL("INSERT INTO financeiro VALUES (7, 'Venda antiga', 12.55, 'ENTRADA')")
            db.version = 1
        }

        val database = Room.databaseBuilder(context, AgroDatabase::class.java, name)
            .addMigrations(
                AgroDatabase.MIGRATION_1_2,
                AgroDatabase.MIGRATION_2_3,
                AgroDatabase.MIGRATION_3_4,
                AgroDatabase.MIGRATION_4_5,
                AgroDatabase.MIGRATION_5_6,
                AgroDatabase.MIGRATION_6_7,
                AgroDatabase.MIGRATION_7_8,
                AgroDatabase.MIGRATION_8_9,
                AgroDatabase.MIGRATION_9_10,
                AgroDatabase.MIGRATION_10_11
            )
            .allowMainThreadQueries()
            .build()
        try {
            database.openHelper.writableDatabase
            database.openHelper.readableDatabase.query(
                "SELECT nomeProdutor, email FROM produtor WHERE id = 1"
            ).use {
                assertEquals(true, it.moveToFirst())
                assertEquals("Produtora antiga", it.getString(0))
                assertEquals("antiga@example.com", it.getString(1))
            }
            database.openHelper.readableDatabase.query(
                "SELECT descricao, valorCentavos FROM financeiro WHERE id = 7"
            ).use {
                assertEquals(true, it.moveToFirst())
                assertEquals("Venda antiga", it.getString(0))
                assertEquals(1255L, it.getLong(1))
            }
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun migrate3To4PreservesRowsAndAddsTombstones() {
        helper.createDatabase(databaseName, 3).apply {
            execSQL(
                """
                INSERT INTO safras (
                    id, nomeCultura, areaHectares, dataInicio, previsaoColheita,
                    progressoPercentual, statusManejo, syncStatus
                ) VALUES (
                    7, 'Feijão', 1.5, '2026-07-30', '2026-10-30',
                    30, 'Irrigação', 'Salvo no Celular (Offline)'
                )
                """.trimIndent()
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            databaseName,
            4,
            true,
            AgroDatabase.MIGRATION_3_4
        )

        migrated.query("SELECT id, nomeCultura, isDeleted FROM safras WHERE id = 7").use {
            assertEquals(true, it.moveToFirst())
            assertEquals("Feijão", it.getString(1))
            assertEquals(0, it.getInt(2))
        }
        migrated.close()
    }

    @Test
    fun migrate4To5PreservesProfileAndAddsRefreshSessionFields() {
        helper.createDatabase(databaseName, 4).apply {
            execSQL(
                """
                INSERT INTO produtor (
                    id, nomeProdutor, email, nomePropriedade, municipioUF,
                    dAPouCAF, areaTotalHectares, isLoggedIn, accessToken, syncStatus
                ) VALUES (
                    1, 'Maria', 'maria@example.com', 'Sítio Verde', 'Juazeiro - BA',
                    'CAF-123', 8.0, 1, 'access-antigo', 'Sincronizado na Nuvem'
                )
                """.trimIndent()
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            databaseName,
            5,
            true,
            AgroDatabase.MIGRATION_4_5
        )

        migrated.query(
            """
            SELECT email, accessToken, refreshToken, tokenExpiresAtEpochSeconds
            FROM produtor WHERE id = 1
            """.trimIndent()
        ).use {
            assertEquals(true, it.moveToFirst())
            assertEquals("maria@example.com", it.getString(0))
            assertEquals("access-antigo", it.getString(1))
            assertEquals("", it.getString(2))
            assertEquals(0L, it.getLong(3))
        }
        migrated.close()
    }

    @Test
    fun migrate5To6PreservesRowsAndCreatesStableCloudIdentity() {
        helper.createDatabase(databaseName, 5).apply {
            execSQL(
                """
                INSERT INTO safras (
                    id, nomeCultura, areaHectares, dataInicio, previsaoColheita,
                    progressoPercentual, statusManejo, syncStatus, isDeleted
                ) VALUES (
                    9, 'Mandioca', 3.0, '2026-01-10', '2026-11-10',
                    40, 'Capina', 'Salvo no Celular (Offline)', 0
                )
                """.trimIndent()
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            databaseName,
            6,
            true,
            AgroDatabase.MIGRATION_5_6
        )

        migrated.query(
            """
            SELECT nomeCultura, cloudId, ownerUserId, updatedAtEpochMillis
            FROM safras WHERE id = 9
            """.trimIndent()
        ).use {
            assertEquals(true, it.moveToFirst())
            assertEquals("Mandioca", it.getString(0))
            assertEquals(true, UUID_REGEX.matches(it.getString(1)))
            assertEquals("", it.getString(2))
            assertEquals(0L, it.getLong(3))
        }
        migrated.close()
    }

    @Test
    fun migrate6To7PreservesTasksAndAddsOptionalCropAssociation() {
        helper.createDatabase(databaseName, 6).apply {
            execSQL(
                """
                INSERT INTO tarefas (
                    id, titulo, descricao, categoria, dataLimite, status,
                    syncStatus, isDeleted, cloudId, ownerUserId, updatedAtEpochMillis
                ) VALUES (
                    11, 'Adubar', 'Aplicar NPK', 'Adubação', '2026-08-10', 'A_FAZER',
                    'Sincronizado na Nuvem', 0,
                    '00000000-0000-4000-8000-000000000011',
                    '00000000-0000-4000-8000-000000000001', 1234
                )
                """.trimIndent()
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            databaseName,
            7,
            true,
            AgroDatabase.MIGRATION_6_7
        )

        migrated.query(
            "SELECT titulo, cropCloudId FROM tarefas WHERE id = 11"
        ).use {
            assertEquals(true, it.moveToFirst())
            assertEquals("Adubar", it.getString(0))
            assertEquals(true, it.isNull(1))
        }
        migrated.close()
    }

    @Test
    fun migrate7To8CreatesAccountScopedReportHistory() {
        helper.createDatabase(databaseName, 7).close()

        val migrated = helper.runMigrationsAndValidate(
            databaseName,
            8,
            true,
            AgroDatabase.MIGRATION_7_8
        )
        migrated.execSQL(
            """
            INSERT INTO report_history (
                reportId, ownerUserId, fileName, relativePath, createdAtEpochMillis,
                generatedDate, fromDate, toDate, income, expenses, balance,
                isComplete, missingItems, sha256, fileSizeBytes
            ) VALUES (
                'report-1', 'owner-1', 'report.pdf', 'owner/report.pdf', 1234,
                '2026-08-02', '2026-01-01', '2026-07-31', 1000.0, 250.0, 750.0,
                1, '', 'abc123', 999
            )
            """.trimIndent()
        )
        migrated.query(
            "SELECT ownerUserId, balance, isComplete, sha256 FROM report_history"
        ).use {
            assertEquals(true, it.moveToFirst())
            assertEquals("owner-1", it.getString(0))
            assertEquals(750.0, it.getDouble(1), 0.0)
            assertEquals(1, it.getInt(2))
            assertEquals("abc123", it.getString(3))
        }
        migrated.close()
    }

    @Test
    fun migrate8To9PreservesHistoryAndCreatesVersionedConsent() {
        helper.createDatabase(databaseName, 8).apply {
            execSQL(
                """
                INSERT INTO report_history (
                    reportId, ownerUserId, fileName, relativePath, createdAtEpochMillis,
                    generatedDate, fromDate, toDate, income, expenses, balance,
                    isComplete, missingItems, sha256, fileSizeBytes
                ) VALUES (
                    'legacy-report', 'owner-1', 'old.pdf', 'owner/old.pdf', 1234,
                    '2026-08-02', '2026-01-01', '2026-07-31', 1000.0, 250.0, 750.0,
                    1, '', 'abc123', 999
                )
                """.trimIndent()
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            databaseName,
            9,
            true,
            AgroDatabase.MIGRATION_8_9
        )

        migrated.query(
            """
            SELECT reportFormatVersion, consentVersion, consentAcceptedAtEpochMillis
            FROM report_history WHERE reportId = 'legacy-report'
            """.trimIndent()
        ).use {
            assertEquals(true, it.moveToFirst())
            assertEquals(1, it.getInt(0))
            assertEquals(0, it.getInt(1))
            assertEquals(0L, it.getLong(2))
        }
        migrated.execSQL(
            """
            INSERT INTO report_consent (
                ownerUserId, consentVersion, acceptedAtEpochMillis, isGranted, revokedAtEpochMillis
            ) VALUES ('owner-1', 1, 4567, 1, NULL)
            """.trimIndent()
        )
        migrated.query(
            "SELECT consentVersion, acceptedAtEpochMillis, isGranted FROM report_consent"
        ).use {
            assertEquals(true, it.moveToFirst())
            assertEquals(1, it.getInt(0))
            assertEquals(4567L, it.getLong(1))
            assertEquals(1, it.getInt(2))
        }
        migrated.close()
    }

    @Test
    fun migrate9To10ConvertsMoneyToExactCents() {
        helper.createDatabase(databaseName, 9).apply {
            execSQL(
                """
                INSERT INTO financeiro (
                    id, descricao, valor, tipo, data, categoria, syncStatus,
                    isDeleted, cloudId, ownerUserId, updatedAtEpochMillis, cropCloudId
                ) VALUES (
                    21, 'Venda', 125.55, 'ENTRADA', '2026-08-04', 'Venda',
                    'Salvo no Celular (Offline)', 0,
                    '00000000-0000-4000-8000-000000000021', 'owner-1', 1234, NULL
                )
                """.trimIndent()
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            databaseName,
            10,
            true,
            AgroDatabase.MIGRATION_9_10
        )

        migrated.query(
            "SELECT descricao, valorCentavos, typeof(valorCentavos) FROM financeiro WHERE id = 21"
        ).use {
            assertEquals(true, it.moveToFirst())
            assertEquals("Venda", it.getString(0))
            assertEquals(12_555L, it.getLong(1))
            assertEquals("integer", it.getString(2))
        }
        migrated.close()
    }

    @Test
    fun migrate10To11CreatesConflictAuditTrail() {
        val migrated = helper.createDatabase(databaseName, 10).let { database ->
            database.close()
            helper.runMigrationsAndValidate(
                databaseName,
                11,
                true,
                AgroDatabase.MIGRATION_10_11
            )
        }
        migrated.execSQL(
            """
            INSERT INTO sync_conflicts (
                ownerUserId, entityType, entityCloudId, localTimestamp,
                remoteTimestamp, resolution, detectedAtEpochMillis
            ) VALUES ('owner', 'tarefa', 'cloud-id', 100, 200, 'REMOTE_WON', 300)
            """.trimIndent()
        )
        migrated.query("SELECT resolution FROM sync_conflicts").use {
            assertEquals(true, it.moveToFirst())
            assertEquals("REMOTE_WON", it.getString(0))
        }
        migrated.close()
    }

    private companion object {
        val UUID_REGEX = Regex(
            "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"
        )
    }
}

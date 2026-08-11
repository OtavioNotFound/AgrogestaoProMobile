package com.agrogestao.pro.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.agrogestao.pro.data.local.dao.CropDao
import com.agrogestao.pro.data.local.dao.BackupDao
import com.agrogestao.pro.data.local.dao.DailyActivityDao
import com.agrogestao.pro.data.local.dao.FinancialDao
import com.agrogestao.pro.data.local.dao.ProducerDao
import com.agrogestao.pro.data.local.dao.ReportHistoryDao
import com.agrogestao.pro.data.local.dao.ReportConsentDao
import com.agrogestao.pro.data.local.dao.TaskDao
import com.agrogestao.pro.data.local.dao.SyncConflictDao
import com.agrogestao.pro.data.local.entities.CropEntity
import com.agrogestao.pro.data.local.entities.FinancialEntity
import com.agrogestao.pro.data.local.entities.ProducerEntity
import com.agrogestao.pro.data.local.entities.ReportHistoryEntity
import com.agrogestao.pro.data.local.entities.ReportConsentEntity
import com.agrogestao.pro.data.local.entities.TaskEntity
import com.agrogestao.pro.data.local.entities.SyncConflictEntity

@Database(
    entities = [
        CropEntity::class,
        TaskEntity::class,
        FinancialEntity::class,
        ProducerEntity::class,
        ReportHistoryEntity::class,
        ReportConsentEntity::class,
        SyncConflictEntity::class
    ],
    version = 11,
    exportSchema = true
)
abstract class AgroDatabase : RoomDatabase() {

    abstract fun cropDao(): CropDao
    abstract fun taskDao(): TaskDao
    abstract fun financialDao(): FinancialDao
    abstract fun producerDao(): ProducerDao
    abstract fun backupDao(): BackupDao
    abstract fun dailyActivityDao(): DailyActivityDao
    abstract fun reportHistoryDao(): ReportHistoryDao
    abstract fun reportConsentDao(): ReportConsentDao
    abstract fun syncConflictDao(): SyncConflictDao

    companion object {
        @Volatile
        private var INSTANCE: AgroDatabase? = null

        fun getDatabase(context: Context): AgroDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AgroDatabase::class.java,
                    "agrogestao_pro_db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11
                    )
                    // O schema da versão 1 não existe no projeto original.
                    // Limitamos a recriação somente a essa versão desconhecida.
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * O schema v1 original não foi exportado. Em vez de apagar o banco, esta migração
         * recria o schema v2 conhecido e copia cada coluna reconhecida com valores seguros
         * para campos ausentes.
         */
        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                salvageTable(db, "safras", """
                    CREATE TABLE IF NOT EXISTS safras (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        nomeCultura TEXT NOT NULL, areaHectares REAL NOT NULL,
                        dataInicio TEXT NOT NULL, previsaoColheita TEXT NOT NULL,
                        progressoPercentual INTEGER NOT NULL, statusManejo TEXT NOT NULL,
                        syncStatus TEXT NOT NULL
                    )
                """.trimIndent(), linkedMapOf(
                    "id" to "NULL", "nomeCultura" to "''", "areaHectares" to "0.0",
                    "dataInicio" to "''", "previsaoColheita" to "''",
                    "progressoPercentual" to "0", "statusManejo" to "''",
                    "syncStatus" to "'LOCAL_OFFLINE'"
                ))
                salvageTable(db, "tarefas", """
                    CREATE TABLE IF NOT EXISTS tarefas (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        titulo TEXT NOT NULL, descricao TEXT NOT NULL, categoria TEXT NOT NULL,
                        dataLimite TEXT NOT NULL, status TEXT NOT NULL, syncStatus TEXT NOT NULL
                    )
                """.trimIndent(), linkedMapOf(
                    "id" to "NULL", "titulo" to "''", "descricao" to "''",
                    "categoria" to "''", "dataLimite" to "''", "status" to "'A_FAZER'",
                    "syncStatus" to "'LOCAL_OFFLINE'"
                ))
                salvageTable(db, "financeiro", """
                    CREATE TABLE IF NOT EXISTS financeiro (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        descricao TEXT NOT NULL, valor REAL NOT NULL, tipo TEXT NOT NULL,
                        data TEXT NOT NULL, categoria TEXT NOT NULL, syncStatus TEXT NOT NULL
                    )
                """.trimIndent(), linkedMapOf(
                    "id" to "NULL", "descricao" to "''", "valor" to "0.0",
                    "tipo" to "'SAIDA'", "data" to "''", "categoria" to "''",
                    "syncStatus" to "'LOCAL_OFFLINE'"
                ))
                salvageTable(db, "produtor", """
                    CREATE TABLE IF NOT EXISTS produtor (
                        id INTEGER NOT NULL, nomeProdutor TEXT NOT NULL, email TEXT NOT NULL,
                        nomePropriedade TEXT NOT NULL, municipioUF TEXT NOT NULL,
                        dAPouCAF TEXT NOT NULL, areaTotalHectares REAL NOT NULL,
                        isLoggedIn INTEGER NOT NULL, syncStatus TEXT NOT NULL,
                        PRIMARY KEY(id)
                    )
                """.trimIndent(), linkedMapOf(
                    "id" to "1", "nomeProdutor" to "''", "email" to "''",
                    "nomePropriedade" to "''", "municipioUF" to "''", "dAPouCAF" to "''",
                    "areaTotalHectares" to "0.0", "isLoggedIn" to "0",
                    "syncStatus" to "'LOCAL_OFFLINE'"
                ))
            }
        }

        internal val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE produtor ADD COLUMN accessToken TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        internal val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE safras ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE tarefas ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE financeiro ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        internal val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE produtor ADD COLUMN refreshToken TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE produtor ADD COLUMN tokenExpiresAtEpochSeconds INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        internal val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE produtor ADD COLUMN remoteUserId TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE produtor ADD COLUMN updatedAtEpochMillis INTEGER NOT NULL DEFAULT 0"
                )

                addCloudSyncColumns(db, "safras")
                addCloudSyncColumns(db, "tarefas")
                addCloudSyncColumns(db, "financeiro")
            }

            private fun addCloudSyncColumns(db: SupportSQLiteDatabase, table: String) {
                db.execSQL(
                    "ALTER TABLE $table ADD COLUMN cloudId TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE $table ADD COLUMN ownerUserId TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE $table ADD COLUMN updatedAtEpochMillis INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    """
                    UPDATE $table
                    SET cloudId = lower(
                        hex(randomblob(4)) || '-' ||
                        hex(randomblob(2)) || '-4' ||
                        substr(hex(randomblob(2)), 2) || '-' ||
                        substr('89ab', abs(random()) % 4 + 1, 1) ||
                        substr(hex(randomblob(2)), 2) || '-' ||
                        hex(randomblob(6))
                    )
                    WHERE cloudId = ''
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_${table}_cloudId ON $table (cloudId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_${table}_ownerUserId_syncStatus " +
                        "ON $table (ownerUserId, syncStatus)"
                )
            }
        }

        internal val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tarefas ADD COLUMN cropCloudId TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE financeiro ADD COLUMN cropCloudId TEXT DEFAULT NULL")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_tarefas_cropCloudId ON tarefas (cropCloudId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_financeiro_cropCloudId ON financeiro (cropCloudId)"
                )
            }
        }

        internal val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS report_history (
                        reportId TEXT NOT NULL,
                        ownerUserId TEXT NOT NULL,
                        fileName TEXT NOT NULL,
                        relativePath TEXT NOT NULL,
                        createdAtEpochMillis INTEGER NOT NULL,
                        generatedDate TEXT NOT NULL,
                        fromDate TEXT NOT NULL,
                        toDate TEXT NOT NULL,
                        income REAL NOT NULL,
                        expenses REAL NOT NULL,
                        balance REAL NOT NULL,
                        isComplete INTEGER NOT NULL,
                        missingItems TEXT NOT NULL,
                        sha256 TEXT NOT NULL,
                        fileSizeBytes INTEGER NOT NULL,
                        PRIMARY KEY(reportId)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS " +
                        "index_report_history_ownerUserId_createdAtEpochMillis " +
                        "ON report_history (ownerUserId, createdAtEpochMillis)"
                )
            }
        }

        internal val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE report_history ADD COLUMN " +
                        "reportFormatVersion INTEGER NOT NULL DEFAULT 1"
                )
                db.execSQL(
                    "ALTER TABLE report_history ADD COLUMN " +
                        "consentVersion INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE report_history ADD COLUMN " +
                        "consentAcceptedAtEpochMillis INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS report_consent (
                        ownerUserId TEXT NOT NULL,
                        consentVersion INTEGER NOT NULL,
                        acceptedAtEpochMillis INTEGER NOT NULL,
                        isGranted INTEGER NOT NULL,
                        revokedAtEpochMillis INTEGER,
                        PRIMARY KEY(ownerUserId)
                    )
                    """.trimIndent()
                )
            }
        }

        internal val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS financeiro_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        descricao TEXT NOT NULL,
                        valorCentavos INTEGER NOT NULL,
                        tipo TEXT NOT NULL,
                        data TEXT NOT NULL,
                        categoria TEXT NOT NULL,
                        syncStatus TEXT NOT NULL,
                        isDeleted INTEGER NOT NULL,
                        cloudId TEXT NOT NULL,
                        ownerUserId TEXT NOT NULL,
                        updatedAtEpochMillis INTEGER NOT NULL,
                        cropCloudId TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO financeiro_new (
                        id, descricao, valorCentavos, tipo, data, categoria,
                        syncStatus, isDeleted, cloudId, ownerUserId,
                        updatedAtEpochMillis, cropCloudId
                    )
                    SELECT
                        id, descricao, CAST(ROUND(valor * 100.0) AS INTEGER), tipo, data, categoria,
                        syncStatus, isDeleted, cloudId, ownerUserId,
                        updatedAtEpochMillis, cropCloudId
                    FROM financeiro
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE financeiro")
                db.execSQL("ALTER TABLE financeiro_new RENAME TO financeiro")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_financeiro_cloudId " +
                        "ON financeiro (cloudId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_financeiro_ownerUserId_syncStatus " +
                        "ON financeiro (ownerUserId, syncStatus)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_financeiro_cropCloudId " +
                        "ON financeiro (cropCloudId)"
                )
            }
        }

        internal val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sync_conflicts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        ownerUserId TEXT NOT NULL,
                        entityType TEXT NOT NULL,
                        entityCloudId TEXT NOT NULL,
                        localTimestamp INTEGER NOT NULL,
                        remoteTimestamp INTEGER NOT NULL,
                        resolution TEXT NOT NULL,
                        detectedAtEpochMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_sync_conflicts_ownerUserId_detectedAtEpochMillis " +
                        "ON sync_conflicts (ownerUserId, detectedAtEpochMillis)"
                )
            }
        }

        private fun salvageTable(
            db: SupportSQLiteDatabase,
            table: String,
            createSql: String,
            columns: LinkedHashMap<String, String>
        ) {
            val existingColumns = mutableSetOf<String>()
            db.query("PRAGMA table_info(`$table`)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) existingColumns += cursor.getString(nameIndex)
            }
            if (existingColumns.isEmpty()) {
                db.execSQL(createSql)
                return
            }
            val legacy = "${table}_v1_legacy"
            db.execSQL("ALTER TABLE `$table` RENAME TO `$legacy`")
            db.execSQL(createSql)
            val names = columns.keys.joinToString(", ") { "`$it`" }
            val values = columns.entries.joinToString(", ") { (name, fallback) ->
                if (name in existingColumns) "`$name`" else fallback
            }
            db.execSQL("INSERT OR IGNORE INTO `$table` ($names) SELECT $values FROM `$legacy`")
            db.execSQL("DROP TABLE `$legacy`")
        }
    }
}

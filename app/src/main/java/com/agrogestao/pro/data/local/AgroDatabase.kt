package com.agrogestao.pro.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.agrogestao.pro.data.local.dao.CropDao
import com.agrogestao.pro.data.local.dao.BackupDao
import com.agrogestao.pro.data.local.dao.FinancialDao
import com.agrogestao.pro.data.local.dao.ProducerDao
import com.agrogestao.pro.data.local.dao.ReportHistoryDao
import com.agrogestao.pro.data.local.dao.ReportConsentDao
import com.agrogestao.pro.data.local.dao.TaskDao
import com.agrogestao.pro.data.local.entities.CropEntity
import com.agrogestao.pro.data.local.entities.FinancialEntity
import com.agrogestao.pro.data.local.entities.ProducerEntity
import com.agrogestao.pro.data.local.entities.ReportHistoryEntity
import com.agrogestao.pro.data.local.entities.ReportConsentEntity
import com.agrogestao.pro.data.local.entities.TaskEntity

@Database(
    entities = [
        CropEntity::class,
        TaskEntity::class,
        FinancialEntity::class,
        ProducerEntity::class,
        ReportHistoryEntity::class,
        ReportConsentEntity::class
    ],
    version = 9,
    exportSchema = true
)
abstract class AgroDatabase : RoomDatabase() {

    abstract fun cropDao(): CropDao
    abstract fun taskDao(): TaskDao
    abstract fun financialDao(): FinancialDao
    abstract fun producerDao(): ProducerDao
    abstract fun backupDao(): BackupDao
    abstract fun reportHistoryDao(): ReportHistoryDao
    abstract fun reportConsentDao(): ReportConsentDao

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
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9
                    )
                    // O schema da versão 1 não existe no projeto original.
                    // Limitamos a recriação somente a essa versão desconhecida.
                    .fallbackToDestructiveMigrationFrom(1)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
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
    }
}

package com.agrogestao.pro.data.repository

import android.util.Base64
import android.util.Log
import com.agrogestao.pro.data.backup.AgroBackupCodec
import com.agrogestao.pro.data.backup.BackupException
import com.agrogestao.pro.data.backup.BackupRestoreSummary
import com.agrogestao.pro.data.backup.BackupSnapshot
import com.agrogestao.pro.data.backup.ProducerBackup
import com.agrogestao.pro.data.local.dao.BackupDao
import com.agrogestao.pro.data.local.dao.CropDao
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
import com.agrogestao.pro.data.local.entities.TaskStatus
import com.agrogestao.pro.data.local.entities.TransactionType
import com.agrogestao.pro.data.remote.SupabaseConfig
import com.agrogestao.pro.data.remote.SupabaseRestClient
import com.agrogestao.pro.data.remote.SupabaseSchemaMode
import com.agrogestao.pro.data.remote.SyncResult
import com.agrogestao.pro.data.security.SecureSession
import com.agrogestao.pro.data.security.SecureSessionStore
import com.agrogestao.pro.data.sync.formatCloudTimestamp
import com.agrogestao.pro.data.sync.nextLocalTimestamp
import com.agrogestao.pro.data.sync.parseCloudTimestamp
import com.agrogestao.pro.data.sync.shouldApplyRemoteChange
import com.agrogestao.pro.domain.canUseLocalProfile
import com.agrogestao.pro.domain.accountPasswordError
import com.agrogestao.pro.domain.shouldRefreshToken
import com.agrogestao.pro.domain.tokenExpiryEpochSeconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.UUID

enum class SignUpOutcome {
    SIGNED_IN,
    EMAIL_CONFIRMATION_REQUIRED
}

class EmailConfirmationRequiredException(val pendingEmail: String) : Exception(
    "Seu e-mail ainda não foi confirmado. Abra o e-mail recebido e toque no botão de confirmação."
)

@OptIn(ExperimentalCoroutinesApi::class)
class AgroRepository(
    private val backupDao: BackupDao,
    private val cropDao: CropDao,
    private val taskDao: TaskDao,
    private val financialDao: FinancialDao,
    private val producerDao: ProducerDao,
    private val reportHistoryDao: ReportHistoryDao,
    private val reportConsentDao: ReportConsentDao,
    private val secureSessionStore: SecureSessionStore
) {
    val producerProfile: Flow<ProducerEntity?> = producerDao.getProducerProfile()

    val activeOwnerUserId = producerProfile
        .map { producer ->
            producer
                ?.takeIf { it.isLoggedIn }
                ?.remoteUserId
                .orEmpty()
        }
        .distinctUntilChanged()

    val allCrops: Flow<List<CropEntity>> = activeOwnerUserId.flatMapLatest { ownerUserId ->
        if (ownerUserId.isBlank()) flowOf(emptyList()) else cropDao.getAllCrops(ownerUserId)
    }
    val allTasks: Flow<List<TaskEntity>> = activeOwnerUserId.flatMapLatest { ownerUserId ->
        if (ownerUserId.isBlank()) flowOf(emptyList()) else taskDao.getAllTasks(ownerUserId)
    }
    val allTransactions: Flow<List<FinancialEntity>> = activeOwnerUserId.flatMapLatest { ownerUserId ->
        if (ownerUserId.isBlank()) flowOf(emptyList()) else financialDao.getAllTransactions(ownerUserId)
    }
    val reportHistory: Flow<List<ReportHistoryEntity>> = activeOwnerUserId.flatMapLatest { ownerUserId ->
        if (ownerUserId.isBlank()) {
            flowOf(emptyList())
        } else {
            reportHistoryDao.observeForOwner(ownerUserId)
        }
    }
    val reportConsent: Flow<ReportConsentEntity?> = activeOwnerUserId.flatMapLatest { ownerUserId ->
        if (ownerUserId.isBlank()) {
            flowOf(null)
        } else {
            reportConsentDao.observeForOwner(ownerUserId)
        }
    }

    private val syncMutex = Mutex()

    suspend fun saveReportHistory(report: ReportHistoryEntity) = withContext(Dispatchers.IO) {
        val ownerUserId = requireActiveUserId()
        reportHistoryDao.insert(report.copy(ownerUserId = ownerUserId))
    }

    suspend fun deleteReportHistory(reportId: String): Boolean = withContext(Dispatchers.IO) {
        val ownerUserId = requireActiveUserId()
        reportHistoryDao.deleteOwned(reportId, ownerUserId) == 1
    }

    suspend fun grantReportConsent(
        consentVersion: Int,
        acceptedAtEpochMillis: Long = System.currentTimeMillis()
    ): ReportConsentEntity = withContext(Dispatchers.IO) {
        require(consentVersion > 0) { "Versão do consentimento inválida." }
        require(acceptedAtEpochMillis > 0L) { "Data do consentimento inválida." }
        val consent = ReportConsentEntity(
            ownerUserId = requireActiveUserId(),
            consentVersion = consentVersion,
            acceptedAtEpochMillis = acceptedAtEpochMillis,
            isGranted = true,
            revokedAtEpochMillis = null
        )
        reportConsentDao.upsert(consent)
        consent
    }

    suspend fun revokeReportConsent(
        revokedAtEpochMillis: Long = System.currentTimeMillis()
    ): Boolean = withContext(Dispatchers.IO) {
        val ownerUserId = requireActiveUserId()
        val existing = reportConsentDao.getForOwner(ownerUserId) ?: return@withContext false
        reportConsentDao.upsert(
            existing.copy(
                isGranted = false,
                revokedAtEpochMillis = revokedAtEpochMillis
            )
        )
        true
    }

    suspend fun prepareLocalSession() = withContext(Dispatchers.IO) {
        val producer = producerDao.getProducerProfileOnce() ?: return@withContext
        if (!producer.isLoggedIn) return@withContext
        if (producer.remoteUserId.startsWith(LOCAL_OWNER_PREFIX)) {
            claimLegacyRows(producer.remoteUserId)
            return@withContext
        }

        val storedSession = secureSessionStore.read()
        if (
            storedSession != null &&
            producer.remoteUserId.isNotBlank() &&
            storedSession.userId != producer.remoteUserId
        ) {
            secureSessionStore.clear()
            producerDao.insertOrUpdateProducer(
                producer.copy(
                    isLoggedIn = false,
                    accessToken = "",
                    refreshToken = "",
                    tokenExpiresAtEpochSeconds = 0
                )
            )
            return@withContext
        }

        val migratedSession = storedSession ?: migrateLegacySession(producer)
        if (migratedSession == null) {
            producerDao.insertOrUpdateProducer(
                producer.copy(
                    isLoggedIn = false,
                    accessToken = "",
                    refreshToken = "",
                    tokenExpiresAtEpochSeconds = 0
                )
            )
            return@withContext
        }
        val userId = producer.remoteUserId.ifBlank { migratedSession.userId }
        if (producer.remoteUserId != userId) {
            producerDao.updateRemoteUserId(userId)
        }
        producerDao.clearLegacySession()
        claimLegacyRows(userId)
    }

    suspend fun signUp(
        nome: String,
        email: String,
        password: String,
        propriedade: String,
        municipio: String,
        caf: String,
        area: Double
    ): Result<SignUpOutcome> = withContext(Dispatchers.IO) {
        val normalizedEmail = email.trim().lowercase()
        val existing = producerDao.getProducerProfileOnce()
        if (!canUseLocalProfile(existing?.email, normalizedEmail)) {
            return@withContext Result.failure(accountMismatchError())
        }

        val authResult = SupabaseRestClient.signUp(normalizedEmail, password)
        authResult.errorMessage?.let { return@withContext Result.failure(Exception(it)) }
        val token = authResult.accessToken.orEmpty()
        if (token.isBlank()) {
            val timestamp = nextLocalTimestamp(existing?.updatedAtEpochMillis ?: 0)
            val pendingProducer = (existing ?: ProducerEntity(
                nomeProdutor = nome.trim(),
                email = normalizedEmail,
                nomePropriedade = propriedade.trim(),
                municipioUF = municipio.trim(),
                dAPouCAF = caf.trim(),
                areaTotalHectares = area
            )).copy(
                nomeProdutor = nome.trim(),
                email = normalizedEmail,
                nomePropriedade = propriedade.trim(),
                municipioUF = municipio.trim(),
                dAPouCAF = caf.trim(),
                areaTotalHectares = area,
                isLoggedIn = false,
                accessToken = "",
                refreshToken = "",
                tokenExpiresAtEpochSeconds = 0,
                syncStatus = SupabaseConfig.STATUS_LOCAL_OFFLINE,
                updatedAtEpochMillis = timestamp
            )
            producerDao.insertOrUpdateProducer(pendingProducer)
            return@withContext Result.success(SignUpOutcome.EMAIL_CONFIRMATION_REQUIRED)
        }
        val userId = authResult.userId.orEmpty().ifBlank { extractJwtSubject(token).orEmpty() }
        if (userId.isBlank()) {
            return@withContext Result.failure(Exception("A conta recebida da nuvem é inválida."))
        }

        val expiresAt = tokenExpiryEpochSeconds(
            nowEpochSeconds(),
            authResult.expiresInSeconds
        )
        if (
            !secureSessionStore.save(
                SecureSession(
                    userId = userId,
                    accessToken = token,
                    refreshToken = authResult.refreshToken.orEmpty(),
                    expiresAtEpochSeconds = expiresAt
                )
            )
        ) {
            return@withContext Result.failure(
                Exception("Não foi possível proteger a sessão neste celular.")
            )
        }
        adoptOfflineRows(existing?.remoteUserId, userId)

        val timestamp = nextLocalTimestamp(existing?.updatedAtEpochMillis ?: 0)
        val producer = ProducerEntity(
            id = 1,
            nomeProdutor = nome.trim(),
            email = normalizedEmail,
            nomePropriedade = propriedade.trim(),
            municipioUF = municipio.trim(),
            dAPouCAF = caf.trim(),
            areaTotalHectares = area,
            isLoggedIn = true,
            remoteUserId = userId,
            accessToken = "",
            refreshToken = "",
            tokenExpiresAtEpochSeconds = 0,
            syncStatus = SupabaseConfig.STATUS_LOCAL_OFFLINE,
            updatedAtEpochMillis = timestamp
        )
        producerDao.insertOrUpdateProducer(producer)
        claimLegacyRows(userId)
        syncPendingData()
        Result.success(SignUpOutcome.SIGNED_IN)
    }

    suspend fun signIn(email: String, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val normalizedEmail = email.trim().lowercase()
            val existing = producerDao.getProducerProfileOnce()
            if (!canUseLocalProfile(existing?.email, normalizedEmail)) {
                return@withContext Result.failure(accountMismatchError())
            }

            val authResult = SupabaseRestClient.signIn(normalizedEmail, password)
            if (
                authResult.errorCode == "email_not_confirmed" ||
                authResult.errorMessage?.contains("ainda não foi confirmado", ignoreCase = true) == true
            ) {
                return@withContext Result.failure(
                    EmailConfirmationRequiredException(normalizedEmail)
                )
            }
            authResult.errorMessage?.let { return@withContext Result.failure(Exception(it)) }
            val token = authResult.accessToken.orEmpty()
            if (token.isBlank()) {
                return@withContext Result.failure(
                    Exception("A sessão recebida é inválida. Tente novamente.")
                )
            }
            val userId = authResult.userId.orEmpty().ifBlank { extractJwtSubject(token).orEmpty() }
            if (userId.isBlank()) {
                return@withContext Result.failure(Exception("A conta recebida da nuvem é inválida."))
            }

            val expiresAt = tokenExpiryEpochSeconds(
                nowEpochSeconds(),
                authResult.expiresInSeconds
            )
            if (
                !secureSessionStore.save(
                    SecureSession(
                        userId = userId,
                        accessToken = token,
                        refreshToken = authResult.refreshToken.orEmpty(),
                        expiresAtEpochSeconds = expiresAt
                    )
                )
            ) {
                return@withContext Result.failure(
                    Exception("Não foi possível proteger a sessão neste celular.")
                )
            }
            adoptOfflineRows(existing?.remoteUserId, userId)

            val producer = (existing ?: ProducerEntity(
                nomeProdutor = normalizedEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
                email = normalizedEmail,
                nomePropriedade = "",
                municipioUF = "",
                dAPouCAF = "",
                areaTotalHectares = 0.0,
                syncStatus = SupabaseConfig.STATUS_SYNCED_CLOUD,
                updatedAtEpochMillis = 0
            )).copy(
                email = normalizedEmail,
                isLoggedIn = true,
                remoteUserId = userId,
                accessToken = "",
                refreshToken = "",
                tokenExpiresAtEpochSeconds = 0
            )
            producerDao.insertOrUpdateProducer(producer)
            claimLegacyRows(userId)
            syncPendingData()
            Result.success(Unit)
        }

    suspend fun resendSignUpConfirmation(email: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val normalizedEmail = email.trim().lowercase()
            if (normalizedEmail.isBlank()) {
                return@withContext Result.failure(Exception("Informe o e-mail da conta."))
            }
            val response = SupabaseRestClient.resendSignUpConfirmation(normalizedEmail)
            if (!response.success) {
                return@withContext Result.failure(
                    Exception(response.errorMessage ?: "Não foi possível reenviar o e-mail.")
                )
            }
            Result.success(Unit)
        }

    suspend fun requestPasswordRecovery(email: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val normalizedEmail = email.trim().lowercase()
            if (normalizedEmail.isBlank()) {
                return@withContext Result.failure(Exception("Informe o e-mail da conta."))
            }
            val response = SupabaseRestClient.requestPasswordRecovery(normalizedEmail)
            if (!response.success) {
                return@withContext Result.failure(
                    Exception(response.errorMessage ?: "Não foi possível enviar o e-mail de recuperação.")
                )
            }
            Result.success(Unit)
        }

    suspend fun completePasswordRecovery(
        accessToken: String,
        refreshToken: String,
        expiresInSeconds: Long,
        newPassword: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (accessToken.isBlank() || refreshToken.isBlank()) {
            return@withContext Result.failure(
                Exception("O link de recuperação está incompleto. Peça um novo e-mail.")
            )
        }
        accountPasswordError(newPassword)?.let { message ->
            return@withContext Result.failure(
                Exception(message)
            )
        }
        val verifiedRecovery = SupabaseRestClient.validateAccessToken(accessToken)
        verifiedRecovery.errorMessage?.let {
            return@withContext Result.failure(
                Exception("O link de recuperação expirou ou já foi usado. Peça um novo e-mail.")
            )
        }
        val recoveryEmail = verifiedRecovery.email.orEmpty().trim().lowercase()
        val existing = producerDao.getProducerProfileOnce()
        if (recoveryEmail.isBlank()) {
            return@withContext Result.failure(Exception("A conta do link de recuperação é inválida."))
        }
        if (!canUseLocalProfile(existing?.email, recoveryEmail)) {
            return@withContext Result.failure(accountMismatchError())
        }
        val updated = SupabaseRestClient.updatePassword(accessToken, newPassword)
        if (!updated.success) {
            return@withContext Result.failure(
                Exception(updated.errorMessage ?: "Não foi possível criar a nova senha.")
            )
        }
        completeEmailConfirmation(accessToken, refreshToken, expiresInSeconds)
    }

    suspend fun changePassword(newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        accountPasswordError(newPassword)?.let { message ->
            return@withContext Result.failure(Exception(message))
        }
        val session = getCloudSession()
            ?: return@withContext Result.failure(
                Exception("Entre com uma conta conectada à nuvem para trocar a senha.")
            )
        val updated = SupabaseRestClient.updatePassword(session.accessToken, newPassword)
        if (!updated.success) {
            return@withContext Result.failure(
                Exception(updated.errorMessage ?: "Não foi possível trocar a senha.")
            )
        }
        Result.success(Unit)
    }

    suspend fun completeEmailConfirmation(
        accessToken: String,
        refreshToken: String,
        expiresInSeconds: Long
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (accessToken.isBlank() || refreshToken.isBlank()) {
            return@withContext Result.failure(
                Exception("O link de confirmação está incompleto. Peça um novo e-mail.")
            )
        }

        val refreshed = SupabaseRestClient.refreshSession(refreshToken)
        val authResult = if (refreshed.accessToken.isNullOrBlank()) {
            SupabaseRestClient.validateAccessToken(accessToken)
        } else {
            refreshed
        }
        authResult.errorMessage?.let { return@withContext Result.failure(Exception(it)) }

        val verifiedToken = authResult.accessToken.orEmpty()
        val userId = authResult.userId.orEmpty()
            .ifBlank { extractJwtSubject(verifiedToken).orEmpty() }
        if (userId.isBlank()) {
            return@withContext Result.failure(Exception("A confirmação recebida é inválida."))
        }

        val existing = producerDao.getProducerProfileOnce()
        val verifiedEmail = authResult.email.orEmpty()
            .ifBlank { existing?.email.orEmpty() }
            .trim()
            .lowercase()
        if (verifiedEmail.isBlank()) {
            return@withContext Result.failure(Exception("O e-mail confirmado é inválido."))
        }
        if (!canUseLocalProfile(existing?.email, verifiedEmail)) {
            return@withContext Result.failure(accountMismatchError())
        }

        val verifiedRefreshToken = authResult.refreshToken.orEmpty().ifBlank { refreshToken }
        val verifiedExpiresIn = authResult.expiresInSeconds
            .takeIf { it > 0 }
            ?: expiresInSeconds
        val expiresAt = tokenExpiryEpochSeconds(nowEpochSeconds(), verifiedExpiresIn)
        if (
            !secureSessionStore.save(
                SecureSession(
                    userId = userId,
                    accessToken = verifiedToken,
                    refreshToken = verifiedRefreshToken,
                    expiresAtEpochSeconds = expiresAt
                )
            )
        ) {
            return@withContext Result.failure(
                Exception("Não foi possível proteger a sessão neste celular.")
            )
        }

        adoptOfflineRows(existing?.remoteUserId, userId)
        val producer = (existing ?: ProducerEntity(
            nomeProdutor = verifiedEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
            email = verifiedEmail,
            nomePropriedade = "",
            municipioUF = "",
            dAPouCAF = "",
            areaTotalHectares = 0.0
        )).copy(
            email = verifiedEmail,
            isLoggedIn = true,
            remoteUserId = userId,
            accessToken = "",
            refreshToken = "",
            tokenExpiresAtEpochSeconds = 0
        )
        producerDao.insertOrUpdateProducer(producer)
        claimLegacyRows(userId)
        syncPendingData()
        Result.success(Unit)
    }

    suspend fun startOfflineProfile(
        nome: String,
        propriedade: String,
        municipio: String,
        area: Double
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val existing = producerDao.getProducerProfileOnce()
        if (existing != null && !existing.remoteUserId.startsWith(LOCAL_OWNER_PREFIX)) {
            return@withContext Result.failure(
                Exception("Este celular já pertence a uma conta. Entre com o e-mail cadastrado.")
            )
        }
        secureSessionStore.clear()
        val ownerUserId = existing?.remoteUserId
            ?.takeIf { it.startsWith(LOCAL_OWNER_PREFIX) }
            ?: "$LOCAL_OWNER_PREFIX${UUID.randomUUID()}"
        producerDao.insertOrUpdateProducer(
            ProducerEntity(
                id = 1,
                nomeProdutor = nome.trim().ifBlank { "Produtor de teste" },
                email = "",
                nomePropriedade = propriedade.trim().ifBlank { "Propriedade local" },
                municipioUF = municipio.trim(),
                dAPouCAF = existing?.dAPouCAF.orEmpty(),
                areaTotalHectares = area,
                isLoggedIn = true,
                remoteUserId = ownerUserId,
                syncStatus = SupabaseConfig.STATUS_LOCAL_OFFLINE,
                updatedAtEpochMillis = nextLocalTimestamp(existing?.updatedAtEpochMillis ?: 0)
            )
        )
        claimLegacyRows(ownerUserId)
        Result.success(Unit)
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        secureSessionStore.clear()
        val current = producerDao.getProducerProfileOnce() ?: return@withContext
        producerDao.insertOrUpdateProducer(
            current.copy(
                isLoggedIn = false,
                accessToken = "",
                refreshToken = "",
                tokenExpiresAtEpochSeconds = 0,
                syncStatus = SupabaseConfig.STATUS_LOCAL_OFFLINE
            )
        )
    }

    suspend fun insertCrop(crop: CropEntity) = withContext(Dispatchers.IO) {
        val ownerUserId = requireActiveUserId()
        val timestamp = nextLocalTimestamp(crop.updatedAtEpochMillis)
        cropDao.insertCrop(
            crop.copy(
                id = 0,
                ownerUserId = ownerUserId,
                syncStatus = SupabaseConfig.STATUS_LOCAL_OFFLINE,
                isDeleted = false,
                updatedAtEpochMillis = timestamp
            )
        )
        syncPendingData()
    }

    suspend fun updateCrop(crop: CropEntity) = withContext(Dispatchers.IO) {
        val ownerUserId = requireActiveUserId()
        val existing = cropDao.getById(crop.id)
            ?.takeIf { it.ownerUserId == ownerUserId }
            ?: return@withContext
        cropDao.updateCrop(
            crop.copy(
                cloudId = existing.cloudId,
                ownerUserId = ownerUserId,
                syncStatus = SupabaseConfig.STATUS_LOCAL_OFFLINE,
                isDeleted = false,
                updatedAtEpochMillis = nextLocalTimestamp(existing.updatedAtEpochMillis)
            )
        )
        syncPendingData()
    }

    suspend fun deleteCrop(cropId: Long) = withContext(Dispatchers.IO) {
        val ownerUserId = requireActiveUserId()
        val crop = cropDao.getById(cropId)?.takeIf { it.ownerUserId == ownerUserId }
            ?: return@withContext
        cropDao.markDeleted(
            cropId,
            SupabaseConfig.STATUS_LOCAL_OFFLINE,
            nextLocalTimestamp(crop.updatedAtEpochMillis)
        )
        syncPendingData()
    }

    suspend fun insertTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        val ownerUserId = requireActiveUserId()
        val cropCloudId = validCropAssociation(ownerUserId, task.cropCloudId)
        taskDao.insertTask(
            task.copy(
                id = 0,
                ownerUserId = ownerUserId,
                syncStatus = SupabaseConfig.STATUS_LOCAL_OFFLINE,
                isDeleted = false,
                updatedAtEpochMillis = nextLocalTimestamp(task.updatedAtEpochMillis),
                cropCloudId = cropCloudId
            )
        )
        syncPendingData()
    }

    suspend fun updateTaskStatus(taskId: Long, status: TaskStatus) =
        withContext(Dispatchers.IO) {
            val ownerUserId = requireActiveUserId()
            val task = taskDao.getTaskById(taskId)?.takeIf { it.ownerUserId == ownerUserId }
                ?: return@withContext
            taskDao.updateTaskStatus(
                taskId,
                status,
                SupabaseConfig.STATUS_LOCAL_OFFLINE,
                nextLocalTimestamp(task.updatedAtEpochMillis)
            )
            syncPendingData()
        }

    suspend fun updateTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        val ownerUserId = requireActiveUserId()
        val existing = taskDao.getTaskById(task.id)
            ?.takeIf { it.ownerUserId == ownerUserId }
            ?: return@withContext
        taskDao.updateTask(
            task.copy(
                cloudId = existing.cloudId,
                ownerUserId = ownerUserId,
                syncStatus = SupabaseConfig.STATUS_LOCAL_OFFLINE,
                isDeleted = false,
                updatedAtEpochMillis = nextLocalTimestamp(existing.updatedAtEpochMillis),
                cropCloudId = validCropAssociation(ownerUserId, task.cropCloudId)
            )
        )
        syncPendingData()
    }

    suspend fun deleteTask(taskId: Long) = withContext(Dispatchers.IO) {
        val ownerUserId = requireActiveUserId()
        val task = taskDao.getTaskById(taskId)?.takeIf { it.ownerUserId == ownerUserId }
            ?: return@withContext
        taskDao.markDeleted(
            taskId,
            SupabaseConfig.STATUS_LOCAL_OFFLINE,
            nextLocalTimestamp(task.updatedAtEpochMillis)
        )
        syncPendingData()
    }

    suspend fun insertTransaction(transaction: FinancialEntity) =
        withContext(Dispatchers.IO) {
            val ownerUserId = requireActiveUserId()
            val cropCloudId = validCropAssociation(ownerUserId, transaction.cropCloudId)
            financialDao.insertTransaction(
                transaction.copy(
                    id = 0,
                    ownerUserId = ownerUserId,
                    syncStatus = SupabaseConfig.STATUS_LOCAL_OFFLINE,
                    isDeleted = false,
                    updatedAtEpochMillis = nextLocalTimestamp(transaction.updatedAtEpochMillis),
                    cropCloudId = cropCloudId
                )
            )
            syncPendingData()
        }

    suspend fun updateTransaction(transaction: FinancialEntity) =
        withContext(Dispatchers.IO) {
            val ownerUserId = requireActiveUserId()
            val existing = financialDao.getById(transaction.id)
                ?.takeIf { it.ownerUserId == ownerUserId }
                ?: return@withContext
            financialDao.updateTransaction(
                transaction.copy(
                    cloudId = existing.cloudId,
                    ownerUserId = ownerUserId,
                    syncStatus = SupabaseConfig.STATUS_LOCAL_OFFLINE,
                    isDeleted = false,
                    updatedAtEpochMillis = nextLocalTimestamp(existing.updatedAtEpochMillis),
                    cropCloudId = validCropAssociation(ownerUserId, transaction.cropCloudId)
                )
            )
            syncPendingData()
        }

    suspend fun deleteTransaction(id: Long) = withContext(Dispatchers.IO) {
        val ownerUserId = requireActiveUserId()
        val transaction = financialDao.getById(id)?.takeIf { it.ownerUserId == ownerUserId }
            ?: return@withContext
        financialDao.markDeleted(
            id,
            SupabaseConfig.STATUS_LOCAL_OFFLINE,
            nextLocalTimestamp(transaction.updatedAtEpochMillis)
        )
        syncPendingData()
    }

    suspend fun saveProducerProfile(producer: ProducerEntity) = withContext(Dispatchers.IO) {
        val current = producerDao.getProducerProfileOnce() ?: return@withContext
        val ownerUserId = current.remoteUserId.takeIf(String::isNotBlank) ?: return@withContext
        val updated = producer.copy(
            id = 1,
            isLoggedIn = current.isLoggedIn,
            remoteUserId = ownerUserId,
            accessToken = "",
            refreshToken = "",
            tokenExpiresAtEpochSeconds = 0,
            syncStatus = SupabaseConfig.STATUS_LOCAL_OFFLINE,
            updatedAtEpochMillis = nextLocalTimestamp(current.updatedAtEpochMillis)
        )
        producerDao.insertOrUpdateProducer(updated)
        syncPendingData()
    }

    suspend fun createEncryptedBackup(password: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val producer = requireBackupProducer()
                val ownerUserId = producer.remoteUserId
                AgroBackupCodec.encode(
                    BackupSnapshot(
                        ownerUserId = ownerUserId,
                        ownerEmail = producer.email,
                        producer = ProducerBackup(
                            nomeProdutor = producer.nomeProdutor,
                            nomePropriedade = producer.nomePropriedade,
                            municipioUF = producer.municipioUF,
                            dAPouCAF = producer.dAPouCAF,
                            areaTotalHectares = producer.areaTotalHectares
                        ),
                        crops = cropDao.getOwnedRows(ownerUserId).filterNot { it.isDeleted },
                        tasks = taskDao.getOwnedRows(ownerUserId).filterNot { it.isDeleted },
                        transactions = financialDao.getOwnedRows(ownerUserId)
                            .filterNot { it.isDeleted }
                    ),
                    password
                )
            }
        }

    suspend fun restoreEncryptedBackup(
        serialized: String,
        password: String
    ): Result<BackupRestoreSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val current = requireBackupProducer()
            val snapshot = AgroBackupCodec.decode(serialized, password)
            if (snapshot.ownerUserId != current.remoteUserId) {
                throw BackupException(
                    "Este backup pertence a outra conta e não pode ser restaurado aqui."
                )
            }

            val currentCrops = cropDao.getOwnedRows(current.remoteUserId)
            val currentTasks = taskDao.getOwnedRows(current.remoteUserId)
            val currentTransactions = financialDao.getOwnedRows(current.remoteUserId)
            var timestamp = nextLocalTimestamp(
                maxOf(
                    current.updatedAtEpochMillis,
                    currentCrops.maxOfOrNull(CropEntity::updatedAtEpochMillis) ?: 0,
                    currentTasks.maxOfOrNull(TaskEntity::updatedAtEpochMillis) ?: 0,
                    currentTransactions.maxOfOrNull(FinancialEntity::updatedAtEpochMillis) ?: 0
                )
            )
            fun nextRestoreTimestamp(): Long = timestamp++

            val producer = current.copy(
                nomeProdutor = snapshot.producer.nomeProdutor,
                nomePropriedade = snapshot.producer.nomePropriedade,
                municipioUF = snapshot.producer.municipioUF,
                dAPouCAF = snapshot.producer.dAPouCAF,
                areaTotalHectares = snapshot.producer.areaTotalHectares,
                accessToken = "",
                refreshToken = "",
                tokenExpiresAtEpochSeconds = 0,
                syncStatus = SupabaseConfig.STATUS_LOCAL_OFFLINE,
                updatedAtEpochMillis = nextRestoreTimestamp()
            )
            val crops = snapshot.crops.map { crop ->
                crop.copy(
                    id = 0,
                    ownerUserId = current.remoteUserId,
                    syncStatus = SupabaseConfig.STATUS_LOCAL_OFFLINE,
                    isDeleted = false,
                    updatedAtEpochMillis = nextRestoreTimestamp()
                )
            }
            val tasks = snapshot.tasks.map { task ->
                task.copy(
                    id = 0,
                    ownerUserId = current.remoteUserId,
                    syncStatus = SupabaseConfig.STATUS_LOCAL_OFFLINE,
                    isDeleted = false,
                    updatedAtEpochMillis = nextRestoreTimestamp()
                )
            }
            val transactions = snapshot.transactions.map { transaction ->
                transaction.copy(
                    id = 0,
                    ownerUserId = current.remoteUserId,
                    syncStatus = SupabaseConfig.STATUS_LOCAL_OFFLINE,
                    isDeleted = false,
                    updatedAtEpochMillis = nextRestoreTimestamp()
                )
            }

            backupDao.mergeBackup(producer, crops, tasks, transactions)
            syncPendingData()
            BackupRestoreSummary(
                crops = crops.size,
                tasks = tasks.size,
                transactions = transactions.size
            )
        }
    }

    private suspend fun requireBackupProducer(): ProducerEntity {
        val producer = producerDao.getProducerProfileOnce()
            ?.takeIf { it.isLoggedIn && it.remoteUserId.isNotBlank() }
        return producer ?: throw BackupException(
            "Entre na sua conta antes de criar ou restaurar um backup."
        )
    }

    suspend fun syncPendingData(): Boolean = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            val localProducer = producerDao.getProducerProfileOnce()
            if (localProducer?.isLoggedIn != true) return@withLock true
            if (!SupabaseConfig.isConfigured) {
                markProducerSyncError(localProducer)
                return@withLock false
            }

            val session = getCloudSession()
            if (session == null) {
                markProducerSyncError(localProducer)
                return@withLock false
            }
            val schemaMode = SupabaseRestClient.detectSchema(session.accessToken)
            if (schemaMode == null || (schemaMode == SupabaseSchemaMode.LEGACY && session.email.isBlank())) {
                markProducerSyncError(localProducer)
                return@withLock false
            }
            claimLegacyRows(session.userId)

            val pullSucceeded = pullCloudState(session, schemaMode)
            if (!pullSucceeded) {
                producerDao.getProducerProfileOnce()?.let { markProducerSyncError(it) }
                return@withLock false
            }

            var pushSucceeded = pushProducer(session, schemaMode)
            cropDao.getPendingSync(session.userId, SupabaseConfig.STATUS_SYNCED_CLOUD)
                .forEach { pushSucceeded = pushCrop(session, schemaMode, it) && pushSucceeded }
            taskDao.getPendingSync(session.userId, SupabaseConfig.STATUS_SYNCED_CLOUD)
                .forEach { pushSucceeded = pushTask(session, schemaMode, it) && pushSucceeded }
            financialDao.getPendingSync(session.userId, SupabaseConfig.STATUS_SYNCED_CLOUD)
                .forEach {
                    pushSucceeded = pushTransaction(session, schemaMode, it) && pushSucceeded
                }

            pushSucceeded &&
                producerDao.getProducerProfileOnce()?.syncStatus == SupabaseConfig.STATUS_SYNCED_CLOUD &&
                cropDao.getPendingSync(session.userId, SupabaseConfig.STATUS_SYNCED_CLOUD).isEmpty() &&
                taskDao.getPendingSync(session.userId, SupabaseConfig.STATUS_SYNCED_CLOUD).isEmpty() &&
                financialDao.getPendingSync(session.userId, SupabaseConfig.STATUS_SYNCED_CLOUD).isEmpty()
        }
    }

    private suspend fun pullCloudState(
        session: CloudSession,
        schemaMode: SupabaseSchemaMode
    ): Boolean {
        val ownerColumn = schemaMode.ownerColumn()
        val ownerValue = session.ownerValue(schemaMode)
        val timestampColumn = schemaMode.timestampColumn()
        val profileResult = SupabaseRestClient.selectOwnedRows(
            "produtores",
            session.accessToken,
            ownerColumn,
            ownerValue,
            timestampColumn
        )
        val cropsResult = SupabaseRestClient.selectOwnedRows(
            "safras",
            session.accessToken,
            ownerColumn,
            ownerValue,
            timestampColumn
        )
        val tasksResult = SupabaseRestClient.selectOwnedRows(
            "tarefas",
            session.accessToken,
            ownerColumn,
            ownerValue,
            timestampColumn
        )
        val transactionsResult = SupabaseRestClient.selectOwnedRows(
            "financeiro",
            session.accessToken,
            ownerColumn,
            ownerValue,
            timestampColumn
        )
        val results = listOf(profileResult, cropsResult, tasksResult, transactionsResult)
        if (results.any { !it.success }) {
            Log.w("AgroSync", results.firstOrNull { !it.success }?.errorMessage.orEmpty())
            return false
        }

        applyRemoteProfile(session.userId, schemaMode, profileResult)
        applyRemoteCrops(session.userId, schemaMode, cropsResult)
        applyRemoteTasks(session.userId, schemaMode, tasksResult)
        applyRemoteTransactions(session.userId, schemaMode, transactionsResult)
        if (schemaMode == SupabaseSchemaMode.LEGACY) {
            reconcileLegacyCrops(session.userId, cropsResult.rows)
            reconcileLegacyTasks(session.userId, tasksResult.rows)
            reconcileLegacyTransactions(session.userId, transactionsResult.rows)
        }
        return true
    }

    private suspend fun applyRemoteProfile(
        userId: String,
        schemaMode: SupabaseSchemaMode,
        result: SyncResult
    ) {
        val local = producerDao.getProducerProfileOnce() ?: return
        val remote = result.rows.firstOrNull()
        if (remote == null) {
            if (local.syncStatus == SupabaseConfig.STATUS_SYNCED_CLOUD) {
                producerDao.insertOrUpdateProducer(
                    local.copy(
                        syncStatus = SupabaseConfig.STATUS_LOCAL_OFFLINE,
                        updatedAtEpochMillis = nextLocalTimestamp(local.updatedAtEpochMillis)
                    )
                )
            }
            return
        }
        val remoteTimestamp = remote.updatedAtMillis(schemaMode)
        if (remoteTimestamp < local.updatedAtEpochMillis) return

        producerDao.insertOrUpdateProducer(
            local.copy(
                nomeProdutor = remote.optString("nome_produtor", local.nomeProdutor),
                email = remote.optString(schemaMode.emailColumn(), local.email),
                nomePropriedade = remote.optString("nome_propriedade", local.nomePropriedade),
                municipioUF = remote.optString("municipio_uf", local.municipioUF),
                dAPouCAF = remote.optString("dap_caf", local.dAPouCAF),
                areaTotalHectares = remote.optDouble("area_hectares", local.areaTotalHectares),
                remoteUserId = userId,
                syncStatus = SupabaseConfig.STATUS_SYNCED_CLOUD,
                updatedAtEpochMillis = remoteTimestamp
            )
        )
    }

    private suspend fun applyRemoteCrops(
        userId: String,
        schemaMode: SupabaseSchemaMode,
        result: SyncResult
    ) {
        result.rows.forEach { remote ->
            val cloudId = remote.optString("id")
            if (cloudId.isBlank()) return@forEach
            val timestamp = remote.updatedAtMillis(schemaMode)
            val local = cropDao.getByCloudId(cloudId)
            if (local != null && !shouldApplyRemoteChange(local.updatedAtEpochMillis, timestamp)) {
                return@forEach
            }
            if (schemaMode == SupabaseSchemaMode.MODERN && remote.optBoolean("is_deleted")) {
                local?.let { cropDao.hardDelete(it.id) }
            } else {
                cropDao.insertCrop(
                    CropEntity(
                        id = local?.id ?: 0,
                        nomeCultura = remote.optString("nome_cultura"),
                        areaHectares = remote.optDouble("area_hectares"),
                        dataInicio = remote.optString("data_inicio"),
                        previsaoColheita = remote.optString("previsao_colheita"),
                        progressoPercentual = remote.optInt("progresso"),
                        statusManejo = remote.optString("status_manejo"),
                        syncStatus = SupabaseConfig.STATUS_SYNCED_CLOUD,
                        isDeleted = false,
                        cloudId = cloudId,
                        ownerUserId = userId,
                        updatedAtEpochMillis = timestamp
                    )
                )
            }
        }
    }

    private suspend fun applyRemoteTasks(
        userId: String,
        schemaMode: SupabaseSchemaMode,
        result: SyncResult
    ) {
        result.rows.forEach { remote ->
            val cloudId = remote.optString("id")
            if (cloudId.isBlank()) return@forEach
            val timestamp = remote.updatedAtMillis(schemaMode)
            val local = taskDao.getByCloudId(cloudId)
            if (local != null && !shouldApplyRemoteChange(local.updatedAtEpochMillis, timestamp)) {
                return@forEach
            }
            if (schemaMode == SupabaseSchemaMode.MODERN && remote.optBoolean("is_deleted")) {
                local?.let { taskDao.hardDelete(it.id) }
            } else {
                taskDao.insertTask(
                    TaskEntity(
                        id = local?.id ?: 0,
                        titulo = remote.optString("titulo"),
                        descricao = remote.optString("descricao"),
                        categoria = remote.optString("categoria"),
                        dataLimite = remote.optString("data_limite"),
                        status = remote.optString("status").toTaskStatus(),
                        syncStatus = SupabaseConfig.STATUS_SYNCED_CLOUD,
                        isDeleted = false,
                        cloudId = cloudId,
                        ownerUserId = userId,
                        updatedAtEpochMillis = timestamp,
                        cropCloudId = if (schemaMode == SupabaseSchemaMode.MODERN) {
                            remote.optionalString("crop_id")
                        } else {
                            local?.cropCloudId
                        }
                    )
                )
            }
        }
    }

    private suspend fun applyRemoteTransactions(
        userId: String,
        schemaMode: SupabaseSchemaMode,
        result: SyncResult
    ) {
        result.rows.forEach { remote ->
            val cloudId = remote.optString("id")
            if (cloudId.isBlank()) return@forEach
            val timestamp = remote.updatedAtMillis(schemaMode)
            val local = financialDao.getByCloudId(cloudId)
            if (local != null && !shouldApplyRemoteChange(local.updatedAtEpochMillis, timestamp)) {
                return@forEach
            }
            if (schemaMode == SupabaseSchemaMode.MODERN && remote.optBoolean("is_deleted")) {
                local?.let { financialDao.hardDelete(it.id) }
            } else {
                financialDao.insertTransaction(
                    FinancialEntity(
                        id = local?.id ?: 0,
                        descricao = remote.optString("descricao"),
                        valor = remote.optDouble("valor"),
                        tipo = remote.optString("tipo").toTransactionType(),
                        data = remote.optString("data"),
                        categoria = remote.optString("categoria"),
                        syncStatus = SupabaseConfig.STATUS_SYNCED_CLOUD,
                        isDeleted = false,
                        cloudId = cloudId,
                        ownerUserId = userId,
                        updatedAtEpochMillis = timestamp,
                        cropCloudId = if (schemaMode == SupabaseSchemaMode.MODERN) {
                            remote.optionalString("crop_id")
                        } else {
                            local?.cropCloudId
                        }
                    )
                )
            }
        }
    }

    private suspend fun pushProducer(
        session: CloudSession,
        schemaMode: SupabaseSchemaMode
    ): Boolean {
        val producer = producerDao.getProducerProfileOnce() ?: return true
        if (producer.syncStatus == SupabaseConfig.STATUS_SYNCED_CLOUD) return true
        val result = SupabaseRestClient.upsertRow(
            table = "produtores",
            accessToken = session.accessToken,
            conflictColumn = if (schemaMode == SupabaseSchemaMode.MODERN) "user_id" else "id",
            data = JSONObject().apply {
                if (schemaMode == SupabaseSchemaMode.MODERN) {
                    put("user_id", session.userId)
                    put("email", producer.email)
                } else {
                    put("id", session.userId)
                    put("user_email", session.email)
                }
                put("nome_produtor", producer.nomeProdutor)
                put("nome_propriedade", producer.nomePropriedade)
                put("municipio_uf", producer.municipioUF)
                put("dap_caf", producer.dAPouCAF)
                put("area_hectares", producer.areaTotalHectares)
                put(
                    schemaMode.timestampColumn(),
                    formatCloudTimestamp(producer.updatedAtEpochMillis)
                )
            }
        )
        val acknowledgedTimestamp = result.rows.firstOrNull()?.updatedAtMillis(schemaMode) ?: 0
        if (result.success && acknowledgedTimestamp > producer.updatedAtEpochMillis) {
            applyRemoteProfile(session.userId, schemaMode, result)
            return true
        }
        val accepted = result.success &&
            (acknowledgedTimestamp == 0L || acknowledgedTimestamp >= producer.updatedAtEpochMillis)
        producerDao.updateSyncStatusIfUnchanged(
            producer.updatedAtEpochMillis,
            if (accepted) {
                SupabaseConfig.STATUS_SYNCED_CLOUD
            } else {
                SupabaseConfig.STATUS_SYNC_ERROR
            }
        )
        return accepted
    }

    private suspend fun pushCrop(
        session: CloudSession,
        schemaMode: SupabaseSchemaMode,
        crop: CropEntity
    ): Boolean {
        if (schemaMode == SupabaseSchemaMode.LEGACY && crop.isDeleted) {
            val deleted = SupabaseRestClient.deleteOwnedRow(
                table = "safras",
                accessToken = session.accessToken,
                cloudId = crop.cloudId,
                ownerColumn = schemaMode.ownerColumn(),
                ownerValue = session.ownerValue(schemaMode)
            )
            if (deleted.success) {
                cropDao.hardDeleteIfUnchanged(crop.id, crop.updatedAtEpochMillis)
            }
            return deleted.success
        }
        val result = SupabaseRestClient.upsertRow(
            "safras",
            session.accessToken,
            JSONObject().apply {
                put("id", crop.cloudId)
                put(schemaMode.ownerColumn(), session.ownerValue(schemaMode))
                if (schemaMode == SupabaseSchemaMode.LEGACY) put("local_id", crop.id)
                put("nome_cultura", crop.nomeCultura)
                put("area_hectares", crop.areaHectares)
                put("data_inicio", crop.dataInicio)
                put("previsao_colheita", crop.previsaoColheita)
                put("progresso", crop.progressoPercentual)
                put("status_manejo", crop.statusManejo)
                put(schemaMode.timestampColumn(), formatCloudTimestamp(crop.updatedAtEpochMillis))
                if (schemaMode == SupabaseSchemaMode.MODERN) put("is_deleted", crop.isDeleted)
            }
        )
        val acknowledgedTimestamp = result.rows.firstOrNull()?.updatedAtMillis(schemaMode) ?: 0
        if (result.success && acknowledgedTimestamp > crop.updatedAtEpochMillis) {
            applyRemoteCrops(session.userId, schemaMode, result)
            return true
        }
        val accepted = result.success &&
            (acknowledgedTimestamp == 0L || acknowledgedTimestamp >= crop.updatedAtEpochMillis)
        if (accepted && crop.isDeleted) {
            cropDao.hardDeleteIfUnchanged(crop.id, crop.updatedAtEpochMillis)
        } else {
            cropDao.updateSyncStatusIfUnchanged(
                crop.id,
                crop.updatedAtEpochMillis,
                if (accepted) {
                    SupabaseConfig.STATUS_SYNCED_CLOUD
                } else {
                    SupabaseConfig.STATUS_SYNC_ERROR
                }
            )
        }
        return accepted
    }

    private suspend fun pushTask(
        session: CloudSession,
        schemaMode: SupabaseSchemaMode,
        task: TaskEntity
    ): Boolean {
        if (schemaMode == SupabaseSchemaMode.LEGACY && task.isDeleted) {
            val deleted = SupabaseRestClient.deleteOwnedRow(
                table = "tarefas",
                accessToken = session.accessToken,
                cloudId = task.cloudId,
                ownerColumn = schemaMode.ownerColumn(),
                ownerValue = session.ownerValue(schemaMode)
            )
            if (deleted.success) {
                taskDao.hardDeleteIfUnchanged(task.id, task.updatedAtEpochMillis)
            }
            return deleted.success
        }
        val result = SupabaseRestClient.upsertRow(
            "tarefas",
            session.accessToken,
            JSONObject().apply {
                put("id", task.cloudId)
                put(schemaMode.ownerColumn(), session.ownerValue(schemaMode))
                if (schemaMode == SupabaseSchemaMode.LEGACY) put("local_id", task.id)
                put("titulo", task.titulo)
                put("descricao", task.descricao)
                put("categoria", task.categoria)
                put("data_limite", task.dataLimite)
                put("status", task.status.name)
                put(schemaMode.timestampColumn(), formatCloudTimestamp(task.updatedAtEpochMillis))
                if (schemaMode == SupabaseSchemaMode.MODERN) {
                    put("crop_id", task.cropCloudId ?: JSONObject.NULL)
                    put("is_deleted", task.isDeleted)
                }
            }
        )
        val acknowledgedTimestamp = result.rows.firstOrNull()?.updatedAtMillis(schemaMode) ?: 0
        if (result.success && acknowledgedTimestamp > task.updatedAtEpochMillis) {
            applyRemoteTasks(session.userId, schemaMode, result)
            return true
        }
        val accepted = result.success &&
            (acknowledgedTimestamp == 0L || acknowledgedTimestamp >= task.updatedAtEpochMillis)
        if (accepted && task.isDeleted) {
            taskDao.hardDeleteIfUnchanged(task.id, task.updatedAtEpochMillis)
        } else {
            taskDao.updateSyncStatusIfUnchanged(
                task.id,
                task.updatedAtEpochMillis,
                if (accepted) {
                    SupabaseConfig.STATUS_SYNCED_CLOUD
                } else {
                    SupabaseConfig.STATUS_SYNC_ERROR
                }
            )
        }
        return accepted
    }

    private suspend fun pushTransaction(
        session: CloudSession,
        schemaMode: SupabaseSchemaMode,
        transaction: FinancialEntity
    ): Boolean {
        if (schemaMode == SupabaseSchemaMode.LEGACY && transaction.isDeleted) {
            val deleted = SupabaseRestClient.deleteOwnedRow(
                table = "financeiro",
                accessToken = session.accessToken,
                cloudId = transaction.cloudId,
                ownerColumn = schemaMode.ownerColumn(),
                ownerValue = session.ownerValue(schemaMode)
            )
            if (deleted.success) {
                financialDao.hardDeleteIfUnchanged(transaction.id, transaction.updatedAtEpochMillis)
            }
            return deleted.success
        }
        val result = SupabaseRestClient.upsertRow(
            "financeiro",
            session.accessToken,
            JSONObject().apply {
                put("id", transaction.cloudId)
                put(schemaMode.ownerColumn(), session.ownerValue(schemaMode))
                if (schemaMode == SupabaseSchemaMode.LEGACY) put("local_id", transaction.id)
                put("descricao", transaction.descricao)
                put("valor", transaction.valor)
                put("tipo", transaction.tipo.name)
                put("data", transaction.data)
                put("categoria", transaction.categoria)
                put(
                    schemaMode.timestampColumn(),
                    formatCloudTimestamp(transaction.updatedAtEpochMillis)
                )
                if (schemaMode == SupabaseSchemaMode.MODERN) {
                    put("crop_id", transaction.cropCloudId ?: JSONObject.NULL)
                    put("is_deleted", transaction.isDeleted)
                }
            }
        )
        val acknowledgedTimestamp = result.rows.firstOrNull()?.updatedAtMillis(schemaMode) ?: 0
        if (result.success && acknowledgedTimestamp > transaction.updatedAtEpochMillis) {
            applyRemoteTransactions(session.userId, schemaMode, result)
            return true
        }
        val accepted = result.success &&
            (acknowledgedTimestamp == 0L || acknowledgedTimestamp >= transaction.updatedAtEpochMillis)
        if (accepted && transaction.isDeleted) {
            financialDao.hardDeleteIfUnchanged(transaction.id, transaction.updatedAtEpochMillis)
        } else {
            financialDao.updateSyncStatusIfUnchanged(
                transaction.id,
                transaction.updatedAtEpochMillis,
                if (accepted) {
                    SupabaseConfig.STATUS_SYNCED_CLOUD
                } else {
                    SupabaseConfig.STATUS_SYNC_ERROR
                }
            )
        }
        return accepted
    }

    private suspend fun getCloudSession(): CloudSession? {
        val producer = producerDao.getProducerProfileOnce()?.takeIf { it.isLoggedIn } ?: return null
        val stored = secureSessionStore.read() ?: return null
        val userId = producer.remoteUserId.takeIf(String::isNotBlank) ?: return null
        if (stored.userId != userId) {
            secureSessionStore.clear()
            return null
        }
        if (!shouldRefreshToken(stored.expiresAtEpochSeconds, nowEpochSeconds())) {
            return CloudSession(stored.accessToken, userId, producer.email)
        }

        val refreshToken = stored.refreshToken.takeIf(String::isNotBlank) ?: return null
        val refreshed = SupabaseRestClient.refreshSession(refreshToken)
        val newAccessToken = refreshed.accessToken?.takeIf(String::isNotBlank) ?: return null
        val refreshedUserId = refreshed.userId.orEmpty()
            .ifBlank { extractJwtSubject(newAccessToken).orEmpty() }
        if (refreshedUserId.isBlank() || refreshedUserId != userId) return null

        val newSession = SecureSession(
            userId = userId,
            accessToken = newAccessToken,
            refreshToken = refreshed.refreshToken?.takeIf(String::isNotBlank) ?: refreshToken,
            expiresAtEpochSeconds = tokenExpiryEpochSeconds(
                nowEpochSeconds(),
                refreshed.expiresInSeconds
            )
        )
        if (!secureSessionStore.save(newSession)) return null
        return CloudSession(newAccessToken, userId, producer.email)
    }

    private suspend fun reconcileLegacyCrops(userId: String, remoteRows: List<JSONObject>) {
        val remoteIds = remoteRows.mapTo(mutableSetOf()) { it.optString("id") }
        cropDao.getOwnedRows(userId)
            .filter { row ->
                !row.isDeleted &&
                    row.syncStatus == SupabaseConfig.STATUS_SYNCED_CLOUD &&
                    row.cloudId !in remoteIds
            }
            .forEach { cropDao.hardDelete(it.id) }
    }

    private suspend fun reconcileLegacyTasks(userId: String, remoteRows: List<JSONObject>) {
        val remoteIds = remoteRows.mapTo(mutableSetOf()) { it.optString("id") }
        taskDao.getOwnedRows(userId)
            .filter { row ->
                !row.isDeleted &&
                    row.syncStatus == SupabaseConfig.STATUS_SYNCED_CLOUD &&
                    row.cloudId !in remoteIds
            }
            .forEach { taskDao.hardDelete(it.id) }
    }

    private suspend fun reconcileLegacyTransactions(
        userId: String,
        remoteRows: List<JSONObject>
    ) {
        val remoteIds = remoteRows.mapTo(mutableSetOf()) { it.optString("id") }
        financialDao.getOwnedRows(userId)
            .filter { row ->
                !row.isDeleted &&
                    row.syncStatus == SupabaseConfig.STATUS_SYNCED_CLOUD &&
                    row.cloudId !in remoteIds
            }
            .forEach { financialDao.hardDelete(it.id) }
    }

    private suspend fun requireActiveUserId(): String {
        val producer = producerDao.getProducerProfileOnce()?.takeIf { it.isLoggedIn }
            ?: throw IllegalStateException("Entre na sua conta para salvar dados.")
        return producer.remoteUserId.takeIf(String::isNotBlank)
            ?: throw IllegalStateException("A sessão atual não identifica o produtor.")
    }

    private suspend fun claimLegacyRows(userId: String) {
        val timestamp = System.currentTimeMillis()
        cropDao.claimUnownedRows(userId, SupabaseConfig.STATUS_LOCAL_OFFLINE, timestamp)
        taskDao.claimUnownedRows(userId, SupabaseConfig.STATUS_LOCAL_OFFLINE, timestamp)
        financialDao.claimUnownedRows(userId, SupabaseConfig.STATUS_LOCAL_OFFLINE, timestamp)
    }

    private suspend fun adoptOfflineRows(oldOwnerUserId: String?, newOwnerUserId: String) {
        val offlineOwner = oldOwnerUserId
            ?.takeIf { it.startsWith(LOCAL_OWNER_PREFIX) }
            ?: return
        val timestamp = System.currentTimeMillis()
        cropDao.reassignOwner(
            offlineOwner,
            newOwnerUserId,
            SupabaseConfig.STATUS_LOCAL_OFFLINE,
            timestamp
        )
        taskDao.reassignOwner(
            offlineOwner,
            newOwnerUserId,
            SupabaseConfig.STATUS_LOCAL_OFFLINE,
            timestamp
        )
        financialDao.reassignOwner(
            offlineOwner,
            newOwnerUserId,
            SupabaseConfig.STATUS_LOCAL_OFFLINE,
            timestamp
        )
        reportHistoryDao.reassignOwner(offlineOwner, newOwnerUserId)
        reportConsentDao.reassignOwner(offlineOwner, newOwnerUserId)
    }

    private suspend fun validCropAssociation(ownerUserId: String, cropCloudId: String?): String? {
        val requestedId = cropCloudId?.takeIf(String::isNotBlank) ?: return null
        val crop = cropDao.getByCloudId(requestedId) ?: return null
        return requestedId.takeIf { crop.ownerUserId == ownerUserId && !crop.isDeleted }
    }

    private suspend fun markProducerSyncError(producer: ProducerEntity) {
        producerDao.updateSyncStatusIfUnchanged(
            producer.updatedAtEpochMillis,
            SupabaseConfig.STATUS_SYNC_ERROR
        )
    }

    private fun migrateLegacySession(producer: ProducerEntity): SecureSession? {
        val accessToken = producer.accessToken.takeIf(String::isNotBlank) ?: return null
        val userId = producer.remoteUserId.ifBlank {
            extractJwtSubject(accessToken).orEmpty()
        }
        if (userId.isBlank()) return null
        val session = SecureSession(
            userId = userId,
            accessToken = accessToken,
            refreshToken = producer.refreshToken,
            expiresAtEpochSeconds = producer.tokenExpiresAtEpochSeconds
        )
        return session.takeIf(secureSessionStore::save)
    }

    private fun extractJwtSubject(token: String): String? = runCatching {
        val payload = token.split('.').getOrNull(1) ?: return@runCatching null
        val decoded = Base64.decode(
            payload,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        JSONObject(String(decoded, StandardCharsets.UTF_8))
            .optString("sub")
            .takeIf(String::isNotBlank)
    }.getOrNull()

    private fun JSONObject.updatedAtMillis(schemaMode: SupabaseSchemaMode): Long =
        parseCloudTimestamp(optString(schemaMode.timestampColumn())) ?: 0

    private fun SupabaseSchemaMode.ownerColumn(): String =
        if (this == SupabaseSchemaMode.MODERN) "user_id" else "user_email"

    private fun SupabaseSchemaMode.timestampColumn(): String =
        if (this == SupabaseSchemaMode.MODERN) "updated_at" else "atualizado_em"

    private fun SupabaseSchemaMode.emailColumn(): String =
        if (this == SupabaseSchemaMode.MODERN) "email" else "user_email"

    private fun CloudSession.ownerValue(schemaMode: SupabaseSchemaMode): String =
        if (schemaMode == SupabaseSchemaMode.MODERN) userId else email

    private fun JSONObject.optionalString(name: String): String? =
        if (isNull(name)) null else optString(name).takeIf(String::isNotBlank)

    private fun String.toTaskStatus(): TaskStatus =
        runCatching { TaskStatus.valueOf(this) }.getOrDefault(TaskStatus.A_FAZER)

    private fun String.toTransactionType(): TransactionType =
        runCatching { TransactionType.valueOf(this) }.getOrDefault(TransactionType.SAIDA)

    private fun nowEpochSeconds(): Long = System.currentTimeMillis() / 1000

    private fun accountMismatchError() = Exception(
        "Este celular contém dados de outra conta. " +
            "Saia e exporte os dados antes de trocar de produtor."
    )

    private data class CloudSession(
        val accessToken: String,
        val userId: String,
        val email: String
    )

    private companion object {
        const val LOCAL_OWNER_PREFIX = "local:"
    }

}

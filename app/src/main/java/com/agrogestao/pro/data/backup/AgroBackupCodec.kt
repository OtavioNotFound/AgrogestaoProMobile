package com.agrogestao.pro.data.backup

import android.util.Base64
import com.agrogestao.pro.data.local.entities.CropEntity
import com.agrogestao.pro.data.local.entities.FinancialEntity
import com.agrogestao.pro.data.local.entities.TaskEntity
import com.agrogestao.pro.data.local.entities.TaskStatus
import com.agrogestao.pro.data.local.entities.TransactionType
import com.agrogestao.pro.domain.isoDateParts
import com.agrogestao.pro.domain.moneyToCents
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import org.json.JSONArray
import org.json.JSONObject

data class ProducerBackup(
    val nomeProdutor: String,
    val nomePropriedade: String,
    val municipioUF: String,
    val dAPouCAF: String,
    val areaTotalHectares: Double
)

data class BackupSnapshot(
    val ownerUserId: String,
    val ownerEmail: String,
    val producer: ProducerBackup,
    val crops: List<CropEntity>,
    val tasks: List<TaskEntity>,
    val transactions: List<FinancialEntity>,
    val exportedAtEpochMillis: Long = System.currentTimeMillis()
)

data class BackupRestoreSummary(
    val crops: Int,
    val tasks: Int,
    val transactions: Int
)

class BackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

object AgroBackupCodec {
    const val MIME_TYPE = "application/json"
    const val FILE_EXTENSION = "agrobackup"
    const val MIN_PASSWORD_LENGTH = 8
    const val MAX_FILE_CHARS = 10 * 1024 * 1024

    private const val ENVELOPE_FORMAT = "agrogestao-encrypted-backup"
    private const val PAYLOAD_FORMAT = "agrogestao-data"
    private const val ENVELOPE_FORMAT_VERSION = 1
    private const val LEGACY_PAYLOAD_VERSION = 1
    private const val PAYLOAD_FORMAT_VERSION = 2
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA1"
    private const val ITERATIONS = 210_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val GCM_TAG_BITS = 128
    private const val MAX_RECORDS_PER_TYPE = 20_000
    private val associatedData = "AgroGestaoBackupV1".toByteArray(StandardCharsets.UTF_8)

    fun encode(snapshot: BackupSnapshot, password: String): String {
        validatePassword(password)
        validateSnapshot(snapshot)
        val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
        val iv = ByteArray(IV_BYTES).also(SecureRandom()::nextBytes)
        val key = deriveKey(password, salt, ITERATIONS, PBKDF2_ALGORITHM)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            updateAAD(associatedData)
        }
        val encrypted = cipher.doFinal(snapshot.toJson().toString().toByteArray(StandardCharsets.UTF_8))
        return JSONObject().apply {
            put("format", ENVELOPE_FORMAT)
            put("version", ENVELOPE_FORMAT_VERSION)
            put("kdf", PBKDF2_ALGORITHM)
            put("iterations", ITERATIONS)
            put("salt", salt.toBase64())
            put("iv", iv.toBase64())
            put("ciphertext", encrypted.toBase64())
        }.toString()
    }

    fun decode(serialized: String, password: String): BackupSnapshot {
        validatePassword(password)
        checkBackup(serialized.isNotBlank(), "O arquivo de backup está vazio.")
        checkBackup(
            serialized.length <= MAX_FILE_CHARS,
            "O arquivo de backup excede o tamanho permitido."
        )
        try {
            val envelope = JSONObject(serialized)
            checkBackup(envelope.optString("format") == ENVELOPE_FORMAT, "Formato de backup inválido.")
            checkBackup(
                envelope.optInt("version", -1) == ENVELOPE_FORMAT_VERSION,
                "Esta versão do backup ainda não é compatível com o aplicativo."
            )
            val algorithm = envelope.optString("kdf")
            checkBackup(algorithm == PBKDF2_ALGORITHM, "Proteção do backup não reconhecida.")
            val iterations = envelope.optInt("iterations", 0)
            checkBackup(iterations in 100_000..600_000, "Parâmetros de proteção inválidos.")
            val salt = envelope.requiredBase64("salt", SALT_BYTES, SALT_BYTES)
            val iv = envelope.requiredBase64("iv", IV_BYTES, IV_BYTES)
            val ciphertext = envelope.requiredBase64("ciphertext", 17, MAX_FILE_CHARS)
            val key = deriveKey(password, salt, iterations, algorithm)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
                updateAAD(associatedData)
            }
            val payload = try {
                cipher.doFinal(ciphertext)
            } catch (error: Exception) {
                throw BackupException("Senha incorreta ou arquivo de backup danificado.", error)
            }
            val snapshot = parseSnapshot(JSONObject(String(payload, StandardCharsets.UTF_8)))
            validateSnapshot(snapshot)
            return snapshot
        } catch (error: BackupException) {
            throw error
        } catch (error: Exception) {
            throw BackupException("O arquivo selecionado não é um backup válido do AgroGestão.", error)
        }
    }

    private fun deriveKey(
        password: String,
        salt: ByteArray,
        iterations: Int,
        algorithm: String
    ): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS)
        return try {
            val bytes = SecretKeyFactory.getInstance(algorithm).generateSecret(spec).encoded
            SecretKeySpec(bytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    private fun BackupSnapshot.toJson() = JSONObject().apply {
        put("format", PAYLOAD_FORMAT)
        put("version", PAYLOAD_FORMAT_VERSION)
        put("exported_at_epoch_ms", exportedAtEpochMillis)
        put("owner_user_id", ownerUserId)
        put("owner_email", ownerEmail)
        put("producer", JSONObject().apply {
            put("nome_produtor", producer.nomeProdutor)
            put("nome_propriedade", producer.nomePropriedade)
            put("municipio_uf", producer.municipioUF)
            put("dap_caf", producer.dAPouCAF)
            put("area_total_hectares", producer.areaTotalHectares)
        })
        put("crops", JSONArray().apply {
            crops.forEach { crop ->
                put(JSONObject().apply {
                    put("cloud_id", crop.cloudId)
                    put("nome_cultura", crop.nomeCultura)
                    put("area_hectares", crop.areaHectares)
                    put("data_inicio", crop.dataInicio)
                    put("previsao_colheita", crop.previsaoColheita)
                    put("progresso", crop.progressoPercentual)
                    put("status_manejo", crop.statusManejo)
                })
            }
        })
        put("tasks", JSONArray().apply {
            tasks.forEach { task ->
                put(JSONObject().apply {
                    put("cloud_id", task.cloudId)
                    put("titulo", task.titulo)
                    put("descricao", task.descricao)
                    put("categoria", task.categoria)
                    put("data_limite", task.dataLimite)
                    put("status", task.status.name)
                    put("crop_cloud_id", task.cropCloudId ?: JSONObject.NULL)
                })
            }
        })
        put("transactions", JSONArray().apply {
            transactions.forEach { transaction ->
                put(JSONObject().apply {
                    put("cloud_id", transaction.cloudId)
                    put("descricao", transaction.descricao)
                    put("valor_centavos", transaction.valorCentavos)
                    put("tipo", transaction.tipo.name)
                    put("data", transaction.data)
                    put("categoria", transaction.categoria)
                    put("crop_cloud_id", transaction.cropCloudId ?: JSONObject.NULL)
                })
            }
        })
    }

    private fun parseSnapshot(payload: JSONObject): BackupSnapshot {
        checkBackup(payload.optString("format") == PAYLOAD_FORMAT, "Conteúdo do backup inválido.")
        val payloadVersion = payload.optInt("version", -1)
        checkBackup(
            payloadVersion in LEGACY_PAYLOAD_VERSION..PAYLOAD_FORMAT_VERSION,
            "Versão dos dados do backup incompatível."
        )
        val ownerUserId = payload.requiredText("owner_user_id", 200, allowBlank = false)
        val ownerEmail = payload.requiredText("owner_email", 320)
        val producerJson = payload.getJSONObject("producer")
        val producer = ProducerBackup(
            nomeProdutor = producerJson.requiredText("nome_produtor", 500),
            nomePropriedade = producerJson.requiredText("nome_propriedade", 500),
            municipioUF = producerJson.requiredText("municipio_uf", 500),
            dAPouCAF = producerJson.requiredText("dap_caf", 500),
            areaTotalHectares = producerJson.requiredFiniteDouble("area_total_hectares", 0.0)
        )
        val crops = payload.getJSONArray("crops").toObjects().map { json ->
            CropEntity(
                nomeCultura = json.requiredText("nome_cultura", 500),
                areaHectares = json.requiredFiniteDouble("area_hectares", 0.0),
                dataInicio = json.requiredDate("data_inicio"),
                previsaoColheita = json.requiredDate("previsao_colheita"),
                progressoPercentual = json.getInt("progresso"),
                statusManejo = json.requiredText("status_manejo", 500),
                cloudId = json.requiredUuid("cloud_id"),
                ownerUserId = ownerUserId
            )
        }
        val cropIds = crops.mapTo(hashSetOf(), CropEntity::cloudId)
        val tasks = payload.getJSONArray("tasks").toObjects().map { json ->
            val association = json.optionalUuid("crop_cloud_id")?.takeIf(cropIds::contains)
            TaskEntity(
                titulo = json.requiredText("titulo", 500),
                descricao = json.requiredText("descricao", 5_000),
                categoria = json.requiredText("categoria", 500),
                dataLimite = json.requiredDate("data_limite"),
                status = json.requiredEnum<TaskStatus>("status"),
                cloudId = json.requiredUuid("cloud_id"),
                ownerUserId = ownerUserId,
                cropCloudId = association
            )
        }
        val transactions = payload.getJSONArray("transactions").toObjects().map { json ->
            val association = json.optionalUuid("crop_cloud_id")?.takeIf(cropIds::contains)
            val valueCents = if (payloadVersion >= PAYLOAD_FORMAT_VERSION) {
                json.requiredLong("valor_centavos", 0L)
            } else {
                moneyToCents(json.requiredFiniteDouble("valor", 0.0))
            }
            FinancialEntity(
                descricao = json.requiredText("descricao", 500),
                valorCentavos = valueCents,
                tipo = json.requiredEnum<TransactionType>("tipo"),
                data = json.requiredDate("data"),
                categoria = json.requiredText("categoria", 500),
                cloudId = json.requiredUuid("cloud_id"),
                ownerUserId = ownerUserId,
                cropCloudId = association
            )
        }
        return BackupSnapshot(
            ownerUserId = ownerUserId,
            ownerEmail = ownerEmail,
            producer = producer,
            crops = crops,
            tasks = tasks,
            transactions = transactions,
            exportedAtEpochMillis = payload.optLong("exported_at_epoch_ms", 0)
        )
    }

    private fun validateSnapshot(snapshot: BackupSnapshot) {
        checkBackup(snapshot.ownerUserId.isNotBlank(), "O backup não identifica o produtor.")
        checkBackup(snapshot.ownerUserId.length <= 200, "Identificação do produtor inválida.")
        checkBackup(snapshot.ownerEmail.length <= 320, "E-mail do produtor inválido.")
        checkBackup(snapshot.producer.nomeProdutor.length <= 500, "Nome do produtor inválido.")
        checkBackup(snapshot.producer.nomePropriedade.length <= 500, "Nome da propriedade inválido.")
        checkBackup(snapshot.producer.municipioUF.length <= 500, "Município inválido.")
        checkBackup(snapshot.producer.dAPouCAF.length <= 500, "DAP ou CAF inválido.")
        checkBackup(
            snapshot.producer.areaTotalHectares.isFinite() && snapshot.producer.areaTotalHectares >= 0,
            "Área total inválida no backup."
        )
        checkBackup(snapshot.crops.size <= MAX_RECORDS_PER_TYPE, "O backup contém safras demais.")
        checkBackup(snapshot.tasks.size <= MAX_RECORDS_PER_TYPE, "O backup contém tarefas demais.")
        checkBackup(
            snapshot.transactions.size <= MAX_RECORDS_PER_TYPE,
            "O backup contém lançamentos demais."
        )
        validateUniqueCloudIds(snapshot.crops.map(CropEntity::cloudId), "safras")
        validateUniqueCloudIds(snapshot.tasks.map(TaskEntity::cloudId), "tarefas")
        validateUniqueCloudIds(snapshot.transactions.map(FinancialEntity::cloudId), "lançamentos")
        snapshot.crops.forEach { crop ->
            checkBackup(crop.nomeCultura.length <= 500, "Nome de safra inválido.")
            checkBackup(crop.areaHectares.isFinite() && crop.areaHectares >= 0, "Área de safra inválida.")
            checkBackup(crop.progressoPercentual in 0..100, "Progresso de safra inválido.")
            checkBackup(crop.statusManejo.length <= 500, "Situação de manejo inválida.")
            validateDate(crop.dataInicio)
            validateDate(crop.previsaoColheita)
            validateUuid(crop.cloudId)
        }
        snapshot.tasks.forEach { task ->
            checkBackup(task.titulo.length <= 500, "Título de tarefa inválido.")
            checkBackup(task.descricao.length <= 5_000, "Descrição de tarefa inválida.")
            checkBackup(task.categoria.length <= 500, "Categoria de tarefa inválida.")
            validateDate(task.dataLimite)
            validateUuid(task.cloudId)
            task.cropCloudId?.let(::validateUuid)
        }
        snapshot.transactions.forEach { transaction ->
            checkBackup(transaction.descricao.length <= 500, "Descrição de lançamento inválida.")
            checkBackup(transaction.valorCentavos >= 0L, "Valor inválido.")
            checkBackup(transaction.categoria.length <= 500, "Categoria de lançamento inválida.")
            validateDate(transaction.data)
            validateUuid(transaction.cloudId)
            transaction.cropCloudId?.let(::validateUuid)
        }
    }

    private fun validatePassword(password: String) {
        checkBackup(
            password.length >= MIN_PASSWORD_LENGTH,
            "A senha do backup precisa ter pelo menos $MIN_PASSWORD_LENGTH caracteres."
        )
        checkBackup(password.length <= 200, "A senha do backup é muito longa.")
    }

    private fun validateUniqueCloudIds(ids: List<String>, label: String) {
        checkBackup(ids.toSet().size == ids.size, "O backup contém $label duplicados.")
    }

    private fun validateUuid(value: String) {
        checkBackup(runCatching { UUID.fromString(value) }.isSuccess, "Identificador inválido no backup.")
    }

    private fun validateDate(value: String) {
        checkBackup(
            value.isBlank() || isoDateParts(value) != null,
            "Data inválida no backup."
        )
    }

    private fun JSONArray.toObjects(): List<JSONObject> {
        checkBackup(length() <= MAX_RECORDS_PER_TYPE, "O backup excede o limite de registros.")
        return buildList(length()) {
            for (index in 0 until length()) add(getJSONObject(index))
        }
    }

    private fun JSONObject.requiredText(
        name: String,
        maxLength: Int,
        allowBlank: Boolean = true
    ): String {
        val value = getString(name)
        checkBackup(value.length <= maxLength, "Campo de texto inválido no backup.")
        checkBackup(allowBlank || value.isNotBlank(), "Campo obrigatório ausente no backup.")
        return value
    }

    private fun JSONObject.requiredFiniteDouble(name: String, minimum: Double): Double {
        val value = getDouble(name)
        checkBackup(value.isFinite() && value >= minimum, "Número inválido no backup.")
        return value
    }

    private fun JSONObject.requiredLong(name: String, minimum: Long): Long {
        val value = getLong(name)
        checkBackup(value >= minimum, "Número inválido no backup.")
        return value
    }

    private fun JSONObject.requiredDate(name: String): String =
        requiredText(name, 10).also(::validateDate)

    private fun JSONObject.requiredUuid(name: String): String =
        requiredText(name, 36, allowBlank = false).also(::validateUuid)

    private fun JSONObject.optionalUuid(name: String): String? {
        if (isNull(name)) return null
        return requiredUuid(name)
    }

    private inline fun <reified T : Enum<T>> JSONObject.requiredEnum(name: String): T {
        val value = requiredText(name, 100, allowBlank = false)
        return runCatching { enumValueOf<T>(value) }
            .getOrElse { throw BackupException("Opção inválida no backup.", it) }
    }

    private fun JSONObject.requiredBase64(
        name: String,
        minimumBytes: Int,
        maximumBytes: Int
    ): ByteArray {
        val decoded = runCatching { Base64.decode(getString(name), Base64.NO_WRAP) }
            .getOrElse { throw BackupException("Conteúdo protegido inválido.", it) }
        checkBackup(decoded.size in minimumBytes..maximumBytes, "Conteúdo protegido inválido.")
        return decoded
    }

    private fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)

    private fun checkBackup(condition: Boolean, message: String) {
        if (!condition) throw BackupException(message)
    }
}

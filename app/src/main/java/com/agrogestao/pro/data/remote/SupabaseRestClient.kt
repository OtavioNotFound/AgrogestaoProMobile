package com.agrogestao.pro.data.remote

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URI
import java.net.UnknownHostException
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class SupabaseAuthResponse(
    val accessToken: String?,
    val userId: String?,
    val email: String?,
    val errorMessage: String?,
    val refreshToken: String? = null,
    val expiresInSeconds: Long = 0,
    val errorCode: String? = null
)

data class SupabaseActionResponse(
    val success: Boolean,
    val errorMessage: String? = null,
    val errorCode: String? = null
)

data class SyncResult(
    val success: Boolean,
    val rows: List<JSONObject> = emptyList(),
    val errorMessage: String? = null
)

enum class SupabaseSchemaMode {
    MODERN,
    LEGACY
}

object SupabaseRestClient {

    private const val TAG = "SupabaseRestClient"
    private const val CONNECT_TIMEOUT_MILLIS = 12_000
    private const val READ_TIMEOUT_MILLIS = 15_000

    suspend fun signUp(email: String, password: String): SupabaseAuthResponse {
        if (!SupabaseConfig.isConfigured) return missingConfigurationAuthResult()
        return try {
            val redirect = URLEncoder.encode(
                SupabaseConfig.AUTH_CALLBACK_URL,
                StandardCharsets.UTF_8.name()
            )
            val response = request(
                method = "POST",
                path = "/auth/v1/signup?redirect_to=$redirect",
                body = JSONObject().apply {
                    put("email", email)
                    put("password", password)
                }
            )
            Log.d(TAG, "signUp concluído com HTTP ${response.code}")
            response.toAuthResponse(fallbackEmail = email)
        } catch (error: Exception) {
            Log.e(TAG, "Falha de rede no cadastro", error)
            SupabaseAuthResponse(null, null, null, connectionMessage(error))
        }
    }

    suspend fun resendSignUpConfirmation(email: String): SupabaseActionResponse {
        if (!SupabaseConfig.isConfigured) {
            return SupabaseActionResponse(
                success = false,
                errorMessage = "A conexão com a nuvem ainda não foi configurada neste aplicativo."
            )
        }
        return try {
            val redirect = URLEncoder.encode(
                SupabaseConfig.AUTH_CALLBACK_URL,
                StandardCharsets.UTF_8.name()
            )
            val response = request(
                method = "POST",
                path = "/auth/v1/resend?redirect_to=$redirect",
                body = JSONObject().apply {
                    put("email", email)
                    put("type", "signup")
                }
            )
            Log.d(TAG, "resendSignUpConfirmation concluído com HTTP ${response.code}")
            response.toActionResponse()
        } catch (error: Exception) {
            Log.e(TAG, "Falha de rede ao reenviar confirmação", error)
            SupabaseActionResponse(false, connectionMessage(error))
        }
    }

    suspend fun requestPasswordRecovery(email: String): SupabaseActionResponse {
        if (!SupabaseConfig.isConfigured) {
            return SupabaseActionResponse(
                success = false,
                errorMessage = "A conexão com a nuvem ainda não foi configurada neste aplicativo."
            )
        }
        return try {
            val redirect = URLEncoder.encode(
                SupabaseConfig.AUTH_CALLBACK_URL,
                StandardCharsets.UTF_8.name()
            )
            val response = request(
                method = "POST",
                path = "/auth/v1/recover?redirect_to=$redirect",
                body = JSONObject().apply { put("email", email) }
            )
            Log.d(TAG, "requestPasswordRecovery concluído com HTTP ${response.code}")
            response.toActionResponse()
        } catch (error: Exception) {
            Log.e(TAG, "Falha de rede na recuperação de senha", error)
            SupabaseActionResponse(false, connectionMessage(error))
        }
    }

    suspend fun updatePassword(
        accessToken: String,
        newPassword: String
    ): SupabaseActionResponse {
        if (!SupabaseConfig.isConfigured) {
            return SupabaseActionResponse(
                success = false,
                errorMessage = "A conexão com a nuvem ainda não foi configurada neste aplicativo."
            )
        }
        return try {
            val response = request(
                method = "PUT",
                path = "/auth/v1/user",
                accessToken = accessToken,
                body = JSONObject().apply { put("password", newPassword) }
            )
            Log.d(TAG, "updatePassword concluído com HTTP ${response.code}")
            response.toActionResponse()
        } catch (error: Exception) {
            Log.e(TAG, "Falha de rede ao atualizar senha", error)
            SupabaseActionResponse(false, connectionMessage(error))
        }
    }

    suspend fun signIn(email: String, password: String): SupabaseAuthResponse {
        if (!SupabaseConfig.isConfigured) return missingConfigurationAuthResult()
        return try {
            val response = request(
                method = "POST",
                path = "/auth/v1/token?grant_type=password",
                body = JSONObject().apply {
                    put("email", email)
                    put("password", password)
                }
            )
            Log.d(TAG, "signIn concluído com HTTP ${response.code}")
            response.toAuthResponse(fallbackEmail = email)
        } catch (error: Exception) {
            Log.e(TAG, "Falha de rede no login", error)
            SupabaseAuthResponse(null, null, null, connectionMessage(error))
        }
    }

    suspend fun refreshSession(refreshToken: String): SupabaseAuthResponse {
        if (!SupabaseConfig.isConfigured) return missingConfigurationAuthResult()
        return try {
            val response = request(
                method = "POST",
                path = "/auth/v1/token?grant_type=refresh_token",
                body = JSONObject().apply { put("refresh_token", refreshToken) }
            )
            Log.d(TAG, "refreshSession concluído com HTTP ${response.code}")
            response.toAuthResponse(fallbackEmail = null)
        } catch (error: Exception) {
            Log.e(TAG, "Falha de rede ao renovar a sessão", error)
            SupabaseAuthResponse(null, null, null, connectionMessage(error))
        }
    }

    suspend fun validateAccessToken(accessToken: String): SupabaseAuthResponse {
        if (!SupabaseConfig.isConfigured) return missingConfigurationAuthResult()
        return try {
            val response = request(
                method = "GET",
                path = "/auth/v1/user",
                accessToken = accessToken
            )
            Log.d(TAG, "validateAccessToken concluído com HTTP ${response.code}")
            response.toUserAuthResponse(accessToken)
        } catch (error: Exception) {
            Log.e(TAG, "Falha de rede ao validar confirmação", error)
            SupabaseAuthResponse(null, null, null, connectionMessage(error))
        }
    }

    fun detectSchema(accessToken: String): SupabaseSchemaMode? {
        if (!SupabaseConfig.isConfigured) return null
        return try {
            val modern = request(
                method = "GET",
                path = "/rest/v1/produtores?select=user_id&limit=0",
                accessToken = accessToken
            )
            if (modern.code in 200..299) return SupabaseSchemaMode.MODERN

            val legacy = request(
                method = "GET",
                path = "/rest/v1/produtores?select=user_email&limit=0",
                accessToken = accessToken
            )
            if (legacy.code in 200..299) SupabaseSchemaMode.LEGACY else null
        } catch (error: Exception) {
            Log.e(TAG, "Falha de rede ao identificar o esquema da nuvem", error)
            null
        }
    }

    fun selectOwnedRows(
        table: String,
        accessToken: String,
        ownerColumn: String,
        ownerValue: String,
        timestampColumn: String
    ): SyncResult {
        if (!SupabaseConfig.isConfigured) return missingConfigurationSyncResult()
        return try {
            val encodedOwner = URLEncoder.encode(ownerValue, StandardCharsets.UTF_8.name())
            val response = request(
                method = "GET",
                path = "/rest/v1/$table?select=*&$ownerColumn=eq.$encodedOwner&order=$timestampColumn.asc",
                accessToken = accessToken
            )
            Log.d(TAG, "selectOwnedRows [$table] [${response.code}]")
            response.toSyncResult()
        } catch (error: Exception) {
            Log.e(TAG, "Falha de rede ao baixar $table", error)
            SyncResult(false, errorMessage = connectionMessage(error))
        }
    }

    fun deleteOwnedRow(
        table: String,
        accessToken: String,
        cloudId: String,
        ownerColumn: String,
        ownerValue: String
    ): SyncResult {
        if (!SupabaseConfig.isConfigured) return missingConfigurationSyncResult()
        return try {
            val encodedId = URLEncoder.encode(cloudId, StandardCharsets.UTF_8.name())
            val encodedOwner = URLEncoder.encode(ownerValue, StandardCharsets.UTF_8.name())
            val response = request(
                method = "DELETE",
                path = "/rest/v1/$table?id=eq.$encodedId&$ownerColumn=eq.$encodedOwner",
                accessToken = accessToken,
                extraHeaders = mapOf("Prefer" to "return=representation")
            )
            Log.d(TAG, "deleteOwnedRow [$table] [${response.code}]")
            response.toSyncResult()
        } catch (error: Exception) {
            Log.e(TAG, "Falha de rede ao excluir $table", error)
            SyncResult(false, errorMessage = connectionMessage(error))
        }
    }

    fun upsertRow(
        table: String,
        accessToken: String,
        data: JSONObject,
        conflictColumn: String = "id"
    ): SyncResult {
        if (!SupabaseConfig.isConfigured) return missingConfigurationSyncResult()
        return try {
            val encodedConflict = URLEncoder.encode(conflictColumn, StandardCharsets.UTF_8.name())
            val response = request(
                method = "POST",
                path = "/rest/v1/$table?on_conflict=$encodedConflict",
                accessToken = accessToken,
                body = data,
                extraHeaders = mapOf(
                    "Prefer" to "resolution=merge-duplicates,return=representation"
                )
            )
            Log.d(TAG, "upsertRow [$table] [${response.code}]")
            response.toSyncResult()
        } catch (error: Exception) {
            Log.e(TAG, "Falha de rede ao enviar $table", error)
            SyncResult(false, errorMessage = connectionMessage(error))
        }
    }

    private fun request(
        method: String,
        path: String,
        accessToken: String? = null,
        body: JSONObject? = null,
        extraHeaders: Map<String, String> = emptyMap()
    ): HttpResponse {
        val url = URL(URI.create("${SupabaseConfig.SUPABASE_URL}$path").toASCIIString())
        val connection = url.openConnection() as HttpURLConnection
        return try {
            connection.apply {
                requestMethod = method
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                setRequestProperty("X-Supabase-Api-Version", "2024-01-01")
                if (!accessToken.isNullOrBlank()) {
                    setRequestProperty("Authorization", "Bearer $accessToken")
                }
                extraHeaders.forEach(::setRequestProperty)
                connectTimeout = CONNECT_TIMEOUT_MILLIS
                readTimeout = READ_TIMEOUT_MILLIS
                doOutput = body != null
            }
            if (body != null) {
                OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use {
                    it.write(body.toString())
                }
            }
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val responseBody = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }
                .orEmpty()
            HttpResponse(responseCode, responseBody)
        } finally {
            connection.disconnect()
        }
    }

    private fun HttpResponse.toAuthResponse(fallbackEmail: String?): SupabaseAuthResponse {
        val json = body.toJsonObjectOrNull()
        if (code !in 200..299) {
            return SupabaseAuthResponse(
                accessToken = null,
                userId = null,
                email = null,
                errorMessage = errorMessage(json, code),
                errorCode = authErrorCode(json)
            )
        }
        val user = json?.optJSONObject("user") ?: json
        return SupabaseAuthResponse(
            accessToken = json?.optString("access_token")?.takeIf(String::isNotBlank),
            userId = user?.optString("id")?.takeIf(String::isNotBlank),
            email = user?.optString("email")?.takeIf(String::isNotBlank) ?: fallbackEmail,
            errorMessage = null,
            refreshToken = json?.optString("refresh_token")?.takeIf(String::isNotBlank),
            expiresInSeconds = json?.optLong("expires_in") ?: 0
        )
    }

    private fun HttpResponse.toUserAuthResponse(accessToken: String): SupabaseAuthResponse {
        val json = body.toJsonObjectOrNull()
        if (code !in 200..299) {
            return SupabaseAuthResponse(
                accessToken = null,
                userId = null,
                email = null,
                errorMessage = errorMessage(json, code),
                errorCode = authErrorCode(json)
            )
        }
        return SupabaseAuthResponse(
            accessToken = accessToken,
            userId = json?.optString("id")?.takeIf(String::isNotBlank),
            email = json?.optString("email")?.takeIf(String::isNotBlank),
            errorMessage = null
        )
    }

    private fun HttpResponse.toActionResponse(): SupabaseActionResponse {
        val json = body.toJsonObjectOrNull()
        if (code in 200..299) return SupabaseActionResponse(success = true)
        return SupabaseActionResponse(
            success = false,
            errorMessage = errorMessage(json, code),
            errorCode = authErrorCode(json)
        )
    }

    private fun HttpResponse.toSyncResult(): SyncResult {
        if (code !in 200..299) {
            return SyncResult(
                success = false,
                errorMessage = errorMessage(body.toJsonObjectOrNull(), code)
            )
        }
        return SyncResult(success = true, rows = body.toJsonRows())
    }

    private fun String.toJsonObjectOrNull(): JSONObject? =
        runCatching { JSONObject(this) }.getOrNull()

    private fun String.toJsonRows(): List<JSONObject> {
        if (isBlank()) return emptyList()
        val array = runCatching { JSONArray(this) }.getOrNull()
        if (array != null) {
            return buildList {
                for (index in 0 until array.length()) {
                    array.optJSONObject(index)?.let(::add)
                }
            }
        }
        return toJsonObjectOrNull()?.let(::listOf).orEmpty()
    }

    private fun errorMessage(json: JSONObject?, code: Int): String {
        when (authErrorCode(json)) {
            "email_not_confirmed" -> return "Seu e-mail ainda não foi confirmado. Abra o e-mail recebido e toque no botão de confirmação."
            "invalid_credentials" -> return "E-mail ou senha incorretos."
            "user_already_exists" -> return "Já existe uma conta com este e-mail. Use a opção Entrar."
            "over_email_send_rate_limit" -> return "Aguarde um pouco antes de pedir outro e-mail."
            "email_address_invalid" -> return "Informe um endereço de e-mail válido."
            "weak_password" -> return "Escolha uma senha mais forte."
        }
        val rawMessage = json?.optString("message").orEmpty()
            .ifBlank { json?.optString("msg").orEmpty() }
            .ifBlank { json?.optString("error_description").orEmpty() }
            .ifBlank { json?.optString("error").orEmpty() }
            .ifBlank { "Falha na nuvem (código $code)." }
        return when {
            rawMessage.contains("email rate limit", ignoreCase = true) ->
                "Aguarde um pouco antes de pedir outro e-mail."
            rawMessage.contains("email address", ignoreCase = true) &&
                rawMessage.contains("invalid", ignoreCase = true) ->
                "Informe um endereço de e-mail válido."
            rawMessage.contains("email not confirmed", ignoreCase = true) ->
                "Seu e-mail ainda não foi confirmado. Abra o e-mail recebido e toque no botão de confirmação."
            rawMessage.contains("invalid login credentials", ignoreCase = true) ->
                "E-mail ou senha incorretos."
            else -> rawMessage
        }
    }

    private fun authErrorCode(json: JSONObject?): String? =
        json?.optString("code").orEmpty()
            .ifBlank { json?.optString("error_code").orEmpty() }
            .ifBlank { json?.optString("error").orEmpty() }
            .takeIf(String::isNotBlank)

    private fun missingConfigurationAuthResult() = SupabaseAuthResponse(
        accessToken = null,
        userId = null,
        email = null,
        errorMessage = "A conexão com a nuvem ainda não foi configurada neste aplicativo."
    )

    private fun missingConfigurationSyncResult() = SyncResult(
        success = false,
        errorMessage = "A conexão com a nuvem ainda não foi configurada."
    )

    private fun connectionMessage(error: Exception): String = when (error) {
        is UnknownHostException -> "O servidor da nuvem não foi encontrado. Verifique a configuração do Supabase."
        else -> "Não foi possível acessar a nuvem. Os dados continuam salvos no celular."
    }

    private data class HttpResponse(val code: Int, val body: String)
}

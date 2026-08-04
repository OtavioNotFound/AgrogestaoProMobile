package com.agrogestao.pro.data.remote

import com.agrogestao.pro.BuildConfig

/**
 * Configuração do Backend Supabase da AgroGestão Pro.
 *
 * As credenciais públicas são injetadas no build por variáveis de ambiente ou
 * por `local.properties`. A chave service_role nunca deve estar no aplicativo.
 */
object SupabaseConfig {
    val SUPABASE_URL: String = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
    val SUPABASE_ANON_KEY: String = BuildConfig.SUPABASE_ANON_KEY.trim()

    // Retorno da confirmação de e-mail diretamente para o aplicativo Android.
    const val AUTH_CALLBACK_URL = "com.agrogestao.pro://auth/callback"

    // Labels de status de sincronização exibidos na interface.
    const val STATUS_SYNCED_CLOUD  = "Sincronizado na Nuvem"
    const val STATUS_LOCAL_OFFLINE = "Salvo no Celular (Offline)"
    const val STATUS_SYNC_ERROR    = "Aguardando Conexão"

    val isConfigured: Boolean
        get() = SUPABASE_URL.startsWith("https://") &&
            SUPABASE_URL.endsWith(".supabase.co") &&
            SUPABASE_ANON_KEY.isNotBlank()
}

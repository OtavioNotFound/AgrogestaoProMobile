package com.agrogestao.pro

import android.os.Bundle
import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.agrogestao.pro.ui.navigation.MainAppNavigation
import com.agrogestao.pro.ui.navigation.Screen
import com.agrogestao.pro.ui.theme.AgroGestaoTheme
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import com.agrogestao.pro.data.remote.SupabaseAuthCallbackParser
import com.agrogestao.pro.data.remote.PasswordRecoverySession
import com.agrogestao.pro.data.repository.AgroRepository
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val requestedRoute = mutableStateOf<String?>(null)
    private val passwordRecoverySession = mutableStateOf<PasswordRecoverySession?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedRoute.value = intent.requestedRoute()

        val app = application as AgroGestaoApp
        val repository = app.repository
        handleAuthCallback(intent, repository)

        setContent {
            AgroGestaoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppNavigation(
                        repository = repository,
                        displayModePreferences = app.displayModePreferences,
                        taskReminderGateway = app.taskReminderService,
                        passwordRecoverySession = passwordRecoverySession.value,
                        onPasswordRecoveryConsumed = { passwordRecoverySession.value = null },
                        requestedRoute = requestedRoute.value,
                        onRequestedRouteHandled = { requestedRoute.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedRoute.value = intent.requestedRoute()
        handleAuthCallback(intent, (application as AgroGestaoApp).repository)
    }

    private fun Intent?.requestedRoute(): String? =
        Screen.Kanban.route.takeIf { this?.getBooleanExtra(EXTRA_OPEN_TASKS, false) == true }

    private fun handleAuthCallback(intent: Intent?, repository: AgroRepository) {
        val rawUrl = intent?.dataString
        val callback = SupabaseAuthCallbackParser.parse(rawUrl) ?: return
        intent?.data = null

        if (callback.errorCode != null) {
            Toast.makeText(
                this,
                "Não foi possível confirmar o e-mail. Peça um novo link no aplicativo.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        if (!callback.hasSession) {
            Toast.makeText(
                this,
                "O link de confirmação está incompleto. Peça um novo e-mail.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        if (callback.type.equals("recovery", ignoreCase = true)) {
            passwordRecoverySession.value = PasswordRecoverySession(
                accessToken = callback.accessToken.orEmpty(),
                refreshToken = callback.refreshToken.orEmpty(),
                expiresInSeconds = callback.expiresInSeconds
            )
            Toast.makeText(
                this,
                "Crie uma nova senha para recuperar sua conta.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        lifecycleScope.launch {
            repository.completeEmailConfirmation(
                accessToken = callback.accessToken.orEmpty(),
                refreshToken = callback.refreshToken.orEmpty(),
                expiresInSeconds = callback.expiresInSeconds
            ).fold(
                onSuccess = {
                    Toast.makeText(
                        this@MainActivity,
                        "E-mail confirmado. Sua conta já está conectada.",
                        Toast.LENGTH_LONG
                    ).show()
                },
                onFailure = {
                    Toast.makeText(
                        this@MainActivity,
                        it.message ?: "Não foi possível concluir a confirmação.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }

    companion object {
        const val EXTRA_OPEN_TASKS = "open_tasks"
    }
}

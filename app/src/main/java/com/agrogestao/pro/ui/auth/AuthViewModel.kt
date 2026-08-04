package com.agrogestao.pro.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.agrogestao.pro.data.repository.AgroRepository
import com.agrogestao.pro.data.repository.EmailConfirmationRequiredException
import com.agrogestao.pro.data.repository.SignUpOutcome
import com.agrogestao.pro.domain.accountPasswordError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val pendingConfirmationEmail: String? = null
)

class AuthViewModel(private val repository: AgroRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    fun createAccount(
        nome: String,
        email: String,
        password: String,
        propriedade: String,
        municipio: String,
        caf: String,
        areaText: String
    ) {
        if (nome.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState(errorMessage = "Preencha o nome, e-mail e senha.")
            return
        }
        accountPasswordError(password)?.let { message ->
            _uiState.value = AuthUiState(errorMessage = message)
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            val area = areaText.trim().replace(',', '.').toDoubleOrNull() ?: 0.0
            val result = repository.signUp(nome, email, password, propriedade, municipio, caf, area)
            result.fold(
                onSuccess = { outcome ->
                    _uiState.value = when (outcome) {
                        SignUpOutcome.SIGNED_IN -> AuthUiState(isSuccess = true)
                        SignUpOutcome.EMAIL_CONFIRMATION_REQUIRED -> AuthUiState(
                            infoMessage = "Enviamos um e-mail de confirmação. Abra a mensagem e toque no botão: o AgroGestão Pro voltará a abrir sozinho.",
                            pendingConfirmationEmail = email.trim().lowercase()
                        )
                    }
                },
                onFailure = { _uiState.value = AuthUiState(errorMessage = it.message) }
            )
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState(errorMessage = "Informe seu e-mail e senha.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            val result = repository.signIn(email, password)
            result.fold(
                onSuccess = { _uiState.value = AuthUiState(isSuccess = true) },
                onFailure = { error ->
                    _uiState.value = if (error is EmailConfirmationRequiredException) {
                        AuthUiState(
                            errorMessage = error.message,
                            pendingConfirmationEmail = error.pendingEmail
                        )
                    } else {
                        AuthUiState(errorMessage = error.message)
                    }
                }
            )
        }
    }

    fun resendConfirmation(email: String) {
        val targetEmail = email.trim().ifBlank {
            _uiState.value.pendingConfirmationEmail.orEmpty()
        }
        if (targetEmail.isBlank()) {
            _uiState.value = AuthUiState(errorMessage = "Informe o e-mail da conta.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            repository.resendSignUpConfirmation(targetEmail).fold(
                onSuccess = {
                    _uiState.value = AuthUiState(
                        infoMessage = "Novo e-mail enviado. Abra a mensagem mais recente e toque no botão de confirmação.",
                        pendingConfirmationEmail = targetEmail.lowercase()
                    )
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState(
                        errorMessage = error.message,
                        pendingConfirmationEmail = targetEmail.lowercase()
                    )
                }
            )
        }
    }

    fun requestPasswordRecovery(email: String) {
        if (email.isBlank()) {
            _uiState.value = AuthUiState(errorMessage = "Informe o e-mail da conta.")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            repository.requestPasswordRecovery(email).fold(
                onSuccess = {
                    _uiState.value = AuthUiState(
                        infoMessage = "Se existir uma conta com esse e-mail, enviaremos um link para criar uma nova senha. Confira também a pasta de spam."
                    )
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState(
                        errorMessage = error.message ?: "Não foi possível enviar o e-mail de recuperação."
                    )
                }
            )
        }
    }

    fun completePasswordRecovery(
        accessToken: String,
        refreshToken: String,
        expiresInSeconds: Long,
        newPassword: String,
        confirmation: String
    ) {
        accountPasswordError(newPassword, confirmation)?.let { message ->
            _uiState.value = AuthUiState(errorMessage = message)
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            repository.completePasswordRecovery(
                accessToken,
                refreshToken,
                expiresInSeconds,
                newPassword
            ).fold(
                onSuccess = { _uiState.value = AuthUiState(isSuccess = true) },
                onFailure = { error ->
                    _uiState.value = AuthUiState(
                        errorMessage = error.message ?: "Não foi possível criar a nova senha."
                    )
                }
            )
        }
    }

    fun preparePasswordRecovery() {
        _uiState.value = AuthUiState()
    }

    fun continueOffline(
        nome: String,
        propriedade: String,
        municipio: String,
        areaText: String
    ) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            val area = areaText.trim().replace(',', '.').toDoubleOrNull() ?: 0.0
            val result = repository.startOfflineProfile(nome, propriedade, municipio, area)
            result.fold(
                onSuccess = { _uiState.value = AuthUiState(isSuccess = true) },
                onFailure = { _uiState.value = AuthUiState(errorMessage = it.message) }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

class AuthViewModelFactory(private val repository: AgroRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

package com.agrogestao.pro.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrogestao.pro.data.remote.SupabaseConfig
import com.agrogestao.pro.data.remote.PasswordRecoverySession
import com.agrogestao.pro.ui.theme.AgroGreen050
import com.agrogestao.pro.ui.theme.AgroGreen100
import com.agrogestao.pro.ui.theme.AgroGreen900
import com.agrogestao.pro.ui.theme.CardBorder
import com.agrogestao.pro.ui.theme.PrimaryAgroGreen
import com.agrogestao.pro.ui.theme.StatusOrange
import com.agrogestao.pro.ui.theme.StatusRedSoft
import com.agrogestao.pro.ui.theme.SurfaceCard
import com.agrogestao.pro.ui.theme.SurfaceSoft
import com.agrogestao.pro.ui.theme.TextDark
import com.agrogestao.pro.ui.theme.TextMuted
import com.agrogestao.pro.ui.theme.TextSecondary

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    simpleModeChoice: Boolean = false,
    onSimpleModeChoiceChange: (Boolean) -> Unit = {},
    onDisplayModeSubmitted: (Boolean) -> Unit = {},
    onDisplayModeSubmissionFailed: () -> Unit = {},
    passwordRecoverySession: PasswordRecoverySession? = null,
    onPasswordRecoveryConsumed: () -> Unit = {},
    onAuthSuccess: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var isCadastro by remember { mutableStateOf(false) }
    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var propriedade by remember { mutableStateOf("") }
    var municipio by remember { mutableStateOf("") }
    var caf by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var showRecoveryDialog by remember { mutableStateOf(false) }
    var recoveryEmail by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var newPasswordConfirmation by remember { mutableStateOf("") }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            if (passwordRecoverySession != null) onPasswordRecoveryConsumed()
            onAuthSuccess()
        }
    }
    LaunchedEffect(passwordRecoverySession) {
        if (passwordRecoverySession != null) viewModel.preparePasswordRecovery()
    }
    LaunchedEffect(state.pendingConfirmationEmail) {
        state.pendingConfirmationEmail?.let { pendingEmail ->
            isCadastro = false
            email = pendingEmail
            password = ""
        }
    }
    LaunchedEffect(state.errorMessage, state.pendingConfirmationEmail) {
        if (state.errorMessage != null && state.pendingConfirmationEmail == null) {
            onDisplayModeSubmissionFailed()
        }
    }

    if (passwordRecoverySession != null) {
        PasswordRecoveryContent(
            state = state,
            password = newPassword,
            confirmation = newPasswordConfirmation,
            onPasswordChange = { newPassword = it },
            onConfirmationChange = { newPasswordConfirmation = it },
            onSubmit = {
                viewModel.completePasswordRecovery(
                    accessToken = passwordRecoverySession.accessToken,
                    refreshToken = passwordRecoverySession.refreshToken,
                    expiresInSeconds = passwordRecoverySession.expiresInSeconds,
                    newPassword = newPassword,
                    confirmation = newPasswordConfirmation
                )
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceCard)
            .verticalScroll(rememberScrollState())
    ) {
        AuthHero(compact = isCadastro, onBack = { isCadastro = false; viewModel.clearError() })

        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp)
        ) {
            Text(
                text = if (isCadastro) "Vamos começar" else "Bem-vindo de volta",
                color = TextDark,
                fontSize = 22.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = if (isCadastro) {
                    "Crie sua conta e organize a propriedade em poucos minutos"
                } else {
                    "Acesse sua fazenda em poucos segundos"
                },
                color = TextMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 22.dp)
            )

            SimpleModeAuthChoice(
                enabled = simpleModeChoice,
                onEnabledChange = onSimpleModeChoiceChange
            )
            Spacer(Modifier.height(18.dp))

            state.infoMessage?.let {
                AuthMessage(text = it, isError = false)
                Spacer(Modifier.height(12.dp))
            }
            state.errorMessage?.let {
                AuthMessage(text = it, isError = true)
                Spacer(Modifier.height(12.dp))
            }

            if (isCadastro) {
                AuthField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = "Nome completo",
                    placeholder = "Ex: José Carlos da Silva",
                    icon = Icons.Default.Person
                )
                AuthField(
                    value = propriedade,
                    onValueChange = { propriedade = it },
                    label = "Nome da fazenda",
                    placeholder = "Ex: Fazenda Santa Fé",
                    icon = Icons.Default.HomeWork
                )
                AuthField(
                    value = municipio,
                    onValueChange = { municipio = it },
                    label = "Município e estado",
                    placeholder = "Ex: Petrolina - PE",
                    icon = Icons.Default.LocationOn
                )
            }

            AuthField(
                value = email,
                onValueChange = { email = it },
                label = "E-mail",
                placeholder = "voce@exemplo.com",
                icon = Icons.Default.Email,
                keyboardType = KeyboardType.Email
            )
            AuthField(
                value = password,
                onValueChange = { password = it },
                label = "Senha",
                placeholder = if (isCadastro) "Mínimo de 8 caracteres" else "Digite sua senha",
                icon = Icons.Default.Lock,
                isPassword = true
            )

            if (isCadastro) {
                AuthField(
                    value = caf,
                    onValueChange = { caf = it },
                    label = "DAP ou CAF (opcional)",
                    placeholder = "Documento da agricultura familiar",
                    icon = Icons.Default.Badge
                )
                AuthField(
                    value = area,
                    onValueChange = { area = it },
                    label = "Área total da propriedade",
                    placeholder = "Em hectares",
                    icon = Icons.Default.Agriculture,
                    keyboardType = KeyboardType.Decimal
                )
                Text(
                    text = "Ao criar a conta, você concorda com os Termos de Uso e a Política de Privacidade.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(bottom = 14.dp)
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(PrimaryAgroGreen, RoundedCornerShape(5.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✓", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Text(
                            "Lembrar-me",
                            color = TextSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 7.dp)
                        )
                    }
                    Text(
                        "Esqueci a senha",
                        color = PrimaryAgroGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(enabled = !state.isLoading) {
                            recoveryEmail = email
                            showRecoveryDialog = true
                            viewModel.clearError()
                        }
                    )
                }
            }

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryAgroGreen, modifier = Modifier.size(30.dp))
                }
            } else {
                Button(
                    onClick = {
                        onDisplayModeSubmitted(simpleModeChoice)
                        if (isCadastro) {
                            viewModel.createAccount(nome, email, password, propriedade, municipio, caf, area)
                        } else {
                            viewModel.login(email, password)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAgroGreen),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                ) {
                    Text(
                        if (isCadastro) "Criar conta grátis" else "Entrar",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 8.dp).size(18.dp)
                    )
                }
            }

            if (state.pendingConfirmationEmail != null) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { viewModel.resendConfirmation(email) },
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Reenviar e-mail de confirmação", fontWeight = FontWeight.Bold)
                }
                Text(
                    "Toque no botão do e-mail para voltar automaticamente ao aplicativo.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 7.dp)
                )
            }

            if (!SupabaseConfig.isConfigured) {
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = CardBorder)
                Spacer(Modifier.height(14.dp))
                Text(
                    "A nuvem não está configurada nesta compilação. O modo local continua disponível.",
                    color = TextMuted,
                    fontSize = 11.5.sp
                )
                TextButton(
                    onClick = {
                        onDisplayModeSubmitted(simpleModeChoice)
                        viewModel.continueOffline(nome, propriedade, municipio, area)
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Testar no modo offline", color = PrimaryAgroGreen, fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    if (isCadastro) "Já tem conta? " else "Novo por aqui? ",
                    color = TextMuted,
                    fontSize = 12.5.sp
                )
                Text(
                    if (isCadastro) "Entrar" else "Criar conta",
                    color = PrimaryAgroGreen,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        isCadastro = !isCadastro
                        viewModel.clearError()
                    }
                )
            }

            Surface(
                color = AgroGreen050,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 18.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, null, tint = AgroGreen900, modifier = Modifier.size(17.dp))
                    Text(
                        "Seus dados funcionam com segurança mesmo sem internet no campo.",
                        color = AgroGreen900,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(start = 9.dp)
                    )
                }
            }
        }
    }

    if (showRecoveryDialog) {
        AlertDialog(
            onDismissRequest = { if (!state.isLoading) showRecoveryDialog = false },
            title = { Text("Recuperar acesso", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Informe o e-mail da conta. O link abrirá diretamente no AgroGestão Pro.")
                    OutlinedTextField(
                        value = recoveryEmail,
                        onValueChange = { recoveryEmail = it },
                        label = { Text("E-mail") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.requestPasswordRecovery(recoveryEmail)
                        showRecoveryDialog = false
                    },
                    enabled = !state.isLoading && recoveryEmail.isNotBlank()
                ) { Text("Enviar link") }
            },
            dismissButton = {
                TextButton(onClick = { showRecoveryDialog = false }, enabled = !state.isLoading) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun SimpleModeAuthChoice(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    Surface(
        color = if (enabled) AgroGreen050 else SurfaceSoft,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEnabledChange(!enabled) }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(AgroGreen100, RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = AgroGreen900,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 11.dp)) {
                Text(
                    "Modo simples (recomendado)",
                    color = TextDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Botões grandes, perguntas curtas e só o que você precisa agora.",
                    color = TextMuted,
                    fontSize = 10.5.sp,
                    lineHeight = 14.sp
                )
            }
            Switch(checked = enabled, onCheckedChange = null)
        }
    }
}

@Composable
private fun PasswordRecoveryContent(
    state: AuthUiState,
    password: String,
    confirmation: String,
    onPasswordChange: (String) -> Unit,
    onConfirmationChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceCard)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 40.dp)
    ) {
        Box(
            modifier = Modifier.size(58.dp).background(PrimaryAgroGreen, RoundedCornerShape(17.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(31.dp))
        }
        Text(
            "Crie uma nova senha",
            color = TextDark,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(top = 24.dp)
        )
        Text(
            "Use pelo menos 8 caracteres. Depois você entrará automaticamente na mesma conta.",
            color = TextMuted,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            modifier = Modifier.padding(top = 6.dp, bottom = 22.dp)
        )
        state.infoMessage?.let { AuthMessage(it, isError = false); Spacer(Modifier.height(12.dp)) }
        state.errorMessage?.let { AuthMessage(it, isError = true); Spacer(Modifier.height(12.dp)) }
        AuthField(
            value = password,
            onValueChange = onPasswordChange,
            label = "Nova senha",
            placeholder = "Mínimo de 8 caracteres",
            icon = Icons.Default.Lock,
            isPassword = true
        )
        AuthField(
            value = confirmation,
            onValueChange = onConfirmationChange,
            label = "Repita a nova senha",
            placeholder = "Digite a mesma senha",
            icon = Icons.Default.Lock,
            isPassword = true
        )
        Button(
            onClick = onSubmit,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAgroGreen)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Text("Salvar nova senha", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AuthHero(compact: Boolean, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 82.dp else 184.dp)
            .background(if (compact) SurfaceCard else AgroGreen050)
            .padding(horizontal = 24.dp),
        contentAlignment = if (compact) Alignment.CenterStart else Alignment.Center
    ) {
        if (compact) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.background(SurfaceSoft, RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = TextSecondary)
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(58.dp).background(PrimaryAgroGreen, RoundedCornerShape(17.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Agriculture, null, tint = Color.White, modifier = Modifier.size(31.dp))
                }
                Row(modifier = Modifier.padding(top = 13.dp)) {
                    Text(
                        "AgroGestão ",
                        color = TextDark,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text("Pro", color = PrimaryAgroGreen, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false
) {
    Text(
        label,
        color = TextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 6.dp)
    )
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = TextMuted, fontSize = 13.sp) },
        leadingIcon = { Icon(icon, null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
        modifier = Modifier.fillMaxWidth().padding(bottom = 13.dp),
        singleLine = true,
        shape = RoundedCornerShape(13.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryAgroGreen,
            unfocusedBorderColor = CardBorder,
            focusedContainerColor = SurfaceCard,
            unfocusedContainerColor = SurfaceSoft,
            cursorColor = PrimaryAgroGreen
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None
    )
}

@Composable
private fun AuthMessage(text: String, isError: Boolean) {
    Surface(
        color = if (isError) StatusRedSoft else AgroGreen100,
        shape = RoundedCornerShape(11.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            color = if (isError) StatusOrange else AgroGreen900,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(12.dp)
        )
    }
}

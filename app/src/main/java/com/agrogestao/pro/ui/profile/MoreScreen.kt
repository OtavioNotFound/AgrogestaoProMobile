package com.agrogestao.pro.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrogestao.pro.ui.components.AppScreenHeader
import com.agrogestao.pro.ui.components.MenuListItem
import com.agrogestao.pro.ui.components.PrototypeCard
import com.agrogestao.pro.ui.components.SectionLabel
import com.agrogestao.pro.ui.components.SmallIconTile
import com.agrogestao.pro.ui.theme.AgroGreen100
import com.agrogestao.pro.ui.theme.SurfaceCard
import com.agrogestao.pro.ui.theme.TextDark
import com.agrogestao.pro.ui.theme.TextMuted

@Composable
fun MoreScreen(
    simpleMode: Boolean,
    onSimpleModeChange: (Boolean) -> Unit,
    onNavigateToCosts: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToWeather: () -> Unit,
    onNavigateToConflicts: () -> Unit
) {
    var showQuickHelp by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(SurfaceCard)) {
        AppScreenHeader(title = "Mais opções")
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Text(
                "Encontre custos, seus dados e as configurações do aplicativo.",
                color = TextMuted,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            SectionLabel("Acesso rápido")
            PrototypeCard {
                MenuListItem(Icons.Default.Info, "Como usar", "Ajuda curta para atualizar o dia") {
                    showQuickHelp = true
                }
                MenuListItem(Icons.Default.Description, "Dinheiro e relatórios", "Entradas, gastos e relatório rural") { onNavigateToCosts() }
                MenuListItem(Icons.Default.Cloud, "Clima", "Previsão opcional por município") { onNavigateToWeather() }
                MenuListItem(Icons.Default.Sync, "Problemas com a nuvem", "Use se algum dado não aparecer em outro celular") { onNavigateToConflicts() }
                MenuListItem(Icons.Default.Person, "Meu perfil", "Propriedade, conta, ajuda e segurança") { onNavigateToProfile() }
                MenuListItem(Icons.Default.Backup, "Cópia de segurança", "Salvar ou restaurar seus dados") { onNavigateToBackup() }
            }

            Spacer(Modifier.height(22.dp))
            SectionLabel("Jeito de usar")
            PrototypeCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSimpleModeChange(!simpleMode) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SmallIconTile(Icons.Default.TouchApp, AgroGreen100, size = 40)
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text("Modo simples (recomendado)", color = TextDark, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            if (simpleMode) "Ativado: botões grandes e registro diário guiado" else "Ative para usar perguntas curtas e menos telas",
                            color = TextMuted,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                    Switch(checked = simpleMode, onCheckedChange = onSimpleModeChange)
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }

    if (showQuickHelp) {
        AlertDialog(
            onDismissRequest = { showQuickHelp = false },
            title = { Text("Como atualizar o dia", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column {
                    QuickHelpStep(
                        number = 1,
                        title = "Toque em Atualizar meu dia",
                        body = "O botão verde fica na tela Início."
                    )
                    QuickHelpStep(
                        number = 2,
                        title = "Responda uma pergunta por vez",
                        body = "Você pode escolher uma sugestão ou escrever com suas palavras."
                    )
                    QuickHelpStep(
                        number = 3,
                        title = "Confira e salve",
                        body = "O registro fica no celular mesmo sem internet. Depois, escolha Registrar outra ou Terminei."
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showQuickHelp = false }) { Text("Entendi") }
            }
        )
    }
}

@Composable
private fun QuickHelpStep(number: Int, title: String, body: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Text(
            "$number. $title",
            color = TextDark,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            body,
            color = TextMuted,
            fontSize = 13.5.sp,
            lineHeight = 19.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

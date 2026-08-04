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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    onNavigateToBackup: () -> Unit
) {
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
                MenuListItem(Icons.Default.Description, "Custos e relatórios", "Entradas, gastos e relatório rural") { onNavigateToCosts() }
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
                        Text("Modo simples", color = TextDark, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            if (simpleMode) "Ativado: caminhos principais e textos mais diretos" else "Ative para deixar o aplicativo mais direto",
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
}

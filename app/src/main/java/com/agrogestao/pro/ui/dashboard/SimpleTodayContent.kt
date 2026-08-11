package com.agrogestao.pro.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrogestao.pro.domain.formatDateForDisplay
import com.agrogestao.pro.domain.todayIso
import com.agrogestao.pro.ui.theme.AgroAmber100
import com.agrogestao.pro.ui.theme.AgroGreen050
import com.agrogestao.pro.ui.theme.AgroGreen100
import com.agrogestao.pro.ui.theme.CardBorder
import com.agrogestao.pro.ui.theme.PrimaryAgroGreen
import com.agrogestao.pro.ui.theme.StatusGreen
import com.agrogestao.pro.ui.theme.StatusOrange
import com.agrogestao.pro.ui.theme.SurfaceCard
import com.agrogestao.pro.ui.theme.SurfaceSoft
import com.agrogestao.pro.ui.theme.TextDark
import com.agrogestao.pro.ui.theme.TextMuted
import java.text.NumberFormat
import java.util.Locale

@Composable
internal fun SimpleTodayContent(
    state: DashboardUiState,
    hideFinancialValues: Boolean,
    onHideFinancialValuesChange: (Boolean) -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToSafras: () -> Unit,
    onNavigateToFinance: () -> Unit
) {
    val nextTask = state.tarefasPendentes.minByOrNull { it.dataLimite }
    val isLate = nextTask?.dataLimite?.let { it < todayIso() } == true

    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)) {
        Text(
            "O que precisa da sua atenção",
            color = TextDark,
            fontSize = 19.sp,
            lineHeight = 25.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(12.dp))
        if (nextTask == null) {
            Surface(
                color = AgroGreen050,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, AgroGreen100),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                    SimpleIcon(Icons.Default.CheckCircle, StatusGreen, AgroGreen100)
                    Column(modifier = Modifier.padding(start = 13.dp)) {
                        Text(
                            "Tudo em dia por aqui",
                            color = TextDark,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "Nenhuma tarefa esperando por você.",
                            color = TextMuted,
                            fontSize = 14.sp,
                            lineHeight = 19.sp
                        )
                    }
                }
            }
        } else {
            Surface(
                color = if (isLate) Color(0xFFFFE9E4) else AgroAmber100,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(
                    1.dp,
                    if (isLate) Color(0xFFE9A69A) else Color(0xFFE1BC68)
                ),
                modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToTasks)
            ) {
                Column(modifier = Modifier.padding(17.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SimpleIcon(
                            Icons.Default.Warning,
                            if (isLate) StatusOrange else Color(0xFF8A5A00),
                            if (isLate) Color(0xFFFFD9D1) else Color(0xFFF9E7BD)
                        )
                        Column(modifier = Modifier.weight(1f).padding(start = 13.dp)) {
                            Text(
                                if (isLate) "Tarefa atrasada" else "Próxima tarefa",
                                color = if (isLate) Color(0xFF7A2A20) else Color(0xFF6F4A00),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                nextTask.titulo,
                                color = TextDark,
                                fontSize = 17.sp,
                                lineHeight = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "Para ${formatDateForDisplay(nextTask.dataLimite)}",
                                color = TextMuted,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 3.dp)
                            )
                        }
                    }
                    Button(
                        onClick = onNavigateToTasks,
                        modifier = Modifier.fillMaxWidth().padding(top = 14.dp).heightIn(min = 54.dp),
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAgroGreen)
                    ) {
                        Text("Abrir minhas tarefas", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "Seu dia até agora",
            color = TextDark,
            fontSize = 19.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(12.dp))
        Surface(
            color = SurfaceSoft,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(17.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SimpleIcon(Icons.Default.CheckCircle, StatusGreen, AgroGreen100)
                    Column(modifier = Modifier.weight(1f).padding(start = 13.dp)) {
                        Text(
                            "${state.atividadesConcluidasHoje} ${if (state.atividadesConcluidasHoje == 1) "atividade registrada" else "atividades registradas"}",
                            color = TextDark,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            if (state.atividadesConcluidasHoje == 0) {
                                "Use o botão verde acima para guardar o primeiro registro."
                            } else {
                                "Esses registros já estão no histórico do seu trabalho."
                            },
                            color = TextMuted,
                            fontSize = 13.5.sp,
                            lineHeight = 19.sp
                        )
                    }
                }
                Spacer(Modifier.height(15.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SimpleIcon(Icons.Default.Payments, PrimaryAgroGreen, AgroGreen100)
                    Column(modifier = Modifier.weight(1f).padding(start = 13.dp)) {
                        Text("Dinheiro de hoje", color = TextDark, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            if (hideFinancialValues) {
                                "Valores escondidos"
                            } else {
                                "Entrou ${simpleCurrency(state.entradasHoje)} • Saiu ${simpleCurrency(state.saidasHoje)}"
                            },
                            color = TextMuted,
                            fontSize = 13.5.sp,
                            lineHeight = 19.sp
                        )
                    }
                    IconButton(
                        onClick = { onHideFinancialValuesChange(!hideFinancialValues) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            if (hideFinancialValues) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            if (hideFinancialValues) "Mostrar valores" else "Esconder valores",
                            tint = PrimaryAgroGreen
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "Para onde você quer ir?",
            color = TextDark,
            fontSize = 19.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            "Os detalhes continuam disponíveis quando você precisar.",
            color = TextMuted,
            fontSize = 13.5.sp,
            lineHeight = 19.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )
        SimpleDestinationButton(
            icon = Icons.AutoMirrored.Filled.Assignment,
            title = "Minhas tarefas",
            subtitle = "Ver o que fazer e marcar o que terminou",
            onClick = onNavigateToTasks
        )
        Spacer(Modifier.height(10.dp))
        SimpleDestinationButton(
            icon = Icons.Default.Agriculture,
            title = "Meus terrenos",
            subtitle = "Acompanhar plantio, manejo e colheita",
            onClick = onNavigateToSafras
        )
        Spacer(Modifier.height(10.dp))
        SimpleDestinationButton(
            icon = Icons.Default.Payments,
            title = "Meu dinheiro",
            subtitle = "Conferir entradas, compras e pagamentos",
            onClick = onNavigateToFinance
        )
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun SimpleDestinationButton(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 68.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CardBorder),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfaceCard)
    ) {
        Icon(icon, null, tint = PrimaryAgroGreen, modifier = Modifier.size(25.dp))
        Column(modifier = Modifier.weight(1f).padding(start = 13.dp), horizontalAlignment = Alignment.Start) {
            Text(title, color = TextDark, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = TextMuted, fontSize = 12.5.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun SimpleIcon(icon: ImageVector, iconColor: Color, background: Color) {
    Box(
        modifier = Modifier.size(44.dp).background(background, RoundedCornerShape(13.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = iconColor, modifier = Modifier.size(24.dp))
    }
}

private fun simpleCurrency(value: Double): String =
    NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)

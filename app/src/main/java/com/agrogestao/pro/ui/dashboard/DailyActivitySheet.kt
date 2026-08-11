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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrogestao.pro.data.local.entities.CropEntity
import com.agrogestao.pro.domain.DailyActivityRequest
import com.agrogestao.pro.domain.DailyActivityType
import com.agrogestao.pro.domain.dailyActivitySubjectError
import com.agrogestao.pro.domain.dailyActivityValidationError
import com.agrogestao.pro.domain.formatDateForDisplay
import com.agrogestao.pro.domain.parseMoneyTextToCents
import com.agrogestao.pro.domain.todayIso
import com.agrogestao.pro.ui.theme.AgroGreen050
import com.agrogestao.pro.ui.theme.AgroGreen100
import com.agrogestao.pro.ui.theme.CardBorder
import com.agrogestao.pro.ui.theme.PrimaryAgroGreen
import com.agrogestao.pro.ui.theme.StatusOrange
import com.agrogestao.pro.ui.theme.SurfaceCard
import com.agrogestao.pro.ui.theme.SurfaceSoft
import com.agrogestao.pro.ui.theme.TextDark
import com.agrogestao.pro.ui.theme.TextMuted
import com.agrogestao.pro.ui.theme.TextSecondary
import java.text.NumberFormat
import java.util.Locale

private enum class DailyActivityStep { TYPE, SUBJECT, AMOUNT, CONFIRM }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyActivitySheet(
    crops: List<CropEntity>,
    recentSuggestions: Map<DailyActivityType, List<String>> = emptyMap(),
    isSaving: Boolean,
    externalError: String?,
    onDismiss: () -> Unit,
    onSave: (DailyActivityRequest) -> Unit
) {
    var step by remember { mutableStateOf(DailyActivityStep.TYPE) }
    var selectedType by remember { mutableStateOf<DailyActivityType?>(null) }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedCropCloudId by remember { mutableStateOf<String?>(null) }
    var placeChosen by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    val selectedCrop = crops.firstOrNull { it.cloudId == selectedCropCloudId }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onDismiss() },
        sheetState = sheetState,
        containerColor = SurfaceCard,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .navigationBarsPadding()
                .padding(bottom = 26.dp)
        ) {
            DailyActivityHeader(
                step = step,
                selectedType = selectedType,
                onBack = {
                    localError = null
                    step = when (step) {
                        DailyActivityStep.TYPE -> DailyActivityStep.TYPE
                        DailyActivityStep.SUBJECT -> DailyActivityStep.TYPE
                        DailyActivityStep.AMOUNT -> DailyActivityStep.SUBJECT
                        DailyActivityStep.CONFIRM -> if (selectedType?.requiresAmount == true) {
                            DailyActivityStep.AMOUNT
                        } else {
                            DailyActivityStep.SUBJECT
                        }
                    }
                },
                onCancel = onDismiss,
                isSaving = isSaving
            )

            when (step) {
                DailyActivityStep.TYPE -> {
                    Text(
                        "O que aconteceu hoje?",
                        color = TextDark,
                        fontSize = 24.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(top = 18.dp, bottom = 6.dp)
                    )
                    Text(
                        "Toque em uma opção. Você não precisa preencher um formulário grande.",
                        color = TextMuted,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        modifier = Modifier.padding(bottom = 18.dp)
                    )
                    DailyActivityType.entries.forEach { type ->
                        ActivityTypeButton(type = type) {
                            selectedType = type
                            localError = null
                            val areaMatters = activityUsesArea(type)
                            placeChosen = !areaMatters || crops.isEmpty()
                            step = DailyActivityStep.SUBJECT
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }

                DailyActivityStep.SUBJECT -> {
                    val type = requireNotNull(selectedType)
                    val suggestions = activitySuggestions(
                        type = type,
                        crops = crops,
                        recent = recentSuggestions[type].orEmpty()
                    )
                    Text(
                        activityQuestion(type),
                        color = TextDark,
                        fontSize = 24.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(top = 18.dp)
                    )
                    Text(
                        if (suggestions.isEmpty()) {
                            "Escreva com suas palavras. Uma frase curta já basta."
                        } else {
                            "Toque em uma sugestão ou escreva outra opção."
                        },
                        color = TextMuted,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        modifier = Modifier.padding(top = 7.dp, bottom = 16.dp)
                    )
                    if (suggestions.isNotEmpty()) {
                        SuggestionOptions(
                            options = suggestions,
                            selected = note,
                            onSelected = {
                                note = it
                                localError = null
                            }
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                    val usesLongText = type == DailyActivityType.FOUND_PROBLEM ||
                        type == DailyActivityType.OTHER
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it.take(140); localError = null },
                        label = { Text(activityInputLabel(type)) },
                        placeholder = { Text(activityPlaceholder(type)) },
                        singleLine = !usesLongText,
                        minLines = if (usesLongText) 3 else 1,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                        shape = RoundedCornerShape(14.dp)
                    )
                    localError?.let { ActivityError(it) }
                    Button(
                        onClick = {
                            localError = note.trim().takeIf { it.isBlank() }
                                ?.let { dailyActivitySubjectError(type) }
                            if (localError == null) {
                                step = if (type.requiresAmount) {
                                    DailyActivityStep.AMOUNT
                                } else {
                                    DailyActivityStep.CONFIRM
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 22.dp).heightIn(min = 58.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAgroGreen)
                    ) {
                        Text("Continuar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                DailyActivityStep.AMOUNT -> {
                    val type = requireNotNull(selectedType)
                    Text(
                        "Qual foi o valor?",
                        color = TextDark,
                        fontSize = 24.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(top = 18.dp)
                    )
                    Text(
                        "Digite só o valor de ${note.trim()}. A data de hoje já será colocada.",
                        color = TextMuted,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        modifier = Modifier.padding(top = 7.dp, bottom = 20.dp)
                    )
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = {
                            amountText = it.filter { char -> char.isDigit() || char == ',' || char == '.' }
                            localError = null
                        },
                        label = { Text("Valor em reais") },
                        placeholder = { Text("Ex: 250,00") },
                        leadingIcon = { Text("R$", fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                        shape = RoundedCornerShape(14.dp)
                    )
                    localError?.let { ActivityError(it) }
                    Button(
                        onClick = {
                            val request = DailyActivityRequest(
                                type = type,
                                dateIso = todayIso(),
                                amountCents = parseMoneyTextToCents(amountText),
                                note = note
                            )
                            localError = dailyActivityValidationError(request)
                            if (localError == null) step = DailyActivityStep.CONFIRM
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 22.dp).heightIn(min = 58.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAgroGreen)
                    ) {
                        Text("Continuar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                DailyActivityStep.CONFIRM -> {
                    val type = requireNotNull(selectedType)
                    val areaMatters = activityUsesArea(type)
                    Text(
                        if (areaMatters) "Em qual terreno?" else "Confira antes de salvar",
                        color = TextDark,
                        fontSize = 24.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(top = 18.dp)
                    )
                    Text(
                        if (areaMatters) {
                            "Escolha o terreno ou use Geral da propriedade."
                        } else {
                            "Veja se a atividade e o valor estão certos."
                        },
                        color = TextMuted,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        modifier = Modifier.padding(top = 7.dp, bottom = 18.dp)
                    )
                    if (areaMatters) {
                        PlaceButton(
                            title = "Geral da propriedade",
                            subtitle = "Quando não pertence a um terreno específico",
                            selected = placeChosen && selectedCropCloudId == null,
                            onClick = {
                                selectedCropCloudId = null
                                placeChosen = true
                                localError = null
                            }
                        )
                        crops.forEach { crop ->
                            Spacer(Modifier.height(10.dp))
                            PlaceButton(
                                title = crop.nomeCultura,
                                subtitle = "${crop.areaHectares} ha • ${crop.statusManejo.ifBlank { "Em acompanhamento" }}",
                                selected = selectedCropCloudId == crop.cloudId,
                                onClick = {
                                    selectedCropCloudId = crop.cloudId
                                    placeChosen = true
                                    localError = null
                                }
                            )
                        }
                    }
                    if (!placeChosen) {
                        Text(
                            "Toque no terreno onde isso aconteceu.",
                            color = StatusOrange,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                    localError?.let { ActivityError(it) }
                    externalError?.let { ActivityError(it) }
                    Surface(
                        color = AgroGreen050,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 18.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = PrimaryAgroGreen)
                            Column(modifier = Modifier.padding(start = 11.dp)) {
                                Text(
                                    "${type.userLabel}: ${note.trim()}",
                                    color = TextDark,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (type.requiresAmount) {
                                    Text(
                                        formatActivityAmount(parseMoneyTextToCents(amountText)),
                                        color = TextDark,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    "${selectedCrop?.nomeCultura ?: "Geral da propriedade"} • ${formatDateForDisplay(todayIso())}",
                                    color = TextMuted,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                    Button(
                        onClick = {
                            if (!placeChosen) return@Button
                            val request = DailyActivityRequest(
                                type = type,
                                dateIso = todayIso(),
                                amountCents = if (type.requiresAmount) parseMoneyTextToCents(amountText) else null,
                                note = note,
                                cropCloudId = selectedCropCloudId
                            )
                            localError = dailyActivityValidationError(request)
                            if (localError == null) onSave(request)
                        },
                        enabled = placeChosen && !isSaving,
                        modifier = Modifier.fillMaxWidth().padding(top = 18.dp).heightIn(min = 62.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAgroGreen)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                            Text("Salvando no celular...", modifier = Modifier.padding(start = 10.dp))
                        } else {
                            Text("Salvar agora", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    Text(
                        "Funciona sem internet. A nuvem será atualizada quando houver conexão.",
                        color = TextMuted,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyActivityHeader(
    step: DailyActivityStep,
    selectedType: DailyActivityType?,
    onBack: () -> Unit,
    onCancel: () -> Unit,
    isSaving: Boolean
) {
    val totalSteps = if (selectedType?.requiresAmount == true) 4 else 3
    val currentStep = when (step) {
        DailyActivityStep.TYPE -> 1
        DailyActivityStep.SUBJECT -> 2
        DailyActivityStep.AMOUNT -> 3
        DailyActivityStep.CONFIRM -> totalSteps
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        if (step != DailyActivityStep.TYPE) {
            IconButton(onClick = onBack, enabled = !isSaving, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar para a pergunta anterior")
            }
        } else {
            Spacer(Modifier.size(48.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
            Text(
                "Atualizar meu dia",
                color = TextDark,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                if (selectedType == null) {
                    "Começando • cerca de 30 segundos"
                } else {
                    "Passo $currentStep de $totalSteps • cerca de 30 segundos"
                },
                color = TextMuted,
                fontSize = 12.5.sp
            )
        }
        TextButton(onClick = onCancel, enabled = !isSaving) { Text("Fechar") }
    }
    LinearProgressIndicator(
        progress = {
            if (selectedType == null) 0.25f else currentStep.toFloat() / totalSteps.toFloat()
        },
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(6.dp),
        color = PrimaryAgroGreen,
        trackColor = AgroGreen100
    )
}

@Composable
private fun SuggestionOptions(
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Column {
        options.chunked(2).forEachIndexed { index, rowOptions ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowOptions.forEachIndexed { optionIndex, option ->
                    val isSelected = option.equals(selected.trim(), ignoreCase = true)
                    OutlinedButton(
                        onClick = { onSelected(option) },
                        modifier = Modifier.weight(1f).heightIn(min = 54.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) PrimaryAgroGreen else CardBorder
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) AgroGreen050 else SurfaceCard,
                            contentColor = TextDark
                        )
                    ) {
                        Text(
                            option,
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold
                        )
                    }
                    if (optionIndex == 0) Spacer(Modifier.size(10.dp))
                }
                if (rowOptions.size == 1) Spacer(Modifier.weight(1f).padding(start = 10.dp))
            }
            if (index < options.chunked(2).lastIndex) Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ActivityTypeButton(type: DailyActivityType, onClick: () -> Unit) {
    val icon = activityIcon(type)
    Surface(
        color = SurfaceSoft,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .semantics { contentDescription = "${type.userLabel}. ${activityHint(type)}" }
            .clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).background(AgroGreen100, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = PrimaryAgroGreen, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.padding(start = 13.dp)) {
                Text(type.userLabel, color = TextDark, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                Text(activityHint(type), color = TextMuted, fontSize = 13.sp, lineHeight = 17.sp)
            }
        }
    }
}

@Composable
private fun PlaceButton(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) AgroGreen050 else SurfaceCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) PrimaryAgroGreen else CardBorder),
        modifier = Modifier.fillMaxWidth().heightIn(min = 68.dp).clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Place, null, tint = if (selected) PrimaryAgroGreen else TextSecondary)
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(title, color = TextDark, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Text(subtitle, color = TextMuted, fontSize = 12.5.sp, lineHeight = 17.sp)
            }
            if (selected) Icon(Icons.Default.CheckCircle, "Selecionado", tint = PrimaryAgroGreen)
        }
    }
}

@Composable
private fun ActivityError(message: String) {
    Text(
        message,
        color = StatusOrange,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 12.dp)
    )
}

private fun activityHint(type: DailyActivityType): String = when (type) {
    DailyActivityType.PLANTED -> "Guardar o plantio no histórico do terreno"
    DailyActivityType.HARVESTED -> "Marcar a colheita e concluir o ciclo"
    DailyActivityType.BOUGHT -> "Registrar uma compra no dinheiro que saiu"
    DailyActivityType.SOLD -> "Registrar uma venda no dinheiro que entrou"
    DailyActivityType.PAID -> "Guardar uma conta ou pagamento feito"
    DailyActivityType.RECEIVED -> "Guardar um valor que você recebeu"
    DailyActivityType.USED_INPUT -> "Anotar adubo, semente ou outro insumo"
    DailyActivityType.FOUND_PROBLEM -> "Criar um lembrete para não esquecer"
    DailyActivityType.OTHER -> "Registrar outro trabalho do dia"
}

private fun activityQuestion(type: DailyActivityType): String = when (type) {
    DailyActivityType.PLANTED -> "O que você plantou?"
    DailyActivityType.HARVESTED -> "O que você colheu?"
    DailyActivityType.BOUGHT -> "O que você comprou?"
    DailyActivityType.SOLD -> "O que você vendeu?"
    DailyActivityType.PAID -> "O que você pagou?"
    DailyActivityType.RECEIVED -> "De quem ou pelo que recebeu?"
    DailyActivityType.USED_INPUT -> "Qual insumo você usou?"
    DailyActivityType.FOUND_PROBLEM -> "Qual problema você encontrou?"
    DailyActivityType.OTHER -> "O que você fez?"
}

private fun activityInputLabel(type: DailyActivityType): String = when (type) {
    DailyActivityType.PLANTED -> "Plantação"
    DailyActivityType.HARVESTED -> "Colheita"
    DailyActivityType.BOUGHT -> "Compra"
    DailyActivityType.SOLD -> "Venda"
    DailyActivityType.PAID -> "Conta ou pagamento"
    DailyActivityType.RECEIVED -> "Origem do dinheiro"
    DailyActivityType.USED_INPUT -> "Insumo"
    DailyActivityType.FOUND_PROBLEM -> "Problema encontrado"
    DailyActivityType.OTHER -> "Atividade realizada"
}

private fun activityPlaceholder(type: DailyActivityType): String = when (type) {
    DailyActivityType.PLANTED -> "Ex: Feijão"
    DailyActivityType.HARVESTED -> "Ex: Milho"
    DailyActivityType.BOUGHT -> "Ex: Sementes"
    DailyActivityType.SOLD -> "Ex: 10 sacas de feijão"
    DailyActivityType.PAID -> "Ex: Conta de luz"
    DailyActivityType.RECEIVED -> "Ex: Pagamento da cooperativa"
    DailyActivityType.USED_INPUT -> "Ex: Adubo"
    DailyActivityType.FOUND_PROBLEM -> "Ex: Cerca quebrada perto do milho"
    DailyActivityType.OTHER -> "Ex: Limpei a entrada da propriedade"
}

private fun activitySuggestions(
    type: DailyActivityType,
    crops: List<CropEntity>,
    recent: List<String>
): List<String> {
    val cropNames = crops.map { it.nomeCultura.trim() }.filter { it.isNotBlank() }
    val options = when (type) {
        DailyActivityType.PLANTED,
        DailyActivityType.HARVESTED -> cropNames + listOf("Feijão", "Milho", "Hortaliças", "Mandioca")
        DailyActivityType.BOUGHT -> listOf("Sementes", "Adubo", "Combustível", "Ração")
        DailyActivityType.SOLD -> cropNames + listOf("Feijão", "Milho", "Leite", "Animais")
        DailyActivityType.PAID -> listOf("Conta de luz", "Funcionário", "Frete", "Manutenção")
        DailyActivityType.RECEIVED ->
            listOf("Cliente", "Cooperativa", "Venda anterior", "Benefício ou ajuda")
        DailyActivityType.USED_INPUT -> listOf("Adubo", "Sementes", "Defensivo", "Ração")
        DailyActivityType.FOUND_PROBLEM ->
            listOf("Praga", "Doença", "Falta de água", "Cerca quebrada")
        DailyActivityType.OTHER -> listOf("Manutenção", "Limpeza", "Transporte", "Visita técnica")
    }
    return (recent + options).distinctBy { it.lowercase() }.take(4)
}

private fun activityUsesArea(type: DailyActivityType): Boolean =
    type == DailyActivityType.PLANTED ||
        type == DailyActivityType.HARVESTED ||
        type == DailyActivityType.USED_INPUT ||
        type == DailyActivityType.FOUND_PROBLEM

private fun formatActivityAmount(amountCents: Long?): String =
    NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        .format((amountCents ?: 0L).toDouble() / 100.0)

private fun activityIcon(type: DailyActivityType): ImageVector = when (type) {
    DailyActivityType.PLANTED -> Icons.Default.Agriculture
    DailyActivityType.HARVESTED -> Icons.Default.CheckCircle
    DailyActivityType.BOUGHT -> Icons.Default.ShoppingCart
    DailyActivityType.SOLD -> Icons.Default.Sell
    DailyActivityType.PAID -> Icons.Default.Payments
    DailyActivityType.RECEIVED -> Icons.Default.AccountBalanceWallet
    DailyActivityType.USED_INPUT -> Icons.Default.Inventory2
    DailyActivityType.FOUND_PROBLEM -> Icons.Default.ReportProblem
    DailyActivityType.OTHER -> Icons.Default.EditNote
}

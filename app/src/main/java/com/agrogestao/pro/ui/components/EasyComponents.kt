package com.agrogestao.pro.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrogestao.pro.data.remote.SupabaseConfig
import com.agrogestao.pro.data.local.entities.CropEntity
import com.agrogestao.pro.domain.formatDateForDisplay
import com.agrogestao.pro.domain.isoDateParts
import com.agrogestao.pro.domain.toIsoDate
import java.util.Calendar
import com.agrogestao.pro.ui.theme.CardBorder
import com.agrogestao.pro.ui.theme.PrimaryAgroGreen
import com.agrogestao.pro.ui.theme.StatusGreen
import com.agrogestao.pro.ui.theme.StatusOrange
import com.agrogestao.pro.ui.theme.AccentEarthOrange
import com.agrogestao.pro.ui.theme.AgroAmber100
import com.agrogestao.pro.ui.theme.SurfaceCard
import com.agrogestao.pro.ui.theme.TextDark

@Composable
fun EasyBigButton(
    text: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = PrimaryAgroGreen,
    contentColor: Color = SurfaceCard
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 9.dp)
                )
            }
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

@Composable
fun EasyCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = SurfaceCard,
    borderColor: Color = CardBorder,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        content = content
    )
}

@Composable
fun SimpleStatusBadge(
    text: String,
    backgroundColor: Color,
    textColor: Color = Color.White
) {
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ConfirmDeleteDialog(
    itemLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirmar exclusão", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "Deseja excluir $itemLabel? A remoção será sincronizada com a nuvem quando houver conexão."
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
            colors = ButtonDefaults.buttonColors(
                containerColor = StatusOrange,
                contentColor = SurfaceCard
            )
            ) {
                Text("Excluir")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun DateSelectionButton(
    label: String,
    isoDate: String,
    onDateSelected: (String) -> Unit
) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = {
            val fallback = Calendar.getInstance()
            val parts = isoDateParts(isoDate)
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    onDateSelected(toIsoDate(year, month, day))
                },
                parts?.first ?: fallback.get(Calendar.YEAR),
                parts?.second ?: fallback.get(Calendar.MONTH),
                parts?.third ?: fallback.get(Calendar.DAY_OF_MONTH)
            ).show()
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CalendarMonth,
            contentDescription = null,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text("$label: ${formatDateForDisplay(isoDate)}")
    }
}

@Composable
fun CropSelectionField(
    crops: List<CropEntity>,
    selectedCropCloudId: String?,
    onCropSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = crops.firstOrNull { it.cloudId == selectedCropCloudId }
        ?.nomeCultura
        ?: "Geral (sem safra específica)"

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Agriculture,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Safra: $selectedName")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            DropdownMenuItem(
                text = { Text("Geral (sem safra específica)") },
                onClick = {
                    onCropSelected(null)
                    expanded = false
                }
            )
            crops.forEach { crop ->
                DropdownMenuItem(
                    text = { Text(crop.nomeCultura) },
                    onClick = {
                        onCropSelected(crop.cloudId)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun CloudSyncStatusBadge(
    syncStatus: String
) {
    val isSynced = syncStatus == SupabaseConfig.STATUS_SYNCED_CLOUD
    val isWaiting = syncStatus == SupabaseConfig.STATUS_SYNC_ERROR
    val badgeColor = when {
        isSynced -> StatusGreen
        isWaiting -> StatusOrange
        else -> AccentEarthOrange
    }
    val badgeText = when {
        isSynced -> "Nuvem"
        isWaiting -> "Pendente"
        else -> "Offline"
    }
    Surface(
        color = if (isSynced) StatusGreen.copy(alpha = 0.12f) else AgroAmber100,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSynced) Icons.Default.CloudDone else Icons.Default.CloudOff,
                contentDescription = null,
                tint = badgeColor,
                modifier = Modifier
                    .height(14.dp)
                    .padding(end = 4.dp)
            )
            Text(
                text = badgeText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = badgeColor
            )
        }
    }
}

@Composable
fun TopFarmHeader(
    producerName: String,
    farmName: String,
    location: String,
    onLogout: (() -> Unit)? = null
) {
    Surface(
        color = PrimaryAgroGreen,
        contentColor = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AgroGestão Pro • Supabase Cloud",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = farmName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "$producerName • $location",
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.95f)
                    )
                }
                if (onLogout != null) {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sair da conta",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

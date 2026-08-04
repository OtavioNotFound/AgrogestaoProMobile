package com.agrogestao.pro.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agrogestao.pro.domain.formatDateForDisplay
import com.agrogestao.pro.domain.isoDateParts
import com.agrogestao.pro.domain.toIsoDate
import java.util.Calendar

data class FilterChoice(
    val value: String?,
    val label: String
)

@Composable
fun FiltersButton(
    activeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.FilterList,
            contentDescription = null,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = if (activeCount == 0) "Filtrar resultados" else "Filtros ativos: $activeCount",
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun FilterSelectionField(
    label: String,
    selectedLabel: String,
    choices: List<FilterChoice>,
    onSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "$label: $selectedLabel",
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.ExpandMore, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            choices.forEach { choice ->
                DropdownMenuItem(
                    text = { Text(choice.label) },
                    onClick = {
                        onSelected(choice.value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun OptionalDateFilterField(
    label: String,
    isoDate: String?,
    onDateSelected: (String) -> Unit,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = {
                val fallback = Calendar.getInstance()
                val parts = isoDateParts(isoDate.orEmpty())
                DatePickerDialog(
                    context,
                    { _, year, month, day -> onDateSelected(toIsoDate(year, month, day)) },
                    parts?.first ?: fallback.get(Calendar.YEAR),
                    parts?.second ?: fallback.get(Calendar.MONTH),
                    parts?.third ?: fallback.get(Calendar.DAY_OF_MONTH)
                ).show()
            },
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = if (isoDate == null) "$label: qualquer data" else
                    "$label: ${formatDateForDisplay(isoDate)}"
            )
        }
        if (isoDate != null) {
            TextButton(
                onClick = onClear,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Limpar")
            }
        }
    }
}

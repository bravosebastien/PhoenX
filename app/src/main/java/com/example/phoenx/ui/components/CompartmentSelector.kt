package com.example.phoenx.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.phoenx.domain.model.CompartmentIds

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompartmentSelector(
    selectedIds: List<String>,
    onToggleCompartment: (String) -> Unit,
    accent: Color,
    enabled: Boolean = true
) {
    // v9.4.27 : Liste restreinte aux tiroirs ayant un sens en sélection manuelle
    val manualTiroirs = listOf(
        CompartmentIds.PHOTOS,
        CompartmentIds.LIBRARY_VIDEO,
        CompartmentIds.LIBRARY_MUSIC
    )

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        manualTiroirs.forEach { id ->
            val isSelected = selectedIds.contains(id)
            val label = CompartmentIds.getLabel(id)
            
            FilterChip(
                selected = isSelected,
                onClick = { if (enabled) onToggleCompartment(id) },
                label = { Text(label) },
                enabled = enabled,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accent,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

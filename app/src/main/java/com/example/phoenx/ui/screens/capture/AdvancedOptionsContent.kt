package com.example.phoenx.ui.screens.capture

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.ui.components.InfoPoint
import com.example.phoenx.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedOptionsContent(
    enigmaQuestion: String,
    onEnigmaQuestionChange: (String) -> Unit,
    enigmaAnswer: String,
    onEnigmaAnswerChange: (String) -> Unit,
    enigmaHint: String,
    onEnigmaHintChange: (String) -> Unit,
    enigmaAutoUnlockDays: Int?,
    onEnigmaAutoUnlockDaysChange: (Int?) -> Unit,
    scheduledTimestamp: Long?,
    onScheduledTimestampChange: (Long?) -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    
    val datePickerColors = DatePickerDefaults.colors(
        containerColor = theme.backgroundColor,
        titleContentColor = theme.contentColor,
        headlineContentColor = theme.contentColor,
        weekdayContentColor = theme.contentColor.copy(alpha = 0.4f),
        subheadContentColor = theme.contentColor.copy(alpha = 0.4f),
        yearContentColor = theme.contentColor,
        currentYearContentColor = accent,
        selectedYearContentColor = theme.backgroundColor,
        selectedYearContainerColor = accent,
        dayContentColor = theme.contentColor,
        disabledDayContentColor = theme.contentColor.copy(alpha = 0.1f),
        selectedDayContentColor = theme.backgroundColor,
        selectedDayContainerColor = accent,
        todayContentColor = accent,
        todayDateBorderColor = accent
    )

    Column(modifier = Modifier.padding(24.dp).fillMaxWidth().padding(bottom = 32.dp)) {
        Text("OPTIONS AVANCÉES", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = theme.contentColor.copy(alpha = 0.4f), letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(32.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Fingerprint, null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("Mode Détective", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = theme.contentColor)
            Spacer(modifier = Modifier.weight(1f))
            InfoPoint(
                title = "Le Jeu de Piste",
                content = "Transformez la lecture de vos souvenirs en une quête. Vos proches devront répondre à cette question personnelle pour déverrouiller ce fragment."
            )
        }
        Text(
            "Verrouille ce souvenir derrière une énigme personnelle.",
            style = MaterialTheme.typography.bodySmall,
            color = theme.contentColor.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 32.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = enigmaQuestion,
            onValueChange = onEnigmaQuestionChange,
            label = { Text("Ta question secrète") },
            modifier = Modifier.fillMaxWidth().padding(start = 32.dp),
            placeholder = { Text("Ex: Quel était le nom de notre premier chien ?") },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, unfocusedBorderColor = theme.contentColor.copy(alpha = 0.2f), focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = enigmaAnswer,
            onValueChange = onEnigmaAnswerChange,
            label = { Text("La réponse attendue") },
            modifier = Modifier.fillMaxWidth().padding(start = 32.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, unfocusedBorderColor = theme.contentColor.copy(alpha = 0.2f), focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = enigmaHint,
            onValueChange = onEnigmaHintChange,
            label = { Text("Indice (optionnel, après 3 échecs)") },
            modifier = Modifier.fillMaxWidth().padding(start = 32.dp),
            placeholder = { Text("Ex: C'est un animal à poils...") },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, unfocusedBorderColor = theme.contentColor.copy(alpha = 0.2f), focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        // AUTO UNLOCK DAYS
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Auto-déblocage", style = MaterialTheme.typography.bodyMedium, color = theme.contentColor)
            Switch(
                checked = enigmaAutoUnlockDays != null,
                onCheckedChange = { 
                    if (it) onEnigmaAutoUnlockDaysChange(30)
                    else onEnigmaAutoUnlockDaysChange(null)
                },
                colors = SwitchDefaults.colors(checkedThumbColor = accent)
            )
        }
        
        if (enigmaAutoUnlockDays != null) {
            val delayOptions = listOf(7, 14, 30, 60, 90, 180)
            var sliderPos by remember { mutableFloatStateOf(delayOptions.indexOf(enigmaAutoUnlockDays).coerceAtLeast(0).toFloat()) }
            
            Slider(
                value = sliderPos,
                onValueChange = { 
                    sliderPos = it
                    onEnigmaAutoUnlockDaysChange(delayOptions[it.toInt()])
                },
                valueRange = 0f..(delayOptions.size - 1).toFloat(),
                steps = delayOptions.size - 2,
                modifier = Modifier.padding(start = 32.dp),
                colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
            )
            Text(
                "Ouvrir après $enigmaAutoUnlockDays jours", 
                style = MaterialTheme.typography.labelSmall, 
                color = accent,
                modifier = Modifier.padding(start = 32.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Event, null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("Ouverture Programmée", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = theme.contentColor)
        }
        Text(
            "Ce souvenir ne sera visible qu'à partir d'une date précise.",
            style = MaterialTheme.typography.bodySmall,
            color = theme.contentColor.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 32.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        val dateText = scheduledTimestamp?.let {
            DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH)
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(it))
        } ?: "Choisir une date"
        
        var showDatePicker by remember { mutableStateOf(false) }
        val datePickerState = rememberDatePickerState()

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp)
                .clickable { showDatePicker = true }
                .border(1.dp, theme.contentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
            color = theme.contentColor.copy(alpha = 0.03f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, null, tint = accent, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(dateText, color = theme.contentColor, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        onScheduledTimestampChange(datePickerState.selectedDateMillis)
                        showDatePicker = false
                    }) { Text("Confirmer", color = accent) }
                },
                colors = datePickerColors
            ) {
                DatePicker(state = datePickerState, colors = datePickerColors)
            }
        }
    }
}

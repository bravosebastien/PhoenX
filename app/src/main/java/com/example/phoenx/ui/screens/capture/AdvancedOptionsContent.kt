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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.R
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
    onScheduledTimestampChange: (Long?) -> Unit,
    includeInBook: Boolean,
    onIncludeInBookChange: (Boolean) -> Unit,
    soulTone: String?,
    onSoulToneChange: (String?) -> Unit,
    type: String = "TEXT" // v12.2
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
        Text(stringResource(R.string.capture_advanced_options), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = theme.contentColor.copy(alpha = 0.4f), letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(32.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Fingerprint, null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(stringResource(R.string.capture_advanced_vault_title), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = theme.contentColor)
            Spacer(modifier = Modifier.weight(1f))
            InfoPoint(
                title = stringResource(R.string.capture_advanced_vault_info_title),
                content = stringResource(R.string.capture_advanced_vault_info_content)
            )
        }
        Text(
            stringResource(R.string.capture_advanced_vault_desc),
            style = MaterialTheme.typography.bodySmall,
            color = theme.contentColor.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 32.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = enigmaQuestion,
            onValueChange = onEnigmaQuestionChange,
            label = { Text(stringResource(R.string.capture_enigma_label_question)) },
            modifier = Modifier.fillMaxWidth().padding(start = 32.dp),
            placeholder = { Text(stringResource(R.string.capture_enigma_placeholder_question_dog)) },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, unfocusedBorderColor = theme.contentColor.copy(alpha = 0.2f), focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = enigmaAnswer,
            onValueChange = onEnigmaAnswerChange,
            label = { Text(stringResource(R.string.capture_enigma_label_answer)) },
            modifier = Modifier.fillMaxWidth().padding(start = 32.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, unfocusedBorderColor = theme.contentColor.copy(alpha = 0.2f), focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = enigmaHint,
            onValueChange = onEnigmaHintChange,
            label = { Text(stringResource(R.string.capture_enigma_label_hint)) },
            modifier = Modifier.fillMaxWidth().padding(start = 32.dp),
            placeholder = { Text(stringResource(R.string.capture_enigma_placeholder_hint)) },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, unfocusedBorderColor = theme.contentColor.copy(alpha = 0.2f), focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        // AUTO UNLOCK DAYS
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.capture_enigma_auto_unlock), style = MaterialTheme.typography.bodyMedium, color = theme.contentColor)
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
                stringResource(R.string.capture_enigma_unlock_after, enigmaAutoUnlockDays), 
                style = MaterialTheme.typography.labelSmall, 
                color = accent,
                modifier = Modifier.padding(start = 32.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Event, null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(stringResource(R.string.capture_scheduled_title), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = theme.contentColor)
        }
        Text(
            stringResource(R.string.capture_scheduled_desc),
            style = MaterialTheme.typography.bodySmall,
            color = theme.contentColor.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 32.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        val dateFormat = stringResource(R.string.capture_date_format)
        val dateText = scheduledTimestamp?.let {
            DateTimeFormatter.ofPattern(dateFormat, Locale.FRENCH)
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(it))
        } ?: stringResource(R.string.capture_scheduled_placeholder)
        
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
                    }) { Text(stringResource(R.string.capture_button_confirm), color = accent) }
                },
                colors = datePickerColors
            ) {
                DatePicker(state = datePickerState, colors = datePickerColors)
            }
        }

        // v12.2 : Masqué pour les vidéos et audios (seuls Textes et Photos sont éligibles au Livre)
        if (type != "VIDEO" && type != "CAMERA_VIDEO" && type != "AUDIO") {
            Spacer(modifier = Modifier.height(32.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoStories, null, tint = accent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(stringResource(R.string.capture_book_title), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = theme.contentColor)
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = includeInBook,
                    onCheckedChange = onIncludeInBookChange,
                    colors = SwitchDefaults.colors(checkedThumbColor = accent)
                )
            }
            Text(
                stringResource(R.string.capture_book_desc),
                style = MaterialTheme.typography.bodySmall,
                color = theme.contentColor.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 32.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, null, tint = accent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(stringResource(R.string.capture_soul_tone_title), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = theme.contentColor)
            }
            Text(
                stringResource(R.string.capture_soul_tone_desc),
                style = MaterialTheme.typography.bodySmall,
                color = theme.contentColor.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
            modifier = Modifier.padding(start = 32.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tones = listOf("Authentique", "Cynique", "Rigolo", "Poétique", "Brut")
            tones.forEach { tone ->
                val isSelected = soulTone == tone
                FilterChip(
                    selected = isSelected,
                    onClick = { 
                        onSoulToneChange(if (isSelected) null else tone) 
                    },
                    label = { Text(tone) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accent,
                        selectedLabelColor = theme.backgroundColor,
                        selectedLeadingIconColor = theme.backgroundColor,
                        labelColor = theme.contentColor.copy(alpha = 0.6f)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = theme.contentColor.copy(alpha = 0.1f),
                        selectedBorderColor = accent,
                        enabled = true,
                        selected = isSelected
                    )
                )
            }
        }
    }
}

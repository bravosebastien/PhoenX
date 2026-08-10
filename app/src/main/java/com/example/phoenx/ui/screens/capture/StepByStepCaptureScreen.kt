package com.example.phoenx.ui.screens.capture

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.ui.components.EnigmaForm
import com.example.phoenx.ui.components.InfoPoint
import com.example.phoenx.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StepByStepCaptureScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMap: () -> Unit, // v9.4.26
    viewModel: StepByStepCaptureViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor

    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Étape ${uiState.currentStep} sur 8",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        fontFamily = theme.fontFamily
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.currentStep > 1) viewModel.previousStep()
                        else onNavigateBack()
                    }) {
                        Icon(
                            imageVector = if (uiState.currentStep > 1) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Close,
                            contentDescription = null,
                            tint = theme.contentColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.backgroundColor)
            )
        },
        bottomBar = {
            BottomAppBar(containerColor = theme.backgroundColor, tonalElevation = 0.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.currentStep > 1) {
                        TextButton(onClick = { viewModel.nextStep() }) {
                            Text("Passer", color = theme.contentColor.copy(alpha = 0.6f))
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }

                    Button(
                        onClick = { viewModel.nextStep() },
                        enabled = when (uiState.currentStep) {
                            1 -> uiState.title.isNotBlank()
                            else -> true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Suivant", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (uiState.currentStep) {
                1 -> StepEtincelle(uiState.title, { viewModel.updateTitle(it) }, theme, accent)
                2 -> StepTonalite(
                    uiState.category,
                    uiState.tonalNuance,
                    { viewModel.updateCategory(it) },
                    { viewModel.updateTonalNuance(it) },
                    theme,
                    accent
                )
                3 -> StepDate(
                    isPeriodMode = uiState.isPeriodMode,
                    memoryDate = uiState.memoryDate,
                    memoryDateStart = uiState.memoryDateStart,
                    memoryDateEnd = uiState.memoryDateEnd,
                    onDateChange = { viewModel.updateMemoryDate(it) },
                    onPeriodChange = { s, e -> viewModel.updateMemoryPeriod(s, e) },
                    onTogglePeriod = { viewModel.togglePeriodMode(it) },
                    theme = theme,
                    accent = accent
                )
                4 -> StepLieu(
                    locationName = uiState.locationName,
                    onChooseLocation = onNavigateToMap,
                    onClearLocation = { viewModel.setLocation(null) },
                    theme = theme,
                    accent = accent
                )
                5 -> StepEnigma(
                    isEnabled = uiState.enigmaEnabled,
                    onToggleEnabled = { viewModel.updateEnigmaEnabled(it) },
                    question = uiState.enigmaQuestion,
                    onQuestionChange = { viewModel.updateEnigmaQuestion(it) },
                    answer = uiState.enigmaAnswer,
                    onAnswerChange = { viewModel.updateEnigmaAnswer(it) },
                    hint = uiState.enigmaHint,
                    onHintChange = { viewModel.updateEnigmaHint(it) },
                    autoUnlockDays = uiState.autoUnlockDays,
                    onAutoUnlockDaysChange = { viewModel.updateAutoUnlockDays(it) },
                    isUltimateSecret = uiState.isUltimateSecret,
                    onUltimateSecretToggle = { viewModel.updateUltimateSecret(it) },
                    includeInBook = uiState.includeInBook,
                    onIncludeInBookToggle = { viewModel.updateIncludeInBook(it) },
                    theme = theme,
                    accent = accent
                )
            }
        }
    }
}

@Composable
fun StepEnigma(
    isEnabled: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    question: String,
    onQuestionChange: (String) -> Unit,
    answer: String,
    onAnswerChange: (String) -> Unit,
    hint: String,
    onHintChange: (String) -> Unit,
    autoUnlockDays: Int?,
    onAutoUnlockDaysChange: (Int?) -> Unit,
    isUltimateSecret: Boolean,
    onUltimateSecretToggle: (Boolean) -> Unit,
    includeInBook: Boolean, // v9.4.27
    onIncludeInBookToggle: (Boolean) -> Unit, // v9.4.27
    theme: AppThemeState,
    accent: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "LE COFFRE-FORT",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
            color = accent
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        EnigmaForm(
            isEnabled = isEnabled,
            onToggleEnabled = onToggleEnabled,
            question = question,
            onQuestionChange = onQuestionChange,
            answer = answer,
            onAnswerChange = onAnswerChange,
            hasExistingAnswer = false, // Toujours false en création
            hint = hint,
            onHintChange = onHintChange,
            autoUnlockDays = autoUnlockDays,
            onAutoUnlockDaysChange = onAutoUnlockDaysChange,
            isUltimateSecret = isUltimateSecret,
            onUltimateSecretToggle = onUltimateSecretToggle,
            theme = theme,
            accent = accent,
            isReadOnly = false
        )

        Spacer(modifier = Modifier.height(40.dp))
        HorizontalDivider(color = theme.contentColor.copy(alpha = 0.1f))
        Spacer(modifier = Modifier.height(24.dp))

        // Option Souveraineté (v9.4.27)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onIncludeInBookToggle(!includeInBook) }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = includeInBook,
                onCheckedChange = { onIncludeInBookToggle(it) },
                colors = CheckboxDefaults.colors(checkedColor = accent)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    "Inclure dans mon Livre de Vie", 
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = theme.contentColor
                )
                Text(
                    "Permettre à l'IA de s'appuyer sur ce souvenir pour rédiger votre récit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.contentColor.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun StepLieu(
    locationName: String?,
    onChooseLocation: () -> Unit,
    onClearLocation: () -> Unit,
    theme: AppThemeState,
    accent: Color
) {
    Text(
        text = "LE LIEU",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
        color = accent
    )
    Text(
        text = "Où ce souvenir s'est-il déroulé ? Tu peux l'épingler sur ta Mappemonde.",
        style = MaterialTheme.typography.bodySmall,
        color = theme.contentColor.copy(alpha = 0.5f),
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChooseLocation() }
            .border(1.dp, theme.contentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
        color = theme.contentColor.copy(alpha = 0.03f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (locationName != null) Icons.Default.LocationOn else Icons.Default.AddLocation,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = locationName ?: "Choisir un lieu sur la carte",
                color = if (locationName != null) theme.contentColor else theme.contentColor.copy(alpha = 0.5f),
                fontWeight = if (locationName != null) FontWeight.Bold else FontWeight.Normal,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }

    if (locationName != null) {
        TextButton(
            onClick = onClearLocation,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Retirer le lieu", color = Error.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun StepEtincelle(title: String, onTitleChange: (String) -> Unit, theme: AppThemeState, accent: Color) {
    Text(
        text = "L'ÉTINCELLE",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
        color = accent
    )
    Text(
        text = "Donne un nom ou un sujet court à ce souvenir pour capturer l'instant.",
        style = MaterialTheme.typography.bodySmall,
        color = theme.contentColor.copy(alpha = 0.5f),
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
    )

    TextField(
        value = title,
        onValueChange = onTitleChange,
        placeholder = {
            Text(
                "Quel est le sujet ?",
                style = MaterialTheme.typography.headlineSmall,
                color = theme.contentColor.copy(alpha = 0.3f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        modifier = Modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.headlineSmall.copy(
            color = theme.contentColor,
            fontFamily = theme.fontFamily,
            textAlign = TextAlign.Center
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = accent,
            unfocusedIndicatorColor = theme.contentColor.copy(alpha = 0.1f),
            focusedTextColor = theme.contentColor,
            unfocusedTextColor = theme.contentColor
        )
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StepTonalite(
    selectedCategory: String, 
    tonalNuance: String,
    onCategoryChange: (String) -> Unit, 
    onNuanceChange: (String) -> Unit,
    theme: AppThemeState, 
    accent: Color
) {
    Text(
        text = "LA TONALITÉ",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
        color = accent
    )
    Text(
        text = "L'esprit du souvenir. Cela aide l'IA à comprendre le sens profond de ton récit.",
        style = MaterialTheme.typography.bodySmall,
        color = theme.contentColor.copy(alpha = 0.5f),
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
    )

    val categories = listOf("Sagesse", "Aventure", "Secret", "Famille", "Amour", "Nostalgie", "Humour", "Leçon", "Voyage", "Quotidien", "Épreuve")
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { cat ->
            FilterChip(
                selected = selectedCategory == cat,
                onClick = { onCategoryChange(cat) },
                label = { Text(cat) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accent,
                    selectedLabelColor = theme.backgroundColor
                )
            )
        }
    }

    Spacer(modifier = Modifier.height(32.dp))
    
    OutlinedTextField(
        value = tonalNuance,
        onValueChange = { if (it.length <= 100) onNuanceChange(it) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Précisez la nuance (facultatif)", fontSize = 11.sp) },
        placeholder = { Text("Ex : un peu amer mais je souris en l'écrivant...", fontSize = 11.sp) },
        maxLines = 3,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accent.copy(alpha = 0.5f),
            unfocusedBorderColor = theme.contentColor.copy(alpha = 0.1f),
            focusedTextColor = theme.contentColor,
            unfocusedTextColor = theme.contentColor
        ),
        supportingText = {
            Text("${tonalNuance.length}/100", modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.End, fontSize = 10.sp)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepDate(
    isPeriodMode: Boolean,
    memoryDate: Long?,
    memoryDateStart: Long?,
    memoryDateEnd: Long?,
    onDateChange: (Long?) -> Unit,
    onPeriodChange: (Long?, Long?) -> Unit,
    onTogglePeriod: (Boolean) -> Unit,
    theme: AppThemeState,
    accent: Color
) {
    Text(
        text = "LE MOMENT",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
        color = accent
    )
    Text(
        text = "Quand cela s'est-il passé ?",
        style = MaterialTheme.typography.bodySmall,
        color = theme.contentColor.copy(alpha = 0.5f),
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
    ) {
        Text("Date précise", style = MaterialTheme.typography.labelSmall, color = if (!isPeriodMode) accent else theme.contentColor.copy(alpha = 0.4f))
        Switch(
            checked = isPeriodMode,
            onCheckedChange = onTogglePeriod,
            modifier = Modifier.scale(0.7f).padding(horizontal = 8.dp),
            colors = SwitchDefaults.colors(checkedThumbColor = accent)
        )
        Text("Période", style = MaterialTheme.typography.labelSmall, color = if (isPeriodMode) accent else theme.contentColor.copy(alpha = 0.4f))
    }

    if (!isPeriodMode) {
        var showDatePicker by remember { mutableStateOf(false) }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = memoryDate ?: System.currentTimeMillis())

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePicker = true }
                .border(1.dp, theme.contentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
            color = theme.contentColor.copy(alpha = 0.03f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.CalendarToday, null, tint = accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                val dateText = memoryDate?.let {
                    SimpleDateFormat("dd MMMM yyyy", Locale.FRENCH).format(Date(it))
                } ?: "Choisir une date"
                Text(dateText, color = theme.contentColor, fontWeight = FontWeight.Bold)
            }
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        onDateChange(datePickerState.selectedDateMillis)
                        showDatePicker = false
                    }) { Text("Confirmer", color = accent) }
                }
            ) {
                DatePicker(
                    state = datePickerState,
                    colors = DatePickerDefaults.colors(
                        containerColor = theme.backgroundColor,
                        titleContentColor = theme.contentColor,
                        headlineContentColor = theme.contentColor,
                        selectedDayContainerColor = accent,
                        selectedDayContentColor = theme.backgroundColor,
                        todayContentColor = accent,
                        todayDateBorderColor = accent
                    )
                )
            }
        }
    } else {
        var showStartPicker by remember { mutableStateOf(false) }
        var showEndPicker by remember { mutableStateOf(false) }

        val startState = rememberDatePickerState(initialSelectedDateMillis = memoryDateStart ?: System.currentTimeMillis())
        val endState = rememberDatePickerState(initialSelectedDateMillis = memoryDateEnd ?: System.currentTimeMillis())

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { showStartPicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
            ) {
                Icon(Icons.Default.CalendarToday, null, tint = accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(12.dp))
                val txt = memoryDateStart?.let { SimpleDateFormat("dd MMMM yyyy", Locale.FRENCH).format(Date(it)) } ?: "Date de début"
                Text(txt, color = theme.contentColor)
            }

            OutlinedButton(
                onClick = { showEndPicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
            ) {
                Icon(Icons.Default.CalendarToday, null, tint = accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(12.dp))
                val txt = memoryDateEnd?.let { SimpleDateFormat("dd MMMM yyyy", Locale.FRENCH).format(Date(it)) } ?: "Date de fin"
                Text(txt, color = theme.contentColor)
            }
        }

        if (showStartPicker) {
            DatePickerDialog(
                onDismissRequest = { showStartPicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        onPeriodChange(startState.selectedDateMillis, memoryDateEnd)
                        showStartPicker = false
                    }) { Text("Confirmer", color = accent) }
                }
            ) { DatePicker(state = startState) }
        }
        if (showEndPicker) {
            DatePickerDialog(
                onDismissRequest = { showEndPicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        onPeriodChange(memoryDateStart, endState.selectedDateMillis)
                        showEndPicker = false
                    }) { Text("Confirmer", color = accent) }
                }
            ) { DatePicker(state = endState) }
        }
    }
}

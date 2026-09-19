package com.example.phoenx.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.lazy.items
import com.example.phoenx.ui.screens.book.BookThemeOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.R
import com.example.phoenx.ui.MainViewModel
import com.example.phoenx.ui.components.GlobalThemeSelector
import com.example.phoenx.ui.components.HsvColorPicker
import com.example.phoenx.ui.components.InfoButton
import com.example.phoenx.ui.components.RecoveryPhraseBottomSheet
import com.example.phoenx.ui.theme.*
import kotlinx.coroutines.launch

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.rotate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToProtocol: () -> Unit,
    onNavigateToAccessibility: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToNotificationContacts: () -> Unit,
    onNavigateToReconciliation: () -> Unit,
    onNavigateToRecipients: () -> Unit,
    onNavigateToUniqueKey: () -> Unit,
    onNavigateToDetective: () -> Unit,
    onNavigateToLegacyVideo: () -> Unit,
    onVerifyBiometrics: (onSuccess: () -> Unit) -> Unit,
    mainViewModel: MainViewModel,
    themeViewModel: ThemeViewModel = hiltViewModel(),
    initialShowRecovery: Boolean = false
) {
    val isBiometricEnabled by mainViewModel.isBiometricEnabled.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val backgroundId by themeViewModel.globalBackgroundId.collectAsState()
    val fontId by themeViewModel.globalFontId.collectAsState()
    var showRecoveryPhrase by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // ═══ SYSTÈME AVANCÉ EN VEILLE ═══
    /*
    val recoveryPhraseString by mainViewModel.recoveryPhrase.collectAsState(initial = null)

    // Déclenchement automatique de la biométrie si demandé par le rappel
    LaunchedEffect(initialShowRecovery) {
        if (initialShowRecovery) {
            onVerifyBiometrics {
                showRecoveryPhrase = true
            }
        }
    }

    if (showRecoveryPhrase && recoveryPhraseString != null) {
        RecoveryPhraseBottomSheet(
            phrase = recoveryPhraseString!!.split(" "),
            onDismiss = { showRecoveryPhrase = false }
        )
    }
    */

    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.settings_screen_title), style = MaterialTheme.typography.labelLarge, color = theme.contentColor, fontWeight = FontWeight.Bold)
                        InfoButton(
                            title = stringResource(R.string.settings_info_title),
                            points = listOf(
                                stringResource(R.string.settings_info_point1),
                                stringResource(R.string.settings_info_point2),
                                stringResource(R.string.settings_info_point3),
                                stringResource(R.string.settings_info_point4),
                                stringResource(R.string.settings_info_point5)
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = theme.contentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.backgroundColor, titleContentColor = theme.contentColor)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(stringResource(R.string.settings_section_account), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent)
            Spacer(modifier = Modifier.height(16.dp))

            SettingsItem(
                title = stringResource(R.string.settings_item_profile_title),
                subtitle = stringResource(R.string.settings_item_profile_subtitle),
                icon = Icons.Default.AccountCircle,
                theme = theme,
                onClick = onNavigateToProfile
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(stringResource(R.string.settings_section_security), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent)
            Spacer(modifier = Modifier.height(16.dp))
            
            SettingsItem(
                title = stringResource(R.string.settings_item_protocol_title),
                subtitle = stringResource(R.string.settings_item_protocol_subtitle),
                icon = Icons.Default.Lock,
                theme = theme,
                onClick = onNavigateToProtocol
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsItem(
                title = stringResource(R.string.settings_item_recipients_title),
                subtitle = stringResource(R.string.settings_item_recipients_subtitle),
                icon = Icons.Default.Person,
                theme = theme,
                onClick = onNavigateToRecipients
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsItem(
                title = stringResource(R.string.settings_item_notification_contacts_title),
                subtitle = stringResource(R.string.settings_item_notification_contacts_subtitle),
                icon = Icons.Default.NotificationsNone,
                theme = theme,
                onClick = onNavigateToNotificationContacts
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsItem(
                title = "Vidéo de mon héritage",
                subtitle = "Une vidéo unique, visible par tous vos destinataires",
                icon = Icons.Default.VideoCall,
                theme = theme,
                onClick = onNavigateToLegacyVideo
            )

            Spacer(modifier = Modifier.height(16.dp))

            var showRhythmDialog by remember { mutableStateOf(false) }
            val currentRhythm by mainViewModel.silenceRhythmDays.collectAsState()

            SettingsItem(
                title = stringResource(R.string.settings_item_presence_title),
                subtitle = stringResource(R.string.settings_item_presence_subtitle, currentRhythm),
                icon = Icons.Default.Timer,
                theme = theme,
                onClick = { showRhythmDialog = true }
            )

            if (showRhythmDialog) {
                RhythmSelectionDialog(
                    initialRhythm = currentRhythm,
                    onDismiss = { showRhythmDialog = false },
                    theme = theme,
                    onConfirm = { days ->
                        scope.launch {
                            mainViewModel.setSilenceConfig(days)
                            showRhythmDialog = false
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                color = theme.contentColor.copy(alpha = 0.05f),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Fingerprint, null, tint = accent)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_biometric_title), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = theme.contentColor)
                        Text(stringResource(R.string.settings_biometric_subtitle), style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.6f))
                    }
                    Switch(
                        checked = isBiometricEnabled,
                        onCheckedChange = { mainViewModel.toggleBiometric(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = accent)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            // v12.7 : "Le Tiroir à Clé Unique" retiré des Réglages — faisait doublon avec les
            // autres verrous (Coffre-Fort / Secret Ultime) et n'était pas fiable (voir ci-dessous).

            SettingsItem(
                title = stringResource(R.string.settings_item_reconciliation_title),
                subtitle = stringResource(R.string.settings_item_reconciliation_subtitle),
                icon = Icons.Default.Mail,
                theme = theme,
                onClick = onNavigateToReconciliation
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsItem(
                title = stringResource(R.string.settings_item_detective_title),
                subtitle = stringResource(R.string.settings_item_detective_subtitle),
                icon = Icons.Default.Fingerprint,
                theme = theme,
                onClick = onNavigateToDetective
            )

            Spacer(modifier = Modifier.height(16.dp))

            Spacer(modifier = Modifier.height(32.dp))

            /* ═══ SYSTÈME AVANCÉ EN VEILLE ═══
            // Masqué jusqu'à réactivation V2 Pro
            Text("SÉCURITÉ", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent)
            Spacer(modifier = Modifier.height(16.dp))

            SettingsItem(
                title = "Revoir ma phrase de récupération",
                subtitle = "Tes 12 mots de secours",
                icon = Icons.Default.VpnKey,
                theme = theme,
                onClick = {
                    onVerifyBiometrics {
                        showRecoveryPhrase = true
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
            */

            Text(stringResource(R.string.settings_section_accessibility), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent)
            Spacer(modifier = Modifier.height(16.dp))

            SettingsItem(
                title = stringResource(R.string.settings_item_accessibility_title),
                subtitle = stringResource(R.string.settings_item_accessibility_subtitle),
                icon = Icons.Default.RecordVoiceOver,
                theme = theme,
                onClick = onNavigateToAccessibility
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsItem(
                title = "Language",
                subtitle = "Français, English, Español",
                icon = Icons.Default.Language,
                theme = theme,
                onClick = onNavigateToLanguage
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsItem(
                title = stringResource(R.string.settings_item_reset_video_title),
                subtitle = stringResource(R.string.settings_item_reset_video_subtitle),
                icon = Icons.Default.VideoLibrary,
                theme = theme,
                onClick = { mainViewModel.resetVideoBanner() }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(stringResource(R.string.settings_section_personalization), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent)
            Spacer(modifier = Modifier.height(16.dp))

            var showAccentPicker by remember { mutableStateOf(false) }
            var showBackgroundPicker by remember { mutableStateOf(false) }

            // v12.7 : Nouvelle organisation par bandeaux dépliables pour la couleur libre
            ExpandablePersonalizationBanner(
                title = "Couleur icônes et bordures",
                isExpanded = showAccentPicker,
                onToggle = { showAccentPicker = !showAccentPicker },
                theme = theme,
                accent = accent
            ) {
                val currentAccentInt by mainViewModel.accentColor.collectAsState()
                HsvColorPicker(
                    initialColor = Color(currentAccentInt),
                    onColorChanged = { color -> mainViewModel.setAccentColor(color.toArgb()) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            ExpandablePersonalizationBanner(
                title = "Couleur fond d\'écran",
                isExpanded = showBackgroundPicker,
                onToggle = { showBackgroundPicker = !showBackgroundPicker },
                theme = theme,
                accent = accent
            ) {
                val currentBgColor by themeViewModel.globalBackgroundColor.collectAsState()
                HsvColorPicker(
                    initialColor = currentBgColor,
                    onColorChanged = { color -> themeViewModel.setGlobalBackgroundColor(color) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // v12.7 : Le sélecteur de police est conservé mais séparé des couleurs de fond
            // qui sont désormais libres (HsvColorPicker).
            FontSelector(
                currentFontId = fontId,
                onFontChange = { font -> themeViewModel.setGlobalTheme(backgroundId, font) },
                theme = theme,
                accent = accent
            )

            Spacer(modifier = Modifier.height(32.dp))

            TextButton(
                onClick = { themeViewModel.resetToDefaults() },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.profile_button_reset_defaults))
            }
        }
    }
}

@Composable
fun RhythmSelectionDialog(
    initialRhythm: Int,
    onDismiss: () -> Unit,
    theme: AppThemeState,
    onConfirm: (Int) -> Unit
) {
    val accent = theme.accentColor
    var selectedRythm by remember { mutableIntStateOf(initialRhythm) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.backgroundColor,
        title = { Text(stringResource(R.string.settings_rhythm_dialog_title), color = theme.contentColor, style = MaterialTheme.typography.titleLarge.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold)) },
        text = {
            Column(Modifier.selectableGroup()) {
                RhythmOptionItem(14, stringResource(R.string.settings_rhythm_option_2_weeks), selectedRythm == 14, theme) { selectedRythm = 14 }
                RhythmOptionItem(30, stringResource(R.string.settings_rhythm_option_1_month), selectedRythm == 30, theme) { selectedRythm = 30 }
                RhythmOptionItem(60, stringResource(R.string.settings_rhythm_option_2_months), selectedRythm == 60, theme) { selectedRythm = 60 }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    stringResource(R.string.settings_rhythm_info),
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.contentColor.copy(alpha = 0.6f)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedRythm) },
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                modifier = Modifier.phoenXMatiere()
            ) {
                Text(stringResource(R.string.settings_button_save), color = theme.backgroundColor, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_button_cancel), color = theme.contentColor.copy(alpha = 0.4f))
            }
        }
    )
}

@Composable
fun RhythmOptionItem(days: Int, label: String, selected: Boolean, theme: AppThemeState, onClick: () -> Unit) {
    val accent = theme.accentColor
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = accent)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = if (selected) theme.contentColor else theme.contentColor.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    theme: AppThemeState,
    onClick: () -> Unit
) {
    val accent = theme.accentColor
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = theme.contentColor.copy(alpha = 0.03f),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = accent)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = theme.contentColor)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.6f))
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = theme.contentColor.copy(alpha = 0.2f))
        }
    }
}

@Composable
fun ExpandablePersonalizationBanner(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    theme: AppThemeState,
    accent: Color,
    content: @Composable () -> Unit
) {
    val rotation by animateFloatAsState(if (isExpanded) 180f else 0f, label = "chevron")

    Surface(
        color = theme.contentColor.copy(alpha = 0.05f),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = theme.contentColor
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.rotate(rotation)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
fun FontSelector(
    currentFontId: String,
    onFontChange: (String) -> Unit,
    theme: AppThemeState,
    accent: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.theme_selector_plume),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = theme.contentColor.copy(alpha = 0.4f),
            modifier = Modifier.padding(bottom = 12.dp)
        )
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(BookThemeOptions.fonts) { font ->
                val isSelected = currentFontId == font.id
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(70.dp).clickable { onFontChange(font.id) }
                ) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = theme.contentColor.copy(alpha = 0.03f),
                        border = BorderStroke(1.dp, if (isSelected) accent.copy(alpha = 0.6f) else theme.contentColor.copy(alpha = 0.1f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Aa",
                                style = MaterialTheme.typography.titleLarge.copy(fontFamily = font.fontFamily),
                                color = if (isSelected) accent else theme.contentColor.copy(alpha = 0.6f)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(font.nameRes),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = if (isSelected) accent else theme.contentColor.copy(alpha = 0.4f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

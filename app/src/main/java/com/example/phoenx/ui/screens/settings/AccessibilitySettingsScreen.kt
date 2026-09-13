package com.example.phoenx.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.R
import com.example.phoenx.ui.MainViewModel
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibilitySettingsScreen(
    onNavigateBack: () -> Unit,
    mainViewModel: MainViewModel
) {
    val isVoiceActive by mainViewModel.isVoiceModeActive.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor

    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.accessibility_screen_title), style = MaterialTheme.typography.labelLarge, color = theme.contentColor, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.backgroundColor)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
            ) {
                Text(
                    stringResource(R.string.accessibility_voice_mode_title),
                    style = MaterialTheme.typography.displaySmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold),
                    color = theme.contentColor
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    stringResource(R.string.accessibility_voice_mode_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = theme.contentColor.copy(alpha = 0.7f),
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(40.dp))

                Surface(
                    color = theme.contentColor.copy(alpha = 0.05f),
                    shape = MaterialTheme.shapes.large,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isVoiceActive) accent else theme.contentColor.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.RecordVoiceOver, 
                            null, 
                            tint = if (isVoiceActive) accent else theme.contentColor.copy(alpha = 0.2f),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.accessibility_activate_voice_label), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = theme.contentColor)
                            Text(
                                if (isVoiceActive) stringResource(R.string.accessibility_status_listening) else stringResource(R.string.accessibility_status_disabled),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isVoiceActive) accent else theme.contentColor.copy(alpha = 0.4f)
                            )
                        }
                        Switch(
                            checked = isVoiceActive,
                            onCheckedChange = { mainViewModel.toggleVoiceMode() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = accent,
                                checkedTrackColor = accent.copy(alpha = 0.3f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (isVoiceActive) {
                    Text(
                        stringResource(R.string.accessibility_commands_title),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = accent,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val commands = listOf(
                        stringResource(R.string.accessibility_command_deposit),
                        stringResource(R.string.accessibility_command_timeline),
                        stringResource(R.string.accessibility_command_night_mode),
                        stringResource(R.string.accessibility_command_home),
                        stringResource(R.string.accessibility_command_help)
                    )
                    
                    commands.forEach { cmd ->
                        Text(
                            text = "• $cmd",
                            style = MaterialTheme.typography.bodySmall,
                            color = theme.contentColor.copy(alpha = 0.7f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

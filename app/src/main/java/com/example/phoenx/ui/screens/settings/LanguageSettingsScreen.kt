package com.example.phoenx.ui.screens.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import com.example.phoenx.ui.theme.LocalAppTheme

data class LanguageOption(
    val name: String,
    val code: String, // Empty for system default
    val flag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    
    val languages = remember {
        listOf(
            LanguageOption("System Default", "", "🌐"),
            LanguageOption("Français", "fr", "🇫🇷"),
            LanguageOption("English", "en", "🇬🇧"),
            LanguageOption("Español", "es", "🇪🇸")
        )
    }

    // Current locale from AppCompatDelegate
    val currentLocaleCode = remember {
        AppCompatDelegate.getApplicationLocales().toLanguageTags().ifEmpty { "" }
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                title = { Text("Language", color = theme.contentColor, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = theme.contentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.backgroundColor)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            languages.forEach { lang ->
                val isSelected = if (lang.code.isEmpty()) {
                    currentLocaleCode.isEmpty()
                } else {
                    currentLocaleCode.startsWith(lang.code)
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val appLocale: LocaleListCompat = if (lang.code.isEmpty()) {
                                LocaleListCompat.getEmptyLocaleList()
                            } else {
                                LocaleListCompat.forLanguageTags(lang.code)
                            }
                            AppCompatDelegate.setApplicationLocales(appLocale)
                        },
                    color = if (isSelected) accent.copy(alpha = 0.1f) else theme.contentColor.copy(alpha = 0.03f),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) accent else theme.contentColor.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(lang.flag, fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = lang.name,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = theme.contentColor,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = accent
                            )
                        }
                    }
                }
            }
        }
    }
}

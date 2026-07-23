package com.example.phoenx.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun TimelineScreen() {
    val theme = com.example.phoenx.ui.theme.LocalAppTheme.current
    Box(
        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(text = "Le Fil de Pensée", color = theme.contentColor, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontFamily = theme.fontFamily))
    }
}

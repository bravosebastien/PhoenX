package com.example.phoenx.ui.screens.capture

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.ui.theme.*

@Composable
fun NightCaptureContent(
    isRecording: Boolean,
    onStart: () -> Unit
) {
    LaunchedEffect(Unit) {
        if (!isRecording) onStart()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "CAPTURE INVISIBLE",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.4f),
                letterSpacing = 4.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Surface(
                modifier = Modifier.size(4.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.5f)
            ) {}
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Parle. On garde tout.\nTouches VOLUME pour arrêter.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

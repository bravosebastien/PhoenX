package com.example.phoenx.ui.screens.universal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.ui.theme.LocalAppTheme
import com.example.phoenx.ui.theme.phoenXMatiere

@Composable
fun BecomeCreatorPromptScreen(
    role: String,
    creatorName: String,
    onBecomeCreator: () -> Unit,
    onLater: () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor

    val message = when (role) {
        "witness" -> "$creatorName a choisi de vous confier un témoignage précieux sur son histoire."
        "depositary" -> "$creatorName a fait de vous le Gardien de sa mémoire."
        "recipient" -> "$creatorName a choisi de vous transmettre une part de son histoire."
        else -> "$creatorName vous a invité dans son cercle de confiance."
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = theme.fontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = theme.contentColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Vivez-vous aussi l'expérience PHOEN-X.\nCommencez à sceller vos propres souvenirs pour ceux que vous aimez.",
                style = MaterialTheme.typography.bodyLarge,
                color = theme.contentColor.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 28.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onBecomeCreator,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .phoenXMatiere(),
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Devenir Créateur", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onLater) {
                Text("Plus tard", color = theme.contentColor.copy(alpha = 0.4f))
            }
        }
    }
}

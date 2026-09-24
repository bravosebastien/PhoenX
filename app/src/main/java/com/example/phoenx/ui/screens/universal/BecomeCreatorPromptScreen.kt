package com.example.phoenx.ui.screens.universal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.phoenx.R
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

    // Analytics (v13.2) : écran affiché une seule fois dans la vie du compte (voir appelant).
    val context = LocalContext.current
    val analyticsTracker = remember(context) {
        dagger.hilt.android.EntryPointAccessors.fromApplication(
            context.applicationContext,
            com.example.phoenx.data.analytics.AnalyticsTracker.AnalyticsEntryPoint::class.java
        ).analyticsTracker()
    }
    LaunchedEffect(role) {
        analyticsTracker.logBecomeCreatorPromptShown(role)
    }

    val message = when (role) {
        "witness" -> stringResource(R.string.become_creator_witness_msg, creatorName)
        "depositary" -> stringResource(R.string.become_creator_depositary_msg, creatorName)
        "recipient" -> stringResource(R.string.become_creator_recipient_msg, creatorName)
        else -> stringResource(R.string.become_creator_fallback_msg, creatorName)
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
                text = stringResource(R.string.become_creator_invite_desc),
                style = MaterialTheme.typography.bodyLarge,
                color = theme.contentColor.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 28.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    analyticsTracker.logBecomeCreatorAccepted(role)
                    onBecomeCreator()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .phoenXMatiere(),
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.become_creator_button), color = theme.backgroundColor, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = {
                analyticsTracker.logBecomeCreatorLater(role)
                onLater()
            }) {
                Text(stringResource(R.string.become_creator_later), color = theme.contentColor.copy(alpha = 0.4f))
            }
        }
    }
}

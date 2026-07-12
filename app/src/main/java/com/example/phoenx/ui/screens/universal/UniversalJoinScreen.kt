package com.example.phoenx.ui.screens.universal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.ui.theme.*
import com.google.firebase.auth.FirebaseAuth

@Composable
fun UniversalJoinScreen(
    token: String,
    onNavigateToAuth: (String) -> Unit,
    onSuccess: () -> Unit,
    viewModel: UniversalJoinViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val accent = LocalAccentColor.current

    LaunchedEffect(Unit) {
        viewModel.loadInvitation(token)
    }

    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            onSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalBackgroundBrush.current)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(color = accent)
            }
            uiState.error != null -> {
                ErrorView(uiState.error!!, accent)
            }
            uiState.invitation != null -> {
                InvitationView(
                    invitation = uiState.invitation!!,
                    isLoggedIn = currentUser != null,
                    accent = accent,
                    onAccept = { viewModel.acceptInvitation(token) },
                    onAuth = { onNavigateToAuth(token) }
                )
            }
        }
    }
}

@Composable
fun InvitationView(
    invitation: InvitationDetails,
    isLoggedIn: Boolean,
    accent: Color,
    onAccept: () -> Unit,
    onAuth: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = accent.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.People, null, tint = accent, modifier = Modifier.size(40.dp))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Bonjour,",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary
        )
        
        Text(
            text = "${invitation.creatorName} vous invite",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
            ),
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        val roleText = when(invitation.role) {
            "depositary" -> "à être son Gardien de Confiance"
            "witness" -> "à porter témoignage sur son histoire"
            "recipient" -> "à être l'un de ses héritiers"
            else -> "à rejoindre son cercle"
        }

        Text(
            text = roleText,
            style = MaterialTheme.typography.bodyLarge,
            color = accent,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(32.dp))

        val pedagogie = when(invitation.role) {
            "depositary" -> "En tant que Gardien, vous serez la clé qui déverrouille sa mémoire le moment venu. Votre rôle est de confirmer son absence définitive."
            "witness" -> "Votre témoignage enrichira son héritage. Ce que vous écrirez restera scellé et ne sera transmis qu'après son départ."
            "recipient" -> "Vous avez été choisi pour recevoir une partie de sa mémoire et de ses souvenirs les plus précieux."
            else -> ""
        }

        Surface(
            color = Color.White.copy(alpha = 0.05f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = pedagogie,
                modifier = Modifier.padding(20.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        if (isLoggedIn) {
            Button(
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Accepter ce rôle", color = BackgroundPrimary, fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = onAuth,
                modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Se connecter pour accepter", color = BackgroundPrimary, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "L'accès est réservé à l'adresse : ${invitation.targetEmail}",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary
            )
        }
    }
}

@Composable
fun ErrorView(message: String, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.ErrorOutline, null, tint = Error, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
    }
}

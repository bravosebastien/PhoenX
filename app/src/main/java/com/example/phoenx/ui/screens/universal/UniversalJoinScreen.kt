package com.example.phoenx.ui.screens.universal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
    onSuccess: (String?, String?, String?) -> Unit,
    viewModel: UniversalJoinViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val theme = LocalAppTheme.current
    val accent = theme.accentColor

    LaunchedEffect(Unit) {
        viewModel.loadInvitation(token)
    }

    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            onSuccess(uiState.acceptedRole, uiState.creatorId, uiState.sourceId)
        }
    }

    // Détection mismatch email (v9.4.16)
    val emailMismatch = currentUser != null && 
                        uiState.invitation != null && 
                        currentUser.email?.lowercase() != uiState.invitation?.targetEmail?.lowercase()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(color = accent)
            }
            uiState.error != null -> {
                ErrorView(uiState.error!!, accent, theme)
            }
            emailMismatch -> {
                EmailMismatchView(
                    currentEmail = currentUser?.email ?: "",
                    targetEmail = uiState.invitation?.targetEmail ?: "",
                    accent = accent,
                    theme = theme,
                    onLogoutAndContinue = {
                        FirebaseAuth.getInstance().signOut()
                        onNavigateToAuth(token)
                    }
                )
            }
            uiState.invitation != null -> {
                InvitationView(
                    invitation = uiState.invitation!!,
                    isLoggedIn = currentUser != null,
                    accent = accent,
                    theme = theme,
                    onAccept = { viewModel.acceptInvitation(token) }
                ) { onNavigateToAuth(token) }
            }
        }
    }
}

@Composable
fun EmailMismatchView(
    currentEmail: String,
    targetEmail: String,
    accent: Color,
    theme: AppThemeState,
    onLogoutAndContinue: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = Error,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Mauvais compte détecté",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = theme.contentColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Cette invitation est destinée à :\n",
            style = MaterialTheme.typography.bodyMedium,
            color = theme.contentColor.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        
        Text(
            text = targetEmail,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = accent,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "\nMais vous êtes actuellement connecté avec :\n",
            style = MaterialTheme.typography.bodyMedium,
            color = theme.contentColor.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Text(
            text = currentEmail,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = theme.contentColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onLogoutAndContinue,
            modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
            colors = ButtonDefaults.buttonColors(containerColor = accent),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Se déconnecter et continuer", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun InvitationView(
    invitation: InvitationDetails,
    isLoggedIn: Boolean,
    accent: Color,
    theme: AppThemeState,
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
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = theme.contentColor
        )
        
        Text(
            text = "${invitation.creatorName} vous invite",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = theme.fontFamily,
                fontWeight = FontWeight.Bold
            ),
            color = theme.contentColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        val roleText = when(invitation.role) {
            "depositary" -> "à être son Gardien de Confiance"
            "witness" -> "à porter témoignage sur son histoire"
            "recipient" -> "à être l'un de ses destinataires"
            "mirror_partner" -> "à un Miroir à Deux"
            else -> "à rejoindre son cercle"
        }

        Text(
            text = roleText,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = accent
        )

        Spacer(modifier = Modifier.height(32.dp))

        val pedagogie = when(invitation.role) {
            "depositary" -> "En tant que Gardien, vous serez la clé qui déverrouille sa mémoire le moment venu. Votre rôle est de confirmer son absence définitive."
            "witness" -> "Votre témoignage enrichira son héritage. Ce que vous écrirez restera scellé et ne sera transmis qu'après son départ."
            "recipient" -> "Vous avez été choisi pour recevoir une partie de sa mémoire et de ses souvenirs les plus précieux."
            "mirror_partner" -> "Deux points de vue pour une même histoire. Chacun écrit sa version de son côté, et le Miroir ne se révélera que lorsque vous aurez tous les deux terminé."
            else -> ""
        }

        Surface(
            color = theme.contentColor.copy(alpha = 0.05f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
        ) {
            Text(
                text = pedagogie,
                modifier = Modifier.padding(20.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = theme.contentColor.copy(alpha = 0.7f),
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
                Text("Accepter ce rôle", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = onAuth,
                modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Se connecter pour accepter", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "L'accès est réservé à l'adresse : ${invitation.targetEmail}",
                style = MaterialTheme.typography.labelSmall,
                color = theme.contentColor.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
fun ErrorView(message: String, accent: Color, theme: AppThemeState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.ErrorOutline, null, tint = Error, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = theme.contentColor,
            textAlign = TextAlign.Center
        )
    }
}

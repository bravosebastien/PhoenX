package com.example.phoenx.ui.screens.depositary

import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.R
import com.example.phoenx.ui.theme.*
import com.google.firebase.auth.FirebaseAuth

@Composable
fun DepositaryWelcomeScreen(
    shortCode: String,
    onUnderstood: () -> Unit,
    onNavigateToAuth: (String) -> Unit, // Nouvelle action pour rediriger vers Login/Signup
    viewModel: DepositaryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val redeemState by viewModel.redeemState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val isLoggedIn = FirebaseAuth.getInstance().currentUser != null

    // État local pour éviter les tentatives de liaison multiples
    var joinAttempted by remember { mutableStateOf(false) }

    // Tentative automatique de liaison si l'utilisateur se connecte pendant que le jeton est prêt
    LaunchedEffect(isLoggedIn, redeemState) {
        if (isLoggedIn && redeemState is RedeemState.Success && !joinAttempted) {
            joinAttempted = true
            viewModel.confirmJoin { onUnderstood() }
        }
    }

    // Liaison initiale : échange du code court (seulement si pas déjà fait)
    LaunchedEffect(Unit) {
        viewModel.redeemShortCode(shortCode)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (val state = redeemState) {
            is RedeemState.Loading -> {
                CircularProgressIndicator(color = accent)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Vérification de ton invitation...",
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.contentColor.copy(alpha = 0.6f)
                )
            }
            is RedeemState.Error -> {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = Warning,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Lien invalide ou expiré",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = theme.fontFamily,
                        color = theme.contentColor,
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = theme.contentColor.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(48.dp))
                TextButton(onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = "mailto:".toUri()
                    }
                    context.startActivity(intent)
                }) {
                    Text("Contacter mon proche", color = accent)
                }
            }
            is RedeemState.Success -> {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "PHOEN-X",
                    modifier = Modifier.size(100.dp)
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = "Tu as été choisi(e) pour une mission importante.",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = theme.fontFamily,
                        color = theme.contentColor,
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "${uiState.creatorName} t'a désigné(e) comme Dépositaire. Ce rôle ne demande rien aujourd'hui. Il te demandera, un jour, de confirmer une absence. Et de laisser une présence parler.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = theme.contentColor.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(64.dp))

                if (isLoggedIn) {
                    Button(
                        onClick = { viewModel.confirmJoin { onUnderstood() } },
                        modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Je comprends ce rôle", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { onNavigateToAuth(shortCode) },
                        modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Se connecter pour accepter", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

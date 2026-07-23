package com.example.phoenx.ui.screens.recipient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.tasks.await as kotlinAwait
import com.example.phoenx.ui.theme.*

import com.google.firebase.firestore.FirebaseFirestore

/**
 * RecipientCubeScreen (Signature PHOEN-X 5.0)
 * L'interface immersive pour les Destinataires.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipientCubeScreen(
    creatorId: String,
    onExit: () -> Unit,
    onNavigateToHeritage: () -> Unit,
    isUserCreator: Boolean = false,
    onBecomeCreator: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var creatorName by remember { mutableStateOf("Ton proche") }
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val backgroundBrush = LocalBackgroundBrush.current

    LaunchedEffect(creatorId) {
        try {
            val doc = db.collection("users").document(creatorId).get().kotlinAwait()
            creatorName = doc.getString("displayName") ?: doc.getString("email")?.substringBefore("@") ?: "Ton proche"
        } catch (e: Exception) {}
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        modifier = Modifier.background(backgroundBrush),
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("L'Armoire de $creatorName", color = theme.contentColor, style = MaterialTheme.typography.titleLarge.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold))
                        Text("Explore son héritage", color = theme.contentColor.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.Default.Close, null, tint = theme.contentColor)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Aide Jeu de Piste */ }) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, null, tint = accent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // ZONE D'IMMERSION (Simulation Armoire)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Cercle de prestige central (Halo)
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(accent.copy(alpha = 0.15f), Color.Transparent)
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Lock, 
                            null, 
                            tint = accent.copy(alpha = 0.5f), 
                            modifier = Modifier.size(80.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Text(
                        "L'Héritage Intime",
                        style = MaterialTheme.typography.headlineMedium.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold),
                        color = theme.contentColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Les objets de cette armoire s'ouvriront à toi au fil de ton exploration.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.contentColor.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        fontStyle = FontStyle.Italic
                    )
                }
            }

            // NAVIGATION RAPIDE DES TIROIRS
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "ACCÉDER À L'HÉRITAGE",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Bold),
                    color = theme.contentColor.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                
                Surface(
                    onClick = onNavigateToHeritage,
                    color = accent,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(64.dp).phoenXMatiere()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "ENTRER DANS SES SOUVENIRS", 
                            color = theme.backgroundColor, 
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        )
                    }
                }
                
                if (!isUserCreator) {
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // DEVENIR CRÉATEUR
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Et vous ?", style = MaterialTheme.typography.titleSmall, color = theme.contentColor, fontWeight = FontWeight.Bold)
                                Text("Commencez à sceller vos souvenirs.", style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.6f))
                            }
                            TextButton(onClick = onBecomeCreator) {
                                Text("DEVENIR CRÉATEUR", color = accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerShortCut(label: String, subtitle: String, modifier: Modifier = Modifier, theme: AppThemeState, onClick: () -> Unit) {
    val accent = theme.accentColor
    Surface(
        onClick = onClick,
        color = theme.contentColor.copy(alpha = 0.03f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f)),
        modifier = modifier.height(90.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = theme.contentColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = accent, fontSize = 9.sp)
        }
    }
}

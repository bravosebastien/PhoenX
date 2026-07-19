package com.example.phoenx.ui.screens.recipient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
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
    onBecomeCreator: () -> Unit
) {
    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    var creatorName by remember { mutableStateOf("Ton proche") }
    val accent = LocalAccentColor.current
    val backgroundBrush = LocalBackgroundBrush.current

    LaunchedEffect(creatorId) {
        try {
            val doc = db.collection("users").document(creatorId).get().kotlinAwait()
            creatorName = doc.getString("displayName") ?: doc.getString("email")?.substringBefore("@") ?: "Ton proche"
        } catch (e: Exception) {}
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(backgroundBrush),
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("L'Armoire de $creatorName", color = TextPrimary, style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif))
                        Text("Explore son héritage", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.Default.Close, null, tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Aide Jeu de Piste */ }) {
                        Icon(Icons.Default.HelpOutline, null, tint = accent)
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
                        style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif),
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Les objets de cette armoire s'ouvriront à toi au fil de ton exploration.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
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
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                    color = TextTertiary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                
                Surface(
                    onClick = onNavigateToHeritage,
                    color = accent,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "ENTRER DANS SES SOUVENIRS", 
                            color = BackgroundPrimary, 
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // DEVENIR CRÉATEUR
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Et vous ?", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text("Commencez à sceller vos souvenirs.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
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

@Composable
fun DrawerShortCut(label: String, subtitle: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val accent = LocalAccentColor.current
    Surface(
        onClick = onClick,
        color = SurfaceCard.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.2f)),
        modifier = modifier.height(90.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = accent, fontSize = 9.sp)
        }
    }
}

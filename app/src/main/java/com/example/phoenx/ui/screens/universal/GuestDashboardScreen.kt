package com.example.phoenx.ui.screens.universal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.phoenx.domain.model.UserRole
import com.example.phoenx.ui.MainViewModel
import com.example.phoenx.ui.navigation.Screen
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestDashboardScreen(
    navController: NavController,
    mainViewModel: MainViewModel,
    onLogout: () -> Unit
) {
    val myRoles by mainViewModel.myRoles.collectAsState()
    val pendingByEmail by mainViewModel.pendingInvitations.collectAsState()
    val isCreator by mainViewModel.isCreator.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val backgroundBrush = LocalBackgroundBrush.current

    Scaffold(
        containerColor = theme.backgroundColor,
        modifier = Modifier.background(backgroundBrush),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Espace Proches", 
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = theme.fontFamily, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold),
                        color = theme.contentColor
                    ) 
                },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("Déconnexion", color = Error, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            Text(
                "Bienvenue dans votre espace dédié. Voici les personnes qui comptent sur vous.",
                style = MaterialTheme.typography.bodyMedium,
                color = theme.contentColor.copy(alpha = 0.7f),
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (pendingByEmail.isNotEmpty()) {
                Text(
                    "INVITATIONS EN ATTENTE",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = accent,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                pendingByEmail.forEach { invite ->
                    PendingInviteCard(invite, accent, theme) {
                        navController.navigate(Screen.UniversalJoin.createRoute(invite.id))
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (myRoles.isEmpty() && pendingByEmail.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aucun rôle actif pour le moment.", color = theme.contentColor.copy(alpha = 0.4f), fontWeight = FontWeight.Bold)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Trier par nom du créateur pour la lisibilité
                    val sortedRoles = myRoles.values.toList().sortedBy { it.creatorName }
                    
                    items(sortedRoles) { role ->
                        RoleCard(
                            role = role,
                            accent = accent,
                            theme = theme,
                            onClick = {
                                when(role.role) {
                                    "depositary" -> navController.navigate(Screen.DepositaryDashboard.createRoute(role.creatorId))
                                    "witness" -> {
                                        // Accès via UID lié (v7.2)
                                        navController.navigate("witness_response/${role.creatorId}/${role.sourceId}/none")
                                    }
                                    "recipient" -> navController.navigate(Screen.RecipientCube.createRoute(role.creatorId))
                                }
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Encouragement à devenir Créateur (Uniquement si pas déjà Créateur)
            if (isCreator == false) {
                Card(
                    onClick = { navController.navigate(Screen.SilenceOnboarding.route) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.05f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Et vous ?", style = MaterialTheme.typography.titleSmall, color = theme.contentColor, fontWeight = FontWeight.Bold)
                        Text(
                            "Vous aussi, commencez à sceller vos souvenirs pour ceux que vous aimez.",
                            style = MaterialTheme.typography.bodySmall,
                            color = theme.contentColor.copy(alpha = 0.6f)
                        )
                        Text(
                            "Devenir Créateur →",
                            modifier = Modifier.padding(top = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = accent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PendingInviteCard(
    invite: com.example.phoenx.ui.MainViewModel.PendingInvitation,
    accent: Color,
    theme: AppThemeState,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.1f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.People, null, tint = accent, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(invite.creatorName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = theme.contentColor)
                Text("Vous invite à être : ${invite.label}", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.6f))
            }
            Text("Accepter", style = MaterialTheme.typography.labelMedium, color = accent, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RoleCard(
    role: UserRole,
    accent: Color,
    theme: AppThemeState,
    onClick: () -> Unit
) {
    val (icon, label, color) = when(role.role) {
        "depositary" -> Triple(Icons.Default.Lock, "Je suis son Gardien de Confiance", Success)
        "witness" -> Triple(Icons.Default.People, "Je porte témoignage", Warning)
        "recipient" -> Triple(Icons.Default.Person, "Je suis l'un de ses héritiers", accent)
        else -> Triple(Icons.Default.People, "Lien de confiance", accent)
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.03f)),
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = color.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = role.creatorName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = theme.contentColor
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.contentColor.copy(alpha = 0.6f)
                )
            }
            
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = theme.contentColor.copy(alpha = 0.2f))
        }
    }
}

@Composable
fun GuestPerspectiveContent(
    myRoles: Map<String, UserRole>,
    pendingInvites: List<com.example.phoenx.ui.MainViewModel.PendingInvitation>,
    isCreator: Boolean,
    accent: Color,
    theme: AppThemeState,
    onNavigateToCube: (String) -> Unit,
    onAcceptInvite: (String) -> Unit,
    onBecomeCreator: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            "Bienvenue dans votre espace dédié. Voici les personnes qui comptent sur vous.",
            style = MaterialTheme.typography.bodyMedium,
            color = theme.contentColor.copy(alpha = 0.7f),
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (pendingInvites.isNotEmpty()) {
            Text(
                "INVITATIONS EN ATTENTE",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = accent,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            pendingInvites.forEach { invite ->
                PendingInviteCard(invite, accent, theme) {
                    onAcceptInvite(invite.id)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // REMPLACEMENT LAZYCOLUMN PAR COLUMN POUR ÉVITER ANR (v8.5.0)
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            val sortedRoles = myRoles.values.toList().sortedBy { it.creatorName }
            sortedRoles.forEach { role ->
                RoleCard(
                    role = role,
                    accent = accent,
                    theme = theme,
                    onClick = {
                        onNavigateToCube(role.creatorId)
                    }
                )
            }
        }

        if (!isCreator) {
            Spacer(modifier = Modifier.height(32.dp))

            // Encouragement à devenir Créateur (v8.5.9)
            Card(
                onClick = onBecomeCreator,
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.05f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Et vous ?", style = MaterialTheme.typography.titleSmall, color = theme.contentColor, fontWeight = FontWeight.Bold)
                    Text(
                        "Vous aussi, commencez à sceller vos souvenirs pour ceux que vous aimez.",
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.contentColor.copy(alpha = 0.6f)
                    )
                    Text(
                        "Devenir Créateur →",
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = accent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

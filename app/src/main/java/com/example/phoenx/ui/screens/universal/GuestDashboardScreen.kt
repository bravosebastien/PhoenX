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
import androidx.compose.material.icons.filled.AutoAwesome
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
import com.example.phoenx.ui.components.PhoenXAvatar
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
    val hasSeenPrompt by mainViewModel.hasSeenBecomeCreatorPrompt.collectAsState()
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
                .padding(horizontal = 24.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Bienvenue dans votre espace dédié. Voici les personnes qui comptent sur vous.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.contentColor.copy(alpha = 0.7f),
                        lineHeight = 22.sp
                    )
                }

                if (isCreator == false) {
                    item {
                        BecomeCreatorCard(
                            theme = theme,
                            accent = accent,
                            onClick = { navController.navigate(Screen.SilenceOnboarding.route) }
                        )
                    }
                }

                if (pendingByEmail.isNotEmpty()) {
                    item {
                        Text(
                            "INVITATIONS EN ATTENTE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = accent,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    items(pendingByEmail) { invite ->
                        PendingInviteCard(invite, accent, theme) {
                            navController.navigate(Screen.UniversalJoin.createRoute(invite.id))
                        }
                    }
                }

                if (myRoles.isEmpty() && pendingByEmail.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Aucun rôle actif pour le moment.", color = theme.contentColor.copy(alpha = 0.4f), fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
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
                                        navController.navigate(Screen.WitnessResponse.createRoute(role.creatorId, role.sourceId ?: ""))
                                    }
                                    "recipient" -> navController.navigate(Screen.RecipientCube.createRoute(role.creatorId))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BecomeCreatorCard(
    theme: AppThemeState,
    accent: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.08f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, null, tint = accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text("Et vous ?", style = MaterialTheme.typography.titleSmall, color = theme.contentColor, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Commencez à sceller vos propres souvenirs pour ceux que vous aimez.",
                style = MaterialTheme.typography.bodySmall,
                color = theme.contentColor.copy(alpha = 0.7f)
            )
            Text(
                "Devenir Créateur →",
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                fontWeight = FontWeight.Bold
            )
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
            PhoenXAvatar(
                photoUrl = null, // Photo non dispo sur invitation email brute pour l'instant
                name = invite.creatorName,
                size = 48.dp,
                borderColor = accent.copy(alpha = 0.3f)
            )
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
        "recipient" -> Triple(Icons.Default.Person, "Je suis l'un de ses destinataires", accent)
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
            PhoenXAvatar(
                photoUrl = role.photoUrl,
                name = role.creatorName,
                size = 44.dp,
                borderColor = color.copy(alpha = 0.3f)
            )
            
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
            .padding(horizontal = 24.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Bienvenue dans votre espace dédié. Voici les personnes qui comptent sur vous.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = theme.contentColor.copy(alpha = 0.7f),
                    lineHeight = 22.sp
                )
            }

            if (!isCreator) {
                item {
                    BecomeCreatorCard(theme = theme, accent = accent, onClick = onBecomeCreator)
                }
            }

            if (pendingInvites.isNotEmpty()) {
                item {
                    Text(
                        "INVITATIONS EN ATTENTE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = accent,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                items(pendingInvites) { invite ->
                    PendingInviteCard(invite, accent, theme) {
                        onAcceptInvite(invite.id)
                    }
                }
            }

            val sortedRoles = myRoles.values.toList().sortedBy { it.creatorName }
            items(sortedRoles) { role ->
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
    }
}

package com.example.phoenx.ui.screens.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.ui.MainViewModel
import com.example.phoenx.ui.components.InfoButton
import com.example.phoenx.ui.components.PhoenXAvatar
import com.example.phoenx.ui.theme.AppThemeState
import com.example.phoenx.ui.theme.LocalAppTheme

@Composable
fun PerspectiveSwitcher(
    current: MainViewModel.Perspective,
    onSwitch: (MainViewModel.Perspective) -> Unit,
    accent: Color
) {
    TabRow(
        selectedTabIndex = current.ordinal,
        containerColor = Color.Transparent,
        contentColor = accent,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(tabPositions[current.ordinal]),
                color = accent
            )
        },
        divider = {}
    ) {
        Tab(
            selected = current == MainViewModel.Perspective.MY_MEMORY,
            onClick = { 
                android.util.Log.d("PerspectiveDebug", "HomeScreen: Tab MY_MEMORY clicked")
                onSwitch(MainViewModel.Perspective.MY_MEMORY) 
            },
            text = { Text("MA MÉMOIRE", style = MaterialTheme.typography.labelSmall) }
        )
        Tab(
            selected = current == MainViewModel.Perspective.HERITAGE,
            onClick = { 
                android.util.Log.d("PerspectiveDebug", "HomeScreen: Tab HERITAGE clicked")
                onSwitch(MainViewModel.Perspective.HERITAGE) 
            },
            text = { Text("PROCHES", style = MaterialTheme.typography.labelSmall) }
        )
    }
}

@Composable
fun HomeHeader(name: String, photoUrl: String?, date: String, onProfileClick: () -> Unit, theme: AppThemeState) {
    val accent = theme.accentColor
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Bonsoir, $name",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = theme.fontFamily, 
                    fontStyle = FontStyle.Italic, 
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = theme.contentColor
            )
            Text(
                text = date, 
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), 
                color = theme.contentColor.copy(alpha = 0.5f)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            InfoButton(
                title = "Votre Centre de Pilotage",
                points = listOf(
                    "VOTRE LÉGENDE : C'est ici que vous gérez votre héritage émotionnel et numérique.",
                    "LE SILENCE : Votre état de présence est surveillé pour garantir que vos secrets ne seront libérés qu'au bon moment.",
                    "LA BIBLIOTHÈQUE : Accédez à tous vos compartiments (Photos, Vidéos, Secrets, Quiz).",
                    "CERCLE DE CONFIANCE : Gérez qui sont vos destinataires et vos témoins."
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            PhoenXAvatar(
                photoUrl = photoUrl,
                name = name,
                size = 34.dp,
                modifier = Modifier.clickable { onProfileClick() },
                borderColor = accent.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun HomeNavigationBar(
    onNavigateToHome: () -> Unit,
    onNavigateToTrustCircle: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val theme = LocalAppTheme.current
    Surface(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
        color = theme.backgroundColor.copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NavItem(Icons.Outlined.Home, "Accueil", true, onNavigateToHome)
            NavItem(Icons.Outlined.People, "Mon Cercle", false, onNavigateToTrustCircle)
            NavItem(Icons.Outlined.AutoStories, "Bibliothèque", false, onNavigateToLibrary)
            NavItem(Icons.Outlined.AccountCircle, "Profil", false, onOpenProfile)
        }
    }
}

@Composable
fun NavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    Column(
        modifier = Modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = if (active) accent else theme.contentColor.copy(alpha = 0.3f), modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = if (active) accent else theme.contentColor.copy(alpha = 0.3f))
    }
}

package com.example.phoenx.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ProfileDrawer(
    userName: String,
    userEmail: String,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToTransmission: () -> Unit,
    drawerState: DrawerState,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val theme = LocalAppTheme.current
    val accent = theme.accentColor

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = theme.backgroundColor,
                drawerShape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .border(androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f)))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 48.dp, start = 24.dp, end = 24.dp, bottom = 24.dp)
                ) {
                    // EN-TÊTE - CLIQUE SUR L'IDENTITÉ POUR LE PROFIL
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                scope.launch { drawerState.close() }
                                onNavigateToProfile() 
                            }
                    ) {
                        Surface(
                            modifier = Modifier.size(60.dp),
                            shape = CircleShape,
                            color = accent.copy(alpha = 0.1f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.3f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Person, null, tint = accent, modifier = Modifier.size(32.dp))
                            }
                        }

                        Text(
                            text = userName,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = theme.fontFamily,
                                fontStyle = FontStyle.Italic,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = theme.contentColor,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = theme.contentColor.copy(alpha = 0.4f)
                        )
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(theme.contentColor.copy(alpha = 0.1f)))
                    Spacer(modifier = Modifier.height(32.dp))

                    // MENU SIMPLIFIÉ
                    DrawerItem(icon = Icons.Outlined.AccountCircle, text = "Mon Profil & Style", theme = theme) { 
                        scope.launch { drawerState.close() }
                        onNavigateToProfile() 
                    }

                    DrawerItem(icon = Icons.Outlined.People, text = "Mon Cercle de Confiance", theme = theme) { 
                        scope.launch { drawerState.close() }
                        onNavigateToTransmission() 
                    }
                    
                    DrawerItem(icon = Icons.Outlined.Info, text = "Aide & Guide", theme = theme) { 
                        // Futur guide
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // SÉCURITÉ
                    DrawerItem(icon = Icons.Outlined.Logout, text = "Se déconnecter", textColor = Error, theme = theme) {
                        scope.launch { drawerState.close() }
                        onLogout()
                    }
                    
                    Text(
                        text = "PHOEN-X v8.9.7",
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.contentColor.copy(alpha = 0.2f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        },
        content = content
    )
}

@Composable
fun DrawerItem(
    icon: ImageVector,
    text: String,
    theme: AppThemeState,
    textColor: Color = theme.contentColor,
    onClick: () -> Unit
) {
    val accent = theme.accentColor
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = textColor, modifier = Modifier.weight(1f))
        Icon(Icons.Outlined.ChevronRight, null, tint = theme.contentColor.copy(alpha = 0.2f), modifier = Modifier.size(16.dp))
    }
}

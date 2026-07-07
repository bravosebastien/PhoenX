package com.example.phoenx.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ProfileDrawer(
    userName: String,
    userEmail: String,
    isBiometricEnabled: Boolean,
    onToggleBiometric: (Boolean) -> Unit,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    onResetVideo: () -> Unit,
    onUpdateRhythm: () -> Unit,
    drawerState: DrawerState,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = BackgroundPrimary,
                drawerShape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
                modifier = Modifier
                    .width(280.dp)
                    .fillMaxHeight()
                    .border(androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E2E35)))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 32.dp, start = 20.dp, end = 20.dp, bottom = 20.dp)
                ) {
                    // EN-TÊTE
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = CircleShape,
                        color = AccentPrimary.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(2.dp, AccentPrimary.copy(alpha = 0.4f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Person, null, tint = AccentPrimary, modifier = Modifier.size(28.dp))
                        }
                    }

                    Text(
                        text = userName,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = FontFamily.Serif,
                            fontStyle = FontStyle.Italic,
                            fontSize = 18.sp
                        ),
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        text = userEmail,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextTertiary
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AccentPrimary.copy(alpha = 0.25f)))
                    Spacer(modifier = Modifier.height(24.dp))

                    // ITEMS - MON COMPTE
                    Text("MON COMPTE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = TextTertiary)
                    Spacer(modifier = Modifier.height(8.dp))

                    DrawerItem(icon = Icons.Outlined.Schedule, text = "Fréquence de présence") { 
                        scope.launch { drawerState.close() }
                        onUpdateRhythm() 
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Outlined.Fingerprint, null, tint = AccentPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(14.dp))
                            Text("Empreinte Digitale", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        }
                        Switch(
                            checked = isBiometricEnabled,
                            onCheckedChange = onToggleBiometric,
                            colors = SwitchDefaults.colors(checkedThumbColor = AccentPrimary)
                        )
                    }

                    DrawerItem(icon = Icons.Outlined.Notifications, text = "Contacts à prévenir") {
                        scope.launch { drawerState.close() }
                        onNavigate("notification_contacts")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF2E2E35)))
                    Spacer(modifier = Modifier.height(16.dp))

                    // ITEMS - APPLICATION
                    Text("APPLICATION", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = TextTertiary)
                    Spacer(modifier = Modifier.height(8.dp))

                    DrawerItem(icon = Icons.Outlined.VolumeUp, text = "Mode Vocal Total") {
                        scope.launch { drawerState.close() }
                        onNavigate("settings/accessibility")
                    }
                    DrawerItem(icon = Icons.Outlined.PlayCircleOutline, text = "Réafficher la vidéo") {
                        scope.launch { drawerState.close() }
                        onResetVideo()
                        Toast.makeText(context, "Vidéo réactivée", Toast.LENGTH_SHORT).show()
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF2E2E35)))
                    Spacer(modifier = Modifier.weight(1f))

                    // ITEMS - SÉCURITÉ
                    DrawerItem(icon = Icons.Outlined.Logout, text = "Se déconnecter", textColor = Error) {
                        scope.launch { drawerState.close() }
                        onLogout()
                    }
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
    textColor: Color = TextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = AccentPrimary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = textColor, modifier = Modifier.weight(1f))
        Icon(Icons.Outlined.ChevronRight, null, tint = TextTertiary, modifier = Modifier.size(16.dp))
    }
}

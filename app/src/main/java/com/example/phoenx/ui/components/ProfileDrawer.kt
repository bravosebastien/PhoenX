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
    isBiometricEnabled: Boolean,
    onToggleBiometric: (Boolean) -> Unit,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    onResetVideo: () -> Unit,
    onUpdateRhythm: () -> Unit,
    onNavigateToSettings: () -> Unit,
    mainViewModel: com.example.phoenx.ui.MainViewModel,
    themeViewModel: com.example.phoenx.ui.theme.ThemeViewModel = hiltViewModel(),
    drawerState: DrawerState,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val accent by themeViewModel.accentColor.collectAsState()
    val backgroundColor by themeViewModel.backgroundColor.collectAsState()
    val backgroundStyle by themeViewModel.backgroundStyle.collectAsState()

    val colors = listOf(
        Color(0xFFC97B3A), Color(0xFFFFD700), Color(0xFFFFBF00), Color(0xFFFF9800),
        Color(0xFFFF4500), Color(0xFFFF4E11), Color(0xFFF44336), Color(0xFFE91E63),
        Color(0xFFFF00FF), Color(0xFF9C27B0), Color(0xFF673AB7), Color(0xFF3F51B5),
        Color(0xFF2196F3), Color(0xFF03A9F4), Color(0xFF00FFFF), Color(0xFF00BCD4),
        Color(0xFF009688), Color(0xFF2ECC71), Color(0xFF4CAF50), Color(0xFF8BC34A),
        Color(0xFFC0C0C0), Color(0xFF607D8B)
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = BackgroundPrimary,
                drawerShape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .border(androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E2E35)))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 32.dp, start = 20.dp, end = 20.dp, bottom = 20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // EN-TÊTE
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = CircleShape,
                        color = accent.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(2.dp, accent.copy(alpha = 0.4f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Person, null, tint = accent, modifier = Modifier.size(28.dp))
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
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(accent.copy(alpha = 0.25f)))
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
                            Icon(Icons.Outlined.Fingerprint, null, tint = accent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(14.dp))
                            Text("Empreinte Digitale", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        }
                        Switch(
                            checked = isBiometricEnabled,
                            onCheckedChange = onToggleBiometric,
                            colors = SwitchDefaults.colors(checkedThumbColor = accent)
                        )
                    }

                    DrawerItem(icon = Icons.Outlined.Notifications, text = "Contacts à prévenir") {
                        scope.launch { drawerState.close() }
                        onNavigate("notification_contacts")
                    }

                    DrawerItem(icon = Icons.Outlined.Settings, text = "Tous les réglages") {
                        scope.launch { drawerState.close() }
                        onNavigateToSettings()
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF2E2E35)))
                    Spacer(modifier = Modifier.height(16.dp))

                    // PERSONNALISATION
                    Text("APPARENCE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = TextTertiary)
                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. COULEUR DE FOND (Gradients)
                    Text("COULEUR DE FOND", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    colors.chunked(5).forEach { rowColors ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowColors.forEach { color ->
                                val isSelected = color == backgroundColor
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(color, CircleShape)
                                        .border(
                                            width = if (isSelected) 2.dp else 0.dp,
                                            color = if (isSelected) TextPrimary else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { themeViewModel.setBackground(color) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 2. COULEUR D'ÉCRITURE / ACCENT
                    Text("COULEUR D'ÉCRITURE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    colors.chunked(5).forEach { rowColors ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowColors.forEach { color ->
                                val isSelected = color == accent
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(color, CircleShape)
                                        .border(
                                            width = if (isSelected) 2.dp else 0.dp,
                                            color = if (isSelected) TextPrimary else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { themeViewModel.setAccent(color) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("STYLE DE FOND", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("RADIAL", "LINEAR", "SOLID").forEach { style ->
                            val isSelected = backgroundStyle == style
                            val label = when(style) {
                                "RADIAL" -> "Profondeur (Radial)"
                                "LINEAR" -> "Élégance (Linéaire)"
                                "SOLID" -> "Sobre (Uni)"
                                else -> style
                            }
                            
                            Surface(
                                onClick = { themeViewModel.setBackgroundStyle(style) },
                                color = if (isSelected) accent.copy(alpha = 0.15f) else SurfaceCard.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(10.dp),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, accent) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = null,
                                        colors = RadioButtonDefaults.colors(selectedColor = accent)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isSelected) TextPrimary else TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
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

                    Spacer(modifier = Modifier.height(24.dp))

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
    val accent = LocalAccentColor.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = textColor, modifier = Modifier.weight(1f))
        Icon(Icons.Outlined.ChevronRight, null, tint = TextTertiary, modifier = Modifier.size(16.dp))
    }
}

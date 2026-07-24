package com.example.phoenx.ui.screens.library.archive

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.hapticfeedback.*
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.phoenx.ui.theme.*
import com.example.phoenx.ui.screens.library.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OldWoodenLibraryScreen(
    navController: NavController,
    creatorId: String,
    recipientId: String? = null,
    viewerMode: ViewerMode = ViewerMode.RECIPIENT_FULL,
    viewModel: OldLibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        viewModel.initialize(creatorId, recipientId, viewerMode)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D10))
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                color = AccentPrimary,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                if (viewerMode == ViewerMode.CREATOR_PREVIEW) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(accent.copy(alpha = 0.15f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "👁 Aperçu — Ce que verront tes proches",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = accent,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { navController.popBackStack() }) {
                            Text("← Mon espace", color = accent, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color(0xFF1A1A1F), Color.Transparent)))
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (viewerMode == ViewerMode.CREATOR_PREVIEW) "Ta Bibliothèque" else "La bibliothèque de ${uiState.creatorName}",
                            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold),
                            color = theme.contentColor,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { navController.navigate("questions_room") }) {
                            Icon(Icons.Default.Search, null, tint = accent)
                        }
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(span = { GridItemSpan(2) }) {
                        LibraryStructureHeader()
                    }

                    items(uiState.compartments) { compartment ->
                        CompartmentCell(
                            compartment = compartment,
                            glowIntensity = uiState.glowIntensity,
                            theme = theme
                        ) {
                            if (compartment.access == CompartmentAccess.OPEN) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                navController.navigate(compartment.route)
                            } else {
                                viewModel.onCompartmentSelected(compartment)
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = uiState.selectedCompartment != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            uiState.selectedCompartment?.let {
                LockedCompartmentPanel(it, theme) { viewModel.onDismissCompartment() }
            }
        }
    }
}

@Composable
fun LibraryStructureHeader() {
    Canvas(modifier = Modifier.fillMaxWidth().height(32.dp)) {
        drawRect(brush = Brush.horizontalGradient(listOf(Color(0xFF1C1410), Color(0xFF2A2018), Color(0xFF1C1410))))
        drawLine(
            brush = Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFFC97B3A), Color(0xFFE8A85F), Color(0xFFC97B3A), Color.Transparent)),
            start = Offset(0f, size.height * 0.6f),
            end = Offset(size.width, size.height * 0.6f),
            strokeWidth = 2f
        )
    }
}

@Composable
fun CompartmentCell(compartment: LibraryCompartment, glowIntensity: Float, theme: AppThemeState, onClick: () -> Unit) {
    val accent = theme.accentColor
    val isLocked = compartment.access != CompartmentAccess.OPEN
    Box(
        modifier = Modifier
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(brush = Brush.radialGradient(listOf(Color(0xFF1E1A14), Color(0xFF151210))))
            
            // Bords bois
            drawRect(Color(0xFF2A1F10), topLeft = Offset(0f,0f), size = Size(w, h*0.06f))
            drawRect(Color(0xFF1C1410), topLeft = Offset(0f, h*0.94f), size = Size(w, h*0.06f))
            drawRect(Color(0xFF241A0E), size = Size(w*0.05f, h))
            drawRect(Color(0xFF1C1410), topLeft = Offset(w*0.95f, 0f), size = Size(w*0.05f, h))

            // Contenu
            translate(left = (w * 0.07f), top = (h * 0.08f)) {
                clipRect(right = (w * 0.86f), bottom = (h * 0.75f)) {
                    drawCompartmentContent(compartment, glowIntensity, theme)
                }
            }

            if (isLocked) {
                drawRect(color = Color.Black.copy(alpha = 0.7f))
                drawLockIcon(compartment.access)
            }
        }

        Column(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))).padding(8.dp)) {
            Text(compartment.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = if (isLocked) theme.contentColor.copy(alpha = 0.4f) else theme.contentColor)
            Text(if (isLocked) "Contenu Scellé" else compartment.subtitle, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent)
        }
    }
}

fun DrawScope.drawLockIcon(access: CompartmentAccess) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val s = size.width * 0.15f
    
    val lockColor = when(access) {
        CompartmentAccess.LOCKED_DATE -> Color(0xFFE8A85F)
        else -> Color(0xFFC97B3A)
    }

    drawRoundRect(
        color = lockColor, 
        topLeft = Offset(cx - s, cy - (s * 0.5f)), 
        size = Size(s * 2, s * 1.6f), 
        cornerRadius = CornerRadius(4f)
    )
}

@Composable
fun LockedCompartmentPanel(compartment: LibraryCompartment, theme: AppThemeState, onDismiss: () -> Unit) {
    val accent = theme.accentColor
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp), 
        colors = CardDefaults.cardColors(containerColor = theme.backgroundColor),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Compartiment Scellé", style = MaterialTheme.typography.headlineSmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold), color = theme.contentColor)
            Spacer(modifier = Modifier.height(8.dp))
            val message = when(compartment.access) {
                CompartmentAccess.LOCKED_DATE -> "Ce tiroir s'ouvrira à une date choisie par le Créateur."
                CompartmentAccess.LOCKED_ENIGMA -> "Vous devez résoudre une énigme pour ouvrir ce secret."
                CompartmentAccess.LOCKED_KEY -> "Une clé physique est nécessaire pour déverrouiller ce tiroir."
                else -> "Ce compartiment n'est pas encore accessible."
            }
            Text(message, color = theme.contentColor.copy(alpha = 0.7f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = accent), modifier = Modifier.phoenXMatiere()) {
                Text("Fermer", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}

package com.example.phoenx.ui.screens.book

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.ui.theme.AppThemeState
import com.example.phoenx.ui.theme.LocalAppTheme
import com.example.phoenx.ui.theme.phoenXMatiere

@Composable
fun EmptyBookState(onGenerate: () -> Unit) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(modifier = Modifier.size(80.dp)) {
            val w = size.width
            val h = size.height
            val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            drawRect(
                color = accent,
                topLeft = Offset(w * 0.1f, h * 0.1f),
                size = Size(w * 0.38f, h * 0.8f),
                style = stroke
            )
            drawRect(
                color = accent,
                topLeft = Offset(w * 0.52f, h * 0.1f),
                size = Size(w * 0.38f, h * 0.8f),
                style = stroke
            )
            drawLine(
                color = accent,
                start = Offset(w * 0.5f, h * 0.1f),
                end = Offset(w * 0.5f, h * 0.9f),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            for (i in 1..3) {
                val y = h * (0.25f + i * 0.12f)
                drawLine(
                    color = accent.copy(alpha = 0.4f),
                    start = Offset(w * 0.16f, y),
                    end = Offset(w * 0.44f, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
            for (i in 1..3) {
                val y = h * (0.25f + i * 0.12f)
                drawLine(
                    color = accent.copy(alpha = 0.4f),
                    start = Offset(w * 0.56f, y),
                    end = Offset(w * 0.84f, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Ton histoire mérite d'être racontée.",
            style = TextStyle(
                fontFamily = theme.fontFamily,
                fontSize = 20.sp,
                color = theme.contentColor,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "PHOEN-X va analyser tes souvenirs et rédiger\n" +
                   "le premier chapitre de ton livre de vie.\n" +
                   "Tu pourras tout relire, modifier et valider.",
            style = TextStyle(
                fontSize = 14.sp,
                color = theme.contentColor.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            ),
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = onGenerate,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accent,
                contentColor = theme.backgroundColor
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Générer mon livre",
                style = TextStyle(
                    fontFamily = theme.fontFamily,
                    fontSize = 16.sp,
                    color = theme.backgroundColor
                )
            )
        }
    }
}

@Composable
fun GeneratingBookState(progress: String) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )

        Canvas(
            modifier = Modifier
                .size(80.dp)
                .scale(scale)
        ) {
            drawCircle(
                color = accent.copy(alpha = alpha * 0.2f),
                radius = size.width / 2f
            )
            drawCircle(
                color = accent.copy(alpha = alpha),
                radius = size.width / 3f,
                style = Stroke(width = 2.dp.toPx())
            )
            drawLine(
                color = accent.copy(alpha = alpha),
                start = Offset(size.width / 2f, size.height * 0.25f),
                end = Offset(size.width / 2f, size.height * 0.75f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        val textAlpha by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "textAlpha"
        )

        Text(
            text = progress,
            style = TextStyle(
                fontFamily = theme.fontFamily,
                fontSize = 16.sp,
                fontStyle = FontStyle.Italic,
                color = theme.contentColor.copy(alpha = textAlpha),
                textAlign = TextAlign.Center
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Cela peut prendre quelques instants...",
            style = TextStyle(
                fontSize = 12.sp,
                color = theme.contentColor.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
        )
    }
}

@Composable
fun BookAiExplanationDialog(onDismiss: () -> Unit) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.backgroundColor,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Psychology, null, tint = accent, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(16.dp))
                Text(
                    "Retouches vs Réécriture", 
                    color = theme.contentColor, 
                    fontFamily = theme.fontFamily,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    fontSize = 20.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.EditNote, null, tint = accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Modifier un chapitre", style = MaterialTheme.typography.bodyMedium, color = theme.contentColor, fontWeight = FontWeight.Bold)
                        Text("L'action 'Demander à l'IA' à l'intérieur d'un chapitre ne modifie QUE ce chapitre. C'est idéal pour corriger un détail sans toucher au reste.", style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.7f))
                    }
                }
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.AutoFixHigh, null, tint = accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Régénérer le livre", style = MaterialTheme.typography.bodyMedium, color = theme.contentColor, fontWeight = FontWeight.Bold)
                        Text("Le bouton 'Régénérer' (en haut) relance l'écriture de TOUS les chapitres. Utilisez-le uniquement si vous voulez un manuscrit totalement nouveau.", style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.7f))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                modifier = Modifier.fillMaxWidth().phoenXMatiere(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("C'est très clair", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun BookOnboardingDialog(onDismiss: () -> Unit) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.backgroundColor,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    Icons.Default.AutoStories, 
                    null, 
                    tint = accent, 
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Comment l'IA écrit votre vie", 
                    color = theme.contentColor, 
                    fontFamily = theme.fontFamily,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    fontSize = 20.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OnboardingPoint(
                    icon = Icons.Default.Timeline,
                    title = "Respect de votre chronologie",
                    description = "Chaque souvenir est trié par âge. L'IA utilise ces repères pour tisser un récit fluide, du premier chapitre jusqu'au dernier.",
                    theme = theme
                )
                OnboardingPoint(
                    icon = Icons.Default.People,
                    title = "Reconnaissance des personnages",
                    description = "L'IA identifie vos proches (ex: 'Julie', 'mon fils Marc'). Elle fait le lien entre vos différents souvenirs pour raconter l'évolution de vos relations.",
                    theme = theme
                )
                OnboardingPoint(
                    icon = Icons.Default.HistoryEdu,
                    title = "Une narration personnalisée",
                    description = "Ce n'est pas une simple liste. L'IA rédige à la première personne ('Je') et adapte le ton (joie, nostalgie) selon l'émotion de vos souvenirs.",
                    theme = theme
                )
                
                Surface(
                    color = accent.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "💡 Conseil du Biographe",
                            style = MaterialTheme.typography.labelMedium,
                            color = accent,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Plus vos souvenirs sont détaillés (prénoms, lieux, sentiments), plus le récit de l'IA sera précis et fidèle à votre réalité.",
                            style = MaterialTheme.typography.bodySmall,
                            color = theme.contentColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                modifier = Modifier.fillMaxWidth().phoenXMatiere(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("J'ai compris", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun OnboardingPoint(
    icon: ImageVector,
    title: String,
    description: String,
    theme: AppThemeState
) {
    val accent = theme.accentColor
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp).padding(top = 2.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = theme.contentColor, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = theme.contentColor.copy(alpha = 0.7f), lineHeight = 18.sp)
        }
    }
}

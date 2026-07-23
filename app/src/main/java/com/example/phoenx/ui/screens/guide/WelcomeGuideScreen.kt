package com.example.phoenx.ui.screens.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.ui.theme.*

data class GuideStep(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color = AccentPrimary,
)

@Composable
fun WelcomeGuideScreen(
    onDismiss: (Boolean) -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val steps = listOf(
        GuideStep(
            "Bienvenue dans PHOEN-X",
            "Votre espace intime pour capturer et transmettre ce qui compte vraiment. Apprenons ensemble comment ça fonctionne.",
            Icons.Default.AutoAwesome
        ),
        GuideStep(
            "Capturer vos souvenirs",
            "Texte, voix ou photo. Tout est chiffré immédiatement. L'IA range tout seule vos souvenirs dans les bons tiroirs émotionnels.",
            Icons.Default.AddCircleOutline
        ),
        GuideStep(
            "Le Fil de Pensée",
            "C'est votre signature. Chaque pensée est marquée par votre âge exact. Vos proches verront votre évolution à travers le temps.",
            Icons.Default.Timeline
        ),
        GuideStep(
            "Transmettre à vos proches",
            "Choisissez qui recevra vos messages. Vous pouvez même programmer des envois pour le futur (ex: les 18 ans d'un enfant).",
            Icons.AutoMirrored.Filled.Send
        ),
        GuideStep(
            "Sécurité Totale",
            "Tes souvenirs sont protégés par un chiffrement de pointe (E2EE). Ils ne pourront être ouverts que par toi et tes proches désignés.",
            Icons.Default.Lock
        )
    )

    val pagerState = rememberPagerState { steps.size }
    var neverShowAgain by remember { mutableStateOf(value = false) }

    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            GuideStepContent(steps[page], theme)
        }

        // Contrôles en bas
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Indicateurs de pages
            Row(
                modifier = Modifier.padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(steps.size) { i ->
                    val color = if (pagerState.currentPage == i) accent else theme.contentColor.copy(alpha = 0.2f)
                    Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
                }
            }

            if (pagerState.currentPage == (steps.size - 1)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Checkbox(
                        checked = neverShowAgain,
                        onCheckedChange = { neverShowAgain = it },
                        colors = CheckboxDefaults.colors(checkedColor = accent)
                    )
                    Text("Ne plus afficher ce guide", color = theme.contentColor.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                
                Button(
                    onClick = { onDismiss(neverShowAgain) },
                    modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Text("Commencer l'aventure", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    "Faites glisser pour continuer →",
                    color = theme.contentColor.copy(alpha = 0.4f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(56.dp)) // Espace du bouton absent
            }
        }
    }
}

@Composable
fun GuideStepContent(step: GuideStep, theme: AppThemeState) {
    val accent = theme.accentColor
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = step.color.copy(alpha = 0.1f),
            border = androidx.compose.foundation.BorderStroke(1.dp, step.color.copy(alpha = 0.2f))
        ) {
            Icon(
                imageVector = step.icon,
                contentDescription = null,
                tint = step.color,
                modifier = Modifier.padding(32.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = step.title,
            style = MaterialTheme.typography.displaySmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold),
            color = theme.contentColor,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = step.description,
            style = MaterialTheme.typography.bodyLarge,
            color = theme.contentColor.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 28.sp
        )
    }
}

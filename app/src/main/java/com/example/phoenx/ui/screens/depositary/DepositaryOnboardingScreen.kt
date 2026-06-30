package com.example.phoenx.ui.screens.depositary

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun DepositaryOnboardingScreen(
    creatorName: String,
    onFinish: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 5 })
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = BackgroundPrimary,
        bottomBar = {
            OnboardingBottomBar(
                currentPage = pagerState.currentPage,
                onNext = {
                    if (pagerState.currentPage < 4) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        onFinish()
                    }
                },
                onPrevious = {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                }
            )
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { page ->
            when (page) {
                0 -> OnboardingStep(
                    icon = Icons.Default.Timer,
                    title = "Aujourd'hui : rien.",
                    text = "Tu n'as aucune action à faire maintenant. Aucun document à signer, aucune démarche à entreprendre. Ton rôle ne s'active que dans une seule situation : si $creatorName devenait silencieux pendant une longue période."
                )
                1 -> OnboardingStepWithPulse(
                    title = "Comment ça fonctionne",
                    text = "$creatorName confirme régulièrement sa présence dans l'app — un simple tap, toutes les quelques semaines. Si cette confirmation manque plusieurs fois de suite, l'application t'enverra une notification. D'abord pour t'informer. Puis, si le silence se prolonge, pour te demander d'agir."
                )
                2 -> OnboardingStepAlerts(
                    title = "Les alertes que tu pourrais recevoir",
                    text = "Il existe 3 niveaux de vigilance :"
                )
                3 -> OnboardingStep(
                    icon = Icons.Default.Favorite,
                    title = "Si tu reçois une alerte ACTION DEMANDÉE",
                    text = "Essaie simplement de joindre $creatorName — un appel, un message, une visite si possible. Si tu n'y arrives pas, contacte son entourage proche. Une fois que tu as une réponse claire (il/elle va bien, ou non), ouvre l'app pour nous le dire. Tu n'es jamais seul(e) dans cette démarche — l'app te guide pas à pas à ce moment-là."
                )
                4 -> OnboardingStep(
                    icon = Icons.Default.HourglassEmpty,
                    title = "Et si le pire est confirmé ?",
                    text = "Si tu confirmes que $creatorName n'est plus là, un protocole sécurisé se déclenche : tu décris ce que tu as constaté, tu confirmes en conscience, puis un délai de 72 heures s'écoule avant que les proches de $creatorName soient prévenus. Ce délai protège tout le monde — y compris toi."
                )
            }
        }
    }
}

@Composable
fun OnboardingStep(
    icon: ImageVector,
    title: String,
    text: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AccentPrimary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = FontFamily.Serif,
                color = TextPrimary
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}

@Composable
fun OnboardingStepWithPulse(
    title: String,
    text: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .scale(pulseScale)
                .background(AccentPrimary, shape = CircleShape)
        )
        Spacer(modifier = Modifier.height(56.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = FontFamily.Serif,
                color = TextPrimary
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}

@Composable
fun OnboardingStepAlerts(
    title: String,
    text: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Default.Email, null, tint = AccentPrimary, modifier = Modifier.size(48.dp))
            Icon(Icons.Default.Sms, null, tint = AccentPrimary, modifier = Modifier.size(48.dp))
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = FontFamily.Serif,
                color = TextPrimary
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        AlertLevelItem(Color(0xFFFFB74D), "INFORMATION", "email", "Pas d'inquiétude, juste pour info")
        Spacer(modifier = Modifier.height(16.dp))
        AlertLevelItem(Color(0xFFC97B3A), "ACTION DEMANDÉE", "email + SMS", "Essaie de le/la contacter")
        Spacer(modifier = Modifier.height(16.dp))
        AlertLevelItem(Color(0xFFE57373), "URGENCE", "email + SMS", "Secours notifié si pas de réponse sous 7j")
    }
}

@Composable
fun AlertLevelItem(color: Color, level: String, medium: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(40.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = "$level ($medium)",
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun OnboardingBottomBar(
    currentPage: Int,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (currentPage > 0) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextSecondary)
            }
        } else {
            Spacer(modifier = Modifier.size(48.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(5) { i ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (i == currentPage) AccentPrimary else SurfaceCard)
                )
            }
        }

        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = if (currentPage == 4) AccentPrimary else Color.Transparent),
            elevation = null
        ) {
            if (currentPage == 4) {
                Text("Je suis prêt(e)", color = BackgroundPrimary)
            } else {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = AccentPrimary)
            }
        }
    }
}

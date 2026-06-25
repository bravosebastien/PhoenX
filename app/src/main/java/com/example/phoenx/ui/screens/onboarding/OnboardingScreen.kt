package com.example.phoenx.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: (Boolean) -> Unit // true for signup, false for login
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPrimary)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = true
        ) { page ->
            when (page) {
                0 -> StepFounderStory(onNext = { 
                    scope.launch { pagerState.animateScrollToPage(1) }
                })
                1 -> StepPromises(onNext = {
                    scope.launch { pagerState.animateScrollToPage(2) }
                })
                2 -> StepGetStarted(onFinish)
            }
        }

        // Indicateurs de page
        Row(
            modifier = Modifier
                .height(80.dp)
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { iteration ->
                val color = if (pagerState.currentPage == iteration) AccentPrimary else TextTertiary
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .background(color, CircleShape)
                        .size(8.dp)
                )
            }
        }
    }
}

@Composable
fun StepFounderStory(onNext: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(800))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier.size(120.dp),
                        color = Color.Transparent
                    ) {
                        Text(text = "🦋", fontSize = 80.sp)
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "J'ai survécu trois fois.",
                        style = MaterialTheme.typography.displayLarge,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Chaque retour m'a appris que l'urgence n'était pas médicale.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "C'était ce que je n'avais jamais encore transmis.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = AccentPrimary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        OutlinedButton(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, AccentPrimary),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentPrimary)
        ) {
            Text("Continuer")
        }
        
        Spacer(modifier = Modifier.height(56.dp)) // Espace pour les indicateurs
    }
}

@Composable
fun StepPromises(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PromiseCard(
                icon = "❤",
                title = "Pour toi, maintenant",
                description = "Un espace intime pour capturer ce qui compte.",
                delay = 0
            )
            Spacer(modifier = Modifier.height(16.dp))
            PromiseCard(
                icon = "🦋",
                title = "Pour eux, plus tard",
                description = "Une transmission réfléchie, au bon moment.",
                delay = 300
            )
            Spacer(modifier = Modifier.height(16.dp))
            PromiseCard(
                icon = "✦",
                title = "Avec dignité, toujours",
                description = "Ton histoire, racontée comme tu l'as voulue.",
                delay = 600
            )
        }

        OutlinedButton(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, AccentPrimary),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentPrimary)
        ) {
            Text("Continuer")
        }
        
        Spacer(modifier = Modifier.height(56.dp)) // Espace pour les indicateurs
    }
}

@Composable
fun PromiseCard(icon: String, title: String, description: String, delay: Int) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { 
        kotlinx.coroutines.delay(delay.toLong())
        visible = true 
    }

    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { 40 }
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SurfaceCard,
            shape = MaterialTheme.shapes.large
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = icon, fontSize = 24.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = title, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                    Text(text = description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
fun StepGetStarted(onFinish: (Boolean) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Comment veux-tu commencer ?",
            style = MaterialTheme.typography.displayMedium,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = { onFinish(true) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Créer mon espace", color = BackgroundPrimary)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { onFinish(false) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.medium,
            border = androidx.compose.foundation.BorderStroke(1.dp, TextSecondary)
        ) {
            Text("J'ai déjà un compte", color = TextPrimary)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Chiffrement de bout en bout.\nTes données ne nous appartiennent pas.",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
        
        Spacer(modifier = Modifier.height(56.dp)) // Espace pour les indicateurs
    }
}

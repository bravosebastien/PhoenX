package com.example.phoenx.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import com.example.phoenx.R
import com.example.phoenx.ui.theme.AccentPrimary
import com.example.phoenx.ui.theme.BackgroundPrimary
import com.example.phoenx.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

private data class Spark(
    val angle: Float,
    val distance: Float,
    val size: Float,
    val arrivalDelay: Long  // délai avant que CE point apparaisse
)

// 7 étincelles — arrivalDelay espacés de 120ms
// Elles arrivent rapidement l'une après l'autre (toutes en ~720ms)
// puis restent toutes visibles ensemble avant de s'éteindre lentement
private val sparks = listOf(
    Spark(angle = -2.0f, distance = 62f, size = 4f,   arrivalDelay = 0),
    Spark(angle =  3.0f, distance = 78f, size = 3f,   arrivalDelay = 120),
    Spark(angle = -0.3f, distance = 75f, size = 2.5f, arrivalDelay = 240),
    Spark(angle =  1.4f, distance = 72f, size = 3f,   arrivalDelay = 360),
    Spark(angle = -1.1f, distance = 70f, size = 3f,   arrivalDelay = 480),
    Spark(angle =  2.2f, distance = 65f, size = 2f,   arrivalDelay = 600),
    Spark(angle =  0.6f, distance = 68f, size = 2.5f, arrivalDelay = 720)
)

// État global de disparition — toutes s'éteignent ensemble
// 0f = visibles, 1f = éteintes
@Composable
fun SplashScreen(
    onAnimationFinished: () -> Unit
) {
    val logoScale      = remember { Animatable(0.4f) }
    val logoAlpha      = remember { Animatable(0f) }
    val logoOffsetY    = remember { Animatable(36f) }
    val nameAlpha      = remember { Animatable(0f) }
    val nameOffsetY    = remember { Animatable(16f) }
    val lineWidth      = remember { Animatable(0f) }
    val phraseAlpha    = remember { Animatable(0f) }
    val phraseOffY     = remember { Animatable(14f) }

    // Chaque étincelle : progress 0→1 = voyage vers l'extérieur
    val sparkArrival   = sparks.map { remember { Animatable(0f) } }
    // Fondu global de toutes les étincelles ensemble
    val sparksFade     = remember { Animatable(1f) }

    // Halo — très lent, 7 secondes par cycle
    val haloScale by rememberInfiniteTransition(label = "halo").animateFloat(
        initialValue  = 0.5f,
        targetValue   = 1.4f,
        animationSpec = infiniteRepeatable(
            animation  = tween(7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloScale"
    )
    val haloAlpha by rememberInfiniteTransition(label = "haloA").animateFloat(
        initialValue  = 0.03f,
        targetValue   = 0.22f,
        animationSpec = infiniteRepeatable(
            animation  = tween(7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloAlpha"
    )

    LaunchedEffect(Unit) {

        // ── Phase 1 : Halo seul (0 → 1600ms) ──────────────────────
        // Rien ne se passe, le halo pulse doucement
        delay(1600)

        // ── Phase 2 : Logo monte lentement (1600ms → 3600ms) ───────
        launch {
            logoScale.animateTo(1.08f, tween(1800, easing = EaseOutBack))
            logoScale.animateTo(1.0f,  tween(400,  easing = LinearEasing))
        }
        launch {
            logoAlpha.animateTo(1.0f, tween(2000, easing = FastOutSlowInEasing))
        }
        launch {
            logoOffsetY.animateTo(0f, tween(2000, easing = EaseOutCubic))
        }

        delay(1400)

        // ── Phase 3 : Étincelles arrivent une par une (3000ms → 3720ms)
        // Chaque point part 120ms après le précédent — rapide
        // Mais chaque voyage individuel est lent (1400ms)
        sparks.forEachIndexed { index, spark ->
            launch {
                delay(spark.arrivalDelay.toLong())
                sparkArrival[index].animateTo(
                    targetValue   = 1f,
                    animationSpec = tween(1400, easing = FastOutSlowInEasing)
                )
            }
        }

        // Attendre que toutes soient arrivées (720ms + 1400ms = 2120ms)
        delay(2200)

        // ── Phase 4 : Toutes les étincelles s'éteignent ensemble (lent)
        sparksFade.animateTo(
            targetValue   = 0f,
            animationSpec = tween(1800, easing = FastOutSlowInEasing)
        )

        // ── Phase 5 : Nom PHOEN-X (pendant l'extinction des étincelles)
        launch {
            nameAlpha.animateTo(1.0f, tween(1200, easing = FastOutSlowInEasing))
        }
        launch {
            nameOffsetY.animateTo(0f, tween(1200, easing = EaseOutCubic))
        }

        delay(800)

        // ── Phase 6 : Ligne dorée ───────────────────────────────────
        lineWidth.animateTo(52f, tween(1000, easing = FastOutSlowInEasing))

        delay(600)

        // ── Phase 7 : Phrase ────────────────────────────────────────
        launch {
            phraseAlpha.animateTo(1.0f, tween(1400, easing = FastOutSlowInEasing))
        }
        launch {
            phraseOffY.animateTo(0f, tween(1400, easing = EaseOutCubic))
        }

        // Maintien 3 secondes puis navigation
        delay(3000)
        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPrimary),
        contentAlignment = Alignment.Center
    ) {

        // Halo radial pulsant très lent
        Box(
            modifier = Modifier
                .size(260.dp)
                .scale(haloScale)
                .alpha(haloAlpha)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(AccentPrimary, Color.Transparent),
                        center = Offset(130f, 130f),
                        radius = 380f
                    ),
                    shape = CircleShape
                )
        )

        // Étincelles sur Canvas
        Canvas(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.Center)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)

            sparks.forEachIndexed { i, spark ->
                val arrival  = sparkArrival[i].value
                if (arrival <= 0f) return@forEachIndexed

                val globalFade = sparksFade.value  // 1f = visible, 0f = éteint
                val alpha      = arrival * globalFade
                val radius     = spark.size.dp.toPx() * globalFade.coerceAtLeast(0.3f)
                val dist       = spark.distance * arrival
                val x          = center.x + dist.dp.toPx() * cos(spark.angle)
                val y          = center.y + dist.dp.toPx() * sin(spark.angle)

                val color = if (i % 2 == 0) Color(0xFFC97B3A) else Color(0xFFE8A85F)

                drawCircle(
                    color  = color.copy(alpha = alpha.coerceIn(0f, 1f)),
                    radius = radius,
                    center = Offset(x, y)
                )
            }
        }

        // Contenu principal
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier            = Modifier.fillMaxWidth()
        ) {

            Image(
                painter            = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Logo Phoen-X",
                modifier           = Modifier
                    .size(128.dp)
                    .graphicsLayer {
                        scaleX       = logoScale.value
                        scaleY       = logoScale.value
                        alpha        = logoAlpha.value
                        translationY = logoOffsetY.value.dp.toPx()
                    }
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text  = "PHOEN-X",
                style = TextStyle(
                    fontFamily    = FontFamily.Serif,
                    fontSize      = 24.sp,
                    letterSpacing = TextUnit(0.42f, TextUnitType.Em),
                    color         = TextPrimary
                ),
                modifier = Modifier
                    .alpha(nameAlpha.value)
                    .offset(y = nameOffsetY.value.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .width(lineWidth.value.dp)
                    .height(1.dp)
                    .background(AccentPrimary)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text      = "Ce n'est pas une archive. C'est une présence.",
                style     = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontSize   = 13.sp,
                    fontStyle  = FontStyle.Italic,
                    color      = Color(0xFF9B9590)
                ),
                textAlign = TextAlign.Center,
                modifier  = Modifier
                    .alpha(phraseAlpha.value)
                    .offset(y = phraseOffY.value.dp)
                    .padding(horizontal = 36.dp)
            )
        }
    }
}
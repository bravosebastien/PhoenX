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
import androidx.compose.ui.graphics.StrokeCap
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
    val delay: Long,
    val color: Color
)

private val sparks = listOf(
    Spark(angle = -2.0f, distance = 62f, size = 4f,   delay = 480, color = Color(0xFFC97B3A)),
    Spark(angle = -1.1f, distance = 70f, size = 3f,   delay = 600, color = Color(0xFFC97B3A)),
    Spark(angle = -0.3f, distance = 75f, size = 2.5f, delay = 530, color = Color(0xFFE8A85F)),
    Spark(angle =  0.6f, distance = 68f, size = 2.5f, delay = 680, color = Color(0xFFE8A85F)),
    Spark(angle =  1.4f, distance = 72f, size = 3f,   delay = 560, color = Color(0xFFC97B3A)),
    Spark(angle =  2.2f, distance = 65f, size = 2f,   delay = 720, color = Color(0xFFE8A85F)),
    Spark(angle =  3.0f, distance = 78f, size = 3f,   delay = 500, color = Color(0xFFC97B3A))
)

@Composable
fun SplashScreen(
    onAnimationFinished: () -> Unit
) {
    val logoScale      = remember { Animatable(0.55f) }
    val logoAlpha      = remember { Animatable(0f) }
    val logoOffsetY    = remember { Animatable(18f) }
    val nameAlpha      = remember { Animatable(0f) }
    val nameOffsetY    = remember { Animatable(8f) }
    val lineWidth      = remember { Animatable(0f) }
    val phraseAlpha    = remember { Animatable(0f) }
    val phraseOffY     = remember { Animatable(6f) }
    val sparkProgress  = sparks.map { remember { Animatable(0f) } }

    val haloScale by rememberInfiniteTransition(label = "halo").animateFloat(
        initialValue  = 0.75f,
        targetValue   = 1.25f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloScale"
    )
    val haloAlpha by rememberInfiniteTransition(label = "haloA").animateFloat(
        initialValue  = 0.08f,
        targetValue   = 0.18f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloAlpha"
    )

    LaunchedEffect(Unit) {

        launch {
            logoScale.animateTo(1.05f, tween(500, easing = EaseOutBack))
            logoScale.animateTo(1.0f,  tween(120, easing = LinearEasing))
        }
        launch { logoAlpha.animateTo(1.0f, tween(600, easing = FastOutSlowInEasing)) }
        launch { logoOffsetY.animateTo(0f,  tween(600, easing = EaseOutCubic)) }

        delay(420)

        sparks.forEachIndexed { index, spark ->
            launch {
                delay(spark.delay - 420)
                sparkProgress[index].animateTo(
                    targetValue   = 1f,
                    animationSpec = tween(700, easing = FastOutSlowInEasing)
                )
                sparkProgress[index].animateTo(
                    targetValue   = 2f,
                    animationSpec = tween(400, easing = LinearEasing)
                )
            }
        }

        delay(230)

        launch { nameAlpha.animateTo(1.0f, tween(380, easing = FastOutSlowInEasing)) }
        launch { nameOffsetY.animateTo(0f,  tween(380, easing = EaseOutCubic)) }

        delay(200)

        lineWidth.animateTo(52f, tween(300, easing = FastOutSlowInEasing))

        delay(180)

        launch { phraseAlpha.animateTo(1.0f, tween(400, easing = FastOutSlowInEasing)) }
        launch { phraseOffY.animateTo(0f,    tween(400, easing = EaseOutCubic)) }

        delay(1800)
        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPrimary),
        contentAlignment = Alignment.Center
    ) {

        Box(
            modifier = Modifier
                .size(220.dp)
                .scale(haloScale)
                .alpha(haloAlpha)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(AccentPrimary, Color.Transparent),
                        center = Offset(110f, 110f),
                        radius = 300f
                    ),
                    shape = CircleShape
                )
        )

        Canvas(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.Center)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)

            sparks.forEachIndexed { i, spark ->
                val progress = sparkProgress[i].value
                if (progress <= 0f) return@forEachIndexed

                val travelProgress = progress.coerceIn(0f, 1f)
                val fadeProgress   = (progress - 1f).coerceIn(0f, 1f)
                val currentDist    = spark.distance * travelProgress
                val alpha          = (travelProgress * (1f - fadeProgress)).coerceIn(0f, 1f)
                val radius         = (spark.size * (1f - fadeProgress * 0.5f)).dp.toPx()
                val x              = center.x + currentDist.dp.toPx() * cos(spark.angle)
                val y              = center.y + currentDist.dp.toPx() * sin(spark.angle)

                drawCircle(
                    color  = spark.color.copy(alpha = alpha),
                    radius = radius,
                    center = Offset(x, y)
                )
            }
        }

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
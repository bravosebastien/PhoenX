package com.example.phoenx.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * v12.7 : Sélecteur de couleur complet (teinte + saturation/luminosité), pour remplacer
 * les simples pastilles de couleurs prédéfinies dans Réglages > Apparence et Style.
 *
 * - La bande arc-en-ciel du bas choisit la TEINTE de base (rouge, bleu, vert...).
 * - Le grand carré choisit la nuance exacte de cette teinte : glisser vers la droite = plus
 *   saturé (plus vif), vers le haut = plus clair (vers le blanc), vers le bas = plus foncé
 *   (vers le noir).
 *
 * Le rond blanc indique la position actuelle ; la pastille à droite du titre montre un aperçu
 * de la couleur choisie. `onColorChanged` est appelé en continu pendant le glissement, pour un
 * aperçu en direct.
 */
@Composable
fun HsvColorPicker(
    initialColor: Color,
    onColorChanged: (Color) -> Unit
) {
    val initialHsv = remember(initialColor) {
        val arr = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), arr)
        arr
    }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var sat by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }

    // Si la couleur initiale change depuis l'extérieur (ex: bouton "Réinitialiser"), on
    // resynchronise le curseur sans casser le glissement en cours.
    LaunchedEffect(initialColor) {
        val arr = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), arr)
        hue = arr[0]; sat = arr[1]; value = arr[2]
    }

    fun currentColor(): Color = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value)))

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(currentColor(), CircleShape)
                    .border(1.dp, Color.Black.copy(alpha = 0.2f), CircleShape)
            )
        }

        Spacer(Modifier.height(12.dp))

        // CARRÉ SATURATION (X) / LUMINOSITÉ (Y)
        val squareSize = 220.dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(squareSize)
                .pointerInput(hue) {
                    fun updateFromOffset(offset: Offset) {
                        val x = offset.x.coerceIn(0f, size.width.toFloat())
                        val y = offset.y.coerceIn(0f, size.height.toFloat())
                        sat = x / size.width
                        value = 1f - (y / size.height)
                        onColorChanged(currentColor())
                    }
                    detectTapGestures(onPress = { updateFromOffset(it) })
                }
                .pointerInput(hue) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val x = change.position.x.coerceIn(0f, size.width.toFloat())
                        val y = change.position.y.coerceIn(0f, size.height.toFloat())
                        sat = x / size.width
                        value = 1f - (y / size.height)
                        onColorChanged(currentColor())
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(squareSize)) {
                val hueColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                drawRect(brush = Brush.horizontalGradient(listOf(Color.White, hueColor)))
                drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))

                val cursorX = (sat * size.width).coerceIn(0f, size.width)
                val cursorY = ((1f - value) * size.height).coerceIn(0f, size.height)
                drawCircle(color = Color.White, radius = 10.dp.toPx(), center = Offset(cursorX, cursorY), style = Stroke(width = 3.dp.toPx()))
                drawCircle(color = Color.Black.copy(alpha = 0.5f), radius = 10.dp.toPx(), center = Offset(cursorX, cursorY), style = Stroke(width = 1.dp.toPx()))
            }
        }

        Spacer(Modifier.height(16.dp))

        // BANDE DE TEINTE (arc-en-ciel)
        val hueColors = remember {
            (0..12).map { step -> Color(android.graphics.Color.HSVToColor(floatArrayOf(step * 30f, 1f, 1f))) }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .pointerInput(Unit) {
                    fun updateHueFromOffset(offset: Offset) {
                        val x = offset.x.coerceIn(0f, size.width.toFloat())
                        hue = (x / size.width) * 360f
                        onColorChanged(currentColor())
                    }
                    detectTapGestures(onPress = { updateHueFromOffset(it) })
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val x = change.position.x.coerceIn(0f, size.width.toFloat())
                        hue = (x / size.width) * 360f
                        onColorChanged(currentColor())
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(32.dp)) {
                drawRect(brush = Brush.horizontalGradient(hueColors))
                val cursorX = ((hue / 360f) * size.width).coerceIn(0f, size.width)
                drawCircle(color = Color.White, radius = size.height / 2f, center = Offset(cursorX, size.height / 2f), style = Stroke(width = 3.dp.toPx()))
                drawCircle(color = Color.Black.copy(alpha = 0.3f), radius = size.height / 2f, center = Offset(cursorX, size.height / 2f), style = Stroke(width = 1.dp.toPx()))
            }
        }
    }
}

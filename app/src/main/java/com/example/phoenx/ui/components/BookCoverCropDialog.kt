package com.example.phoenx.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.RectF
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.phoenx.ui.theme.*
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@Composable
fun BookCoverCropDialog(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onConfirmed: (Uri) -> Unit,
    accent: Color
) {
    val context = LocalContext.current
    val theme = LocalAppTheme.current
    
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    
    val bitmap = remember(imageUri) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) { null }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = theme.backgroundColor,
            modifier = Modifier.fillMaxWidth().height(550.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Cadre la couverture", style = MaterialTheme.typography.titleMedium, color = theme.contentColor)
                Text("Ratio Livre (0.72)", style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.5f))
                
                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .aspectRatio(0.72f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black)
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.5f, 5f)
                                offset += pan
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                ),
                            contentScale = ContentScale.Fit
                        )
                    }
                    
                    // Guide visuel rectangulaire (v9.2.4)
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        drawIntoCanvas { canvas ->
                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                style = android.graphics.Paint.Style.STROKE
                                strokeWidth = 2.dp.toPx()
                                pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 10f), 0f)
                            }
                            canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onDismiss) { Text("Annuler", color = theme.contentColor.copy(alpha = 0.6f)) }
                    Button(
                        onClick = {
                            if (bitmap != null) {
                                // Recadrage au ratio 0.72
                                val cropped = cropBitmapToRatio(context, bitmap, scale, offset, 0.72f, 400)
                                if (cropped != null) {
                                    val uri = saveBitmapToTempUri(context, cropped)
                                    onConfirmed(uri)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accent)
                    ) {
                        Text("Confirmer", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun cropBitmapToRatio(context: android.content.Context, source: Bitmap, scale: Float, offset: androidx.compose.ui.geometry.Offset, ratio: Float, baseWidth: Int): Bitmap? {
    val density = context.resources.displayMetrics.density
    val targetWidth = baseWidth
    val targetHeight = (baseWidth / ratio).toInt()
    
    val result = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    
    val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)
    val matrix = android.graphics.Matrix()
    
    // Échelle initiale pour couvrir la zone cible
    val initialScale = Math.max(targetWidth.toFloat() / source.width, targetHeight.toFloat() / source.height)
    val dx = (targetWidth - source.width * initialScale) / 2f
    val dy = (targetHeight - source.height * initialScale) / 2f
    
    matrix.postScale(initialScale, initialScale)
    matrix.postTranslate(dx, dy)
    matrix.postScale(scale, scale, targetWidth / 2f, targetHeight / 2f)
    matrix.postTranslate(offset.x, offset.y)
    
    canvas.drawBitmap(source, matrix, paint)
    return result
}

private fun saveBitmapToTempUri(context: android.content.Context, bitmap: Bitmap): Uri {
    val file = File(context.cacheDir, "temp_book_cover_${UUID.randomUUID()}.jpg")
    val out = FileOutputStream(file)
    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
    out.close()
    return Uri.fromFile(file)
}

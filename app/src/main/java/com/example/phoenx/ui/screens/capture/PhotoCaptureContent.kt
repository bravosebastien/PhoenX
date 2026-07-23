package com.example.phoenx.ui.screens.capture

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import com.example.phoenx.ui.components.RecipientSelector
import com.example.phoenx.ui.theme.*
import java.io.File

@Composable
fun PhotoCaptureContent(
    padding: PaddingValues,
    capturedPhoto: File?,
    caption: String,
    onCaptionChange: (String) -> Unit,
    onPhotoCaptured: (File) -> Unit,
    preselectedName: String? = null,
    recipients: List<com.example.phoenx.data.local.RecipientEntity> = emptyList(),
    selectedRecipientIds: MutableList<String>,
    visibility: String,
    onVisibilityChange: (String) -> Unit,
    // Personnes citées (v8.8)
    selectedPersons: List<com.example.phoenx.data.local.PersonEntity> = emptyList(),
    suggestedPersons: List<com.example.phoenx.data.local.PersonEntity> = emptyList(),
    onSearchPersons: (String) -> Unit = {},
    onSelectPerson: (com.example.phoenx.data.local.PersonEntity) -> Unit = {},
    onCreatePerson: (String, String?, String?, String?, String?) -> Unit = { _, _, _, _, _ -> },
    onRemovePerson: (String) -> Unit = {},
    // Menus déroulants (v8.9.2)
    selectedCategory: String = "Sagesse",
    onCategoryChange: (String) -> Unit = {},
    isTonaliteExpanded: Boolean = false,
    onTonaliteToggle: () -> Unit = {},
    isTiroirsExpanded: Boolean = false,
    onTiroirsToggle: () -> Unit = {}
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }
    
    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        if (capturedPhoto == null) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                CameraPreview(
                    imageCapture = imageCapture,
                    modifier = Modifier.fillMaxSize()
                )
                
                IconButton(
                    onClick = {
                        val photoFile = File(context.cacheDir, "phoenx_${System.currentTimeMillis()}.jpg")
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                        
                        imageCapture.takePicture(
                            outputOptions,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                    onPhotoCaptured(photoFile)
                                }
                                override fun onError(exception: ImageCaptureException) {
                                    android.util.Log.e("CaptureScreen", "Photo capture failed", exception)
                                }
                            }
                        )
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 40.dp)
                        .size(80.dp)
                        .background(Color.White, CircleShape)
                        .border(4.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Prendre une photo", tint = Color.Black)
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.DarkGray)) {
                    AsyncImage(
                        model = capturedPhoto,
                        contentDescription = "Photo capturée",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    
                    IconButton(
                        onClick = { onPhotoCaptured(File("clear")) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Supprimer la photo", tint = Color.White)
                    }

                    if (preselectedName != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp).padding(bottom = 80.dp)
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, null, tint = accent, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Enregistré pour : $preselectedName", style = MaterialTheme.typography.labelSmall, color = accent)
                            }
                        }
                    }

                    TextField(
                        value = caption,
                        onValueChange = onCaptionChange,
                        placeholder = { 
                            Text(
                                "Donne une âme à cette photo...", 
                                style = MaterialTheme.typography.bodyLarge, 
                                color = Color.White.copy(alpha = 0.6f)
                            ) 
                        },
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp).padding(bottom = 40.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic, color = Color.White),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Black.copy(alpha = 0.6f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.4f),
                            focusedIndicatorColor = accent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = accent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
                    // POUR QUI (v8.9.2 : Menu déroulant)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTiroirsToggle() }
                            .border(1.dp, theme.contentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                        color = theme.contentColor.copy(alpha = 0.03f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "DANS QUELS TIROIRS ?", 
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp), 
                                color = theme.contentColor.copy(alpha = 0.4f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val count = selectedRecipientIds.size
                                val label = if (visibility == "EVERYONE") "Tout le monde" else if (count == 0) "Privé" else "$count choisi(s)"
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = accent
                                )
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    imageVector = if (isTiroirsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = theme.contentColor.copy(alpha = 0.2f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = isTiroirsExpanded) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            RecipientSelector(
                                recipients = recipients,
                                selectedIds = selectedRecipientIds,
                                visibility = visibility,
                                onVisibilityChange = onVisibilityChange,
                                accent = accent
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    HorizontalDivider(color = theme.contentColor.copy(alpha = 0.2f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

                    // TONALITÉ (v8.9.2 : Menu déroulant)
                    Column {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTonaliteToggle() }
                                .border(1.dp, theme.contentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                            color = theme.contentColor.copy(alpha = 0.03f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "QUELLE TONALITÉ ?", 
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp), 
                                        color = theme.contentColor.copy(alpha = 0.4f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = selectedCategory,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = accent
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Icon(
                                        imageVector = if (isTonaliteExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = theme.contentColor.copy(alpha = 0.2f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = isTonaliteExpanded) {
                            Column {
                                Spacer(modifier = Modifier.height(12.dp))
                                val categories = listOf("Sagesse", "Aventure", "Secret", "Famille", "Amour", "Nostalgie", "Humour", "Leçon", "Voyage", "Quotidien", "Épreuve")
                                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    categories.forEach { cat ->
                                        FilterChip(
                                            selected = selectedCategory == cat,
                                            onClick = { onCategoryChange(cat) },
                                            label = { Text(cat) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = accent,
                                                selectedLabelColor = theme.backgroundColor,
                                                containerColor = theme.contentColor.copy(alpha = 0.05f),
                                                labelColor = theme.contentColor.copy(alpha = 0.6f)
                                            ),
                                            border = BorderStroke(
                                                1.dp, 
                                                if (selectedCategory == cat) accent.copy(alpha = 0.5f) else theme.contentColor.copy(alpha = 0.1f)
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }

                    HorizontalDivider(color = theme.contentColor.copy(alpha = 0.2f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

                    Spacer(modifier = Modifier.height(24.dp))

                    // v8.8 : Personnes citées
                    com.example.phoenx.ui.components.PersonSelector(
                        selectedPersons = selectedPersons,
                        suggestedPersons = suggestedPersons,
                        onSearch = onSearchPersons,
                        onSelect = onSelectPerson,
                        onCreate = onCreatePerson,
                        onRemove = onRemovePerson,
                        accent = accent
                    )
                }
            }
        }
    }
}

@Composable
fun CameraPreview(
    imageCapture: ImageCapture,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
                } catch (e: Exception) {
                    android.util.Log.e("CameraPreview", "Use case binding failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = modifier
    )
}

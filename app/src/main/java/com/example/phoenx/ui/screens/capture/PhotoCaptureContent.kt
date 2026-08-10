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
import com.example.phoenx.domain.model.SimplifiedPerson
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
    selectedRecipientIds: List<String>,
    onToggleRecipient: (String) -> Unit,
    visibility: String,
    onVisibilityChange: (String) -> Unit,
    // NOUVEAUTÉ v8.9.8
    notifyByEmail: Boolean = false,
    onNotifyByEmailChange: ((Boolean) -> Unit)? = null,
    // Personnes citées (v8.8)
    selectedPersons: List<SimplifiedPerson> = emptyList(),
    suggestedPersons: List<SimplifiedPerson> = emptyList(),
    onSearchPersons: (String) -> Unit = {},
    onSelectPerson: (SimplifiedPerson) -> Unit = {},
    onSelectMe: () -> Unit = {}, // v9.0
    onCreatePerson: (String, String?, String?, String?, String?, android.net.Uri?, String) -> Unit = { _, _, _, _, _, _, _ -> },
    onRemovePerson: (String) -> Unit = {},
    onManageCharacters: () -> Unit = {},
    // Menus déroulants (v8.9.2)
    selectedCategory: String = "Sagesse",
    onCategoryChange: (String) -> Unit = {},
    isTonaliteExpanded: Boolean = false,
    onTonaliteToggle: () -> Unit = {},
    isTiroirsExpanded: Boolean = false, // Rétabli v9.0.1
    onTiroirsToggle: () -> Unit = {},
    // NUANCE PERSONNELLE (v9.4.27)
    tonalNuance: String = "",
    onTonalNuanceChange: (String) -> Unit = {},
    currentStep: Int = 1,
    enigmaQuestion: String = "",
    onEnigmaQuestionChange: (String) -> Unit = {},
    enigmaAnswer: String = "",
    onEnigmaAnswerChange: (String) -> Unit = {},
    scheduledTimestamp: Long? = null,
    onScheduledTimestampChange: (Long?) -> Unit = {}
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
                    if (currentStep == 1) {
                        // v8.8 : Personnes citées
                        com.example.phoenx.ui.components.PersonSelector(
                            selectedPersons = selectedPersons,
                            suggestedPersons = suggestedPersons,
                            onSearch = onSearchPersons,
                            onSelect = onSelectPerson,
                            onSelectMe = onSelectMe,
                            onCreate = onCreatePerson,
                            onRemove = onRemovePerson,
                            onManageCharacters = onManageCharacters,
                            accent = accent
                        )
                    } else {
                        // ÉTAPE 2 : HABILLAGE PHOTO
                        Text(
                            "HABILLAGE & DESTINATION", 
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp), 
                            color = theme.contentColor.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // POUR QUI (Tiroirs)
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
                                val count = selectedRecipientIds.size
                                val label = if (visibility == "EVERYONE") "Tout le monde" else if (count == 0) "Privé" else "$count choisi(s)"
                                Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent)
                            }
                        }

                        AnimatedVisibility(visible = isTiroirsExpanded) {
                            Column {
                                Spacer(modifier = Modifier.height(12.dp))
                                com.example.phoenx.ui.components.RecipientSelector(
                                    recipients = recipients,
                                    selectedIds = selectedRecipientIds,
                                    onToggleRecipient = onToggleRecipient,
                                    visibility = visibility,
                                    onVisibilityChange = onVisibilityChange,
                                    accent = accent,
                                    notifyByEmail = notifyByEmail,
                                    onNotifyByEmailChange = onNotifyByEmailChange
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        HorizontalDivider(color = theme.contentColor.copy(alpha = 0.1f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 12.dp))

                        // TONALITÉ
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
                                Text(
                                    text = "QUELLE TONALITÉ ?", 
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp), 
                                    color = theme.contentColor.copy(alpha = 0.4f)
                                )
                                Text(text = selectedCategory, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent)
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
                                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent, selectedLabelColor = theme.backgroundColor)
                                        )
                                    }
                                }
                            }
                        }
                        
                        // NUANCE LIBRE (v9.4.27)
                        Spacer(modifier = Modifier.height(24.dp))
                        OutlinedTextField(
                            value = tonalNuance,
                            onValueChange = { if (it.length <= 100) onTonalNuanceChange(it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Précisez la nuance (facultatif)", fontSize = 11.sp) },
                            placeholder = { Text("Ex : un peu amer mais je souris en l'écrivant...", fontSize = 11.sp) },
                            maxLines = 3,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accent.copy(alpha = 0.5f),
                                unfocusedBorderColor = theme.contentColor.copy(alpha = 0.1f),
                                focusedTextColor = theme.contentColor,
                                unfocusedTextColor = theme.contentColor
                            ),
                            supportingText = {
                                Text("${tonalNuance.length}/100", modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.End, fontSize = 10.sp)
                            }
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider(color = theme.contentColor.copy(alpha = 0.1f), thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(24.dp))

                        // CAPSULE TEMPORELLE (Ouverture programmée)
                        Text(
                            "CAPSULE TEMPORELLE", 
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp), 
                            color = theme.contentColor.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val dateText = scheduledTimestamp?.let {
                            java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale.FRENCH)
                                .withZone(java.time.ZoneId.systemDefault())
                                .format(java.time.Instant.ofEpochMilli(it))
                        } ?: "Dès maintenant"
                        
                        var showDatePicker by remember { mutableStateOf(false) }
                        val datePickerState = rememberDatePickerState()

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDatePicker = true }
                                .border(1.dp, theme.contentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                            color = theme.contentColor.copy(alpha = 0.03f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Event, null, tint = accent, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(dateText, color = if(scheduledTimestamp != null) accent else theme.contentColor.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        if (showDatePicker) {
                            DatePickerDialog(
                                onDismissRequest = { showDatePicker = false },
                                confirmButton = {
                                    TextButton(onClick = {
                                        onScheduledTimestampChange(datePickerState.selectedDateMillis)
                                        showDatePicker = false
                                    }) { Text("Confirmer", color = accent) }
                                }
                            ) { DatePicker(state = datePickerState) }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider(color = theme.contentColor.copy(alpha = 0.1f), thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(24.dp))

                        // VERROU
                        Text(
                            "PROTECTION SECRÈTE", 
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp), 
                            color = theme.contentColor.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = enigmaQuestion,
                            onValueChange = onEnigmaQuestionChange,
                            label = { Text("Question secrète") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = enigmaAnswer,
                            onValueChange = onEnigmaAnswerChange,
                            label = { Text("Réponse attendue") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor)
                        )
                        
                        Spacer(modifier = Modifier.height(48.dp))
                    }
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

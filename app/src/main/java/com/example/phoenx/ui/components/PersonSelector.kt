package com.example.phoenx.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.RectF
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.phoenx.domain.model.SimplifiedPerson
import com.example.phoenx.ui.theme.*
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonSelector(
    selectedPersons: List<SimplifiedPerson>,
    suggestedPersons: List<SimplifiedPerson>,
    onSearch: (String) -> Unit,
    onSelect: (SimplifiedPerson) -> Unit,
    onSelectMe: () -> Unit = {},
    onCreate: (firstName: String, lastName: String?, relation: String?, distType: String?, distValue: String?, imageUri: Uri?, characterType: String) -> Unit,
    onRemove: (String) -> Unit,
    onManageCharacters: () -> Unit = {},
    accent: Color,
    enabled: Boolean = true,
    simpleMode: Boolean = false // v9.4.22 : Masque "Gérer" et "Moi"
) {
    val theme = LocalAppTheme.current
    var query by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var duplicateNameDialog by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Personnes mentionnées",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = theme.contentColor.copy(alpha = 0.4f),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (enabled && !simpleMode) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // RACCOURCI MOI (v9.4.27 : Visibilité renforcée)
                Button(
                    onClick = onSelectMe,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent.copy(alpha = 0.1f), contentColor = accent),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("C'est moi", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }

                OutlinedButton(
                    onClick = onManageCharacters,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.contentColor.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Default.Group, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Gérer mes personnages", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        
        // Tags des personnes sélectionnées
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            selectedPersons.forEach { person ->
                InputChip(
                    selected = true,
                    onClick = { if (enabled) onRemove(person.id) },
                    label = { Text(person.name) },
                    enabled = enabled,
                    leadingIcon = {
                        CameoPortrait(
                            imagePath = person.photoUrl,
                            firstName = person.name,
                            size = 20.dp
                        )
                    },
                    trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp)) },
                    colors = InputChipDefaults.inputChipColors(
                        selectedContainerColor = accent.copy(alpha = 0.2f),
                        selectedLabelColor = accent
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }

        // Champ de recherche
        if (enabled) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    onSearch(it)
                },
                placeholder = { Text("Qui est présent ?", fontSize = 14.sp, color = theme.contentColor.copy(alpha = 0.4f)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.PersonAdd, null, tint = accent) },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = {
                            val existing = suggestedPersons.find { it.name.equals(query, ignoreCase = true) }
                            if (existing != null) {
                                duplicateNameDialog = query
                            } else {
                                showCreateDialog = true
                            }
                        }) {
                            Icon(Icons.Default.Add, null, tint = accent)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    unfocusedBorderColor = theme.contentColor.copy(alpha = 0.1f),
                    focusedTextColor = theme.contentColor,
                    unfocusedTextColor = theme.contentColor,
                    focusedLabelColor = accent,
                    unfocusedLabelColor = theme.contentColor.copy(alpha = 0.6f),
                    cursorColor = accent
                )
            )
        }

        // Suggestions
        if (suggestedPersons.isNotEmpty() && query.isNotBlank()) {
            Surface(
                modifier = Modifier.padding(top = 4.dp).fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = theme.contentColor.copy(alpha = 0.05f),
                border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f)),
                tonalElevation = 2.dp
            ) {
                Column {
                    suggestedPersons.forEach { person ->
                        ListItem(
                            headlineContent = { Text(person.name, color = theme.contentColor, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(person.relationship ?: "Proche", color = theme.contentColor.copy(alpha = 0.6f)) },
                            leadingContent = {
                                CameoPortrait(
                                    imagePath = person.photoUrl,
                                    firstName = person.name,
                                    size = 32.dp
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { 
                                if (enabled) {
                                    val p = person
                                    query = "" // Vider immédiatement pour éviter le double clic (v9.4.19)
                                    onSelect(p)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialog de création
    if (showCreateDialog) {
        CreatePersonDialog(
            initialFirstName = query,
            onDismiss = { showCreateDialog = false },
            onConfirm = { f, l, r, dt, dv, uri, ct ->
                onCreate(f, l, r, dt, dv, uri, ct)
                showCreateDialog = false
                query = ""
            },
            accent = accent
        )
    }

    // Dialog si nom en double
    if (duplicateNameDialog != null) {
        AlertDialog(
            onDismissRequest = { duplicateNameDialog = null },
            title = { Text("Nom déjà existant", color = theme.contentColor, fontWeight = FontWeight.Bold) },
            text = { Text("Il y a déjà un(e) ${duplicateNameDialog} dans votre liste. Voulez-vous utiliser la personne existante ou en créer une nouvelle ?", color = theme.contentColor.copy(alpha = 0.7f)) },
            confirmButton = {
                TextButton(onClick = { 
                    showCreateDialog = true
                    duplicateNameDialog = null 
                }) { Text("Créer un nouveau", color = accent, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { 
                    val p = suggestedPersons.first { it.name.equals(duplicateNameDialog, ignoreCase = true) }
                    onSelect(p)
                    duplicateNameDialog = null
                    query = ""
                }) { Text("Utiliser l'existant", color = theme.contentColor) }
            },
            containerColor = theme.backgroundColor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePersonDialog(
    initialFirstName: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String?, String?, String?, String?, Uri?, String) -> Unit,
    accent: Color
) {
    var firstName by remember { mutableStateOf(initialFirstName) }
    var lastName by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    var distinctionType by remember { mutableStateOf<String?>(null) }
    var distinctionValue by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var characterType by remember { mutableStateOf("HUMAN") }
    var showCropDialog by remember { mutableStateOf(false) }
    var tempPickedUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            tempPickedUri = uri
            showCropDialog = true
        }
    }

    if (showCropDialog && tempPickedUri != null) {
        CameoCropDialog(
            imageUri = tempPickedUri!!,
            onDismiss = { showCropDialog = false },
            onConfirmed = { croppedUri ->
                selectedImageUri = croppedUri
                showCropDialog = false
            },
            accent = accent
        )
    }

    val valuePlaceholder = when(distinctionType) {
        "nom_famille" -> "Ex: Martin, Vallet..."
        "surnom" -> "Ex: Le Grand, Mamie..."
        "ville" -> "Ex: Lyon, Bordeaux..."
        "autre" -> "Précise ici (ex: l'ancien voisin...)"
        else -> "Choisis d'abord un type"
    }

    val theme = LocalAppTheme.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = theme.backgroundColor,
            border = androidx.compose.foundation.BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Nouveau Personnage", style = MaterialTheme.typography.headlineSmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold), color = theme.contentColor)
                Spacer(Modifier.height(24.dp))

                // TYPE SELECTOR (v9.1)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    FilterChip(
                        selected = characterType == "HUMAN",
                        onClick = { characterType = "HUMAN" },
                        label = { Text("Humain") },
                        leadingIcon = if (characterType == "HUMAN") {
                            { Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent, selectedLabelColor = theme.backgroundColor, selectedLeadingIconColor = theme.backgroundColor)
                    )
                    Spacer(Modifier.width(12.dp))
                    FilterChip(
                        selected = characterType == "ANIMAL",
                        onClick = { characterType = "ANIMAL" },
                        label = { Text("Animal") },
                        leadingIcon = if (characterType == "ANIMAL") {
                            { Icon(Icons.Default.Pets, null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent, selectedLabelColor = theme.backgroundColor, selectedLeadingIconColor = theme.backgroundColor)
                    )
                }

                Spacer(Modifier.height(24.dp))

                // BOUTON CAMEO
                Box(
                    modifier = Modifier
                        .clickable { photoPickerLauncher.launch("image/*") }
                        .padding(bottom = 24.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    CameoPortrait(
                        imagePath = selectedImageUri?.toString(),
                        firstName = if (firstName.isBlank()) "?" else firstName,
                        size = 80.dp
                    )
                    Surface(
                        modifier = Modifier.size(28.dp),
                        shape = CircleShape,
                        color = accent,
                        border = androidx.compose.foundation.BorderStroke(2.dp, theme.backgroundColor)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AddAPhoto, null, tint = theme.backgroundColor, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text(if (characterType == "HUMAN") "Prénom" else "Petit nom") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                )
                
                if (characterType == "HUMAN") {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Nom (optionnel)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                    )
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = relationship,
                    onValueChange = { relationship = it },
                    label = { Text(if (characterType == "HUMAN") "Lien de parenté / Relation" else "C'est qui pour toi ?") },
                    placeholder = { Text(if (characterType == "HUMAN") "Ex: Mon cousin" else "Ex: Mon fidèle compagnon") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                )
                Spacer(Modifier.height(24.dp))

                Text("Pour les différencier (si besoin)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = theme.contentColor.copy(alpha = 0.4f), modifier = Modifier.align(Alignment.Start))
                Spacer(Modifier.height(12.dp))

                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("nom_famille" to "Nom", "surnom" to "Surnom", "ville" to "Ville", "autre" to "Autre").forEach { (id, label) ->
                        FilterChip(
                            selected = distinctionType == id,
                            onClick = { distinctionType = id },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accent,
                                selectedLabelColor = theme.backgroundColor,
                                labelColor = theme.contentColor.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
                
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = distinctionValue,
                    onValueChange = { distinctionValue = it },
                    label = { Text("Précision") },
                    placeholder = { Text(valuePlaceholder, color = theme.contentColor.copy(alpha = 0.3f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedTextColor = theme.contentColor, unfocusedTextColor = theme.contentColor)
                )

                Spacer(Modifier.height(32.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Annuler", color = theme.contentColor.copy(alpha = 0.6f)) }
                    Button(
                        onClick = { 
                            onConfirm(firstName, if(lastName.isBlank()) null else lastName, if(relationship.isBlank()) null else relationship, distinctionType, if(distinctionValue.isBlank()) null else distinctionValue, selectedImageUri, characterType)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        enabled = firstName.isNotBlank()
                    ) { Text("Créer", color = theme.backgroundColor, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
fun CameoCropDialog(
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
            modifier = Modifier.fillMaxWidth().height(500.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Cadre le portrait", style = MaterialTheme.typography.titleMedium, color = theme.contentColor)
                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(CircleShape)
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
                    
                    // Guide visuel ovale Cameo
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        drawIntoCanvas { canvas ->
                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                style = android.graphics.Paint.Style.STROKE
                                strokeWidth = 2.dp.toPx()
                                pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 10f), 0f)
                            }
                            val rect = RectF(0f, 0f, size.width, size.height)
                            canvas.nativeCanvas.drawOval(rect, paint)
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onDismiss) { Text("Annuler", color = theme.contentColor.copy(alpha = 0.6f)) }
                    Button(
                        onClick = {
                            if (bitmap != null) {
                                val cropped = cropBitmap(context, bitmap, scale, offset, 240)
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

private fun cropBitmap(context: android.content.Context, source: Bitmap, scale: Float, offset: androidx.compose.ui.geometry.Offset, viewSizeDp: Int): Bitmap? {
    val density = context.resources.displayMetrics.density
    val viewSizePx = (viewSizeDp * density).toInt()
    
    val result = Bitmap.createBitmap(viewSizePx, viewSizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    
    val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)
    val matrix = android.graphics.Matrix()
    
    val initialScale = Math.min(viewSizePx.toFloat() / source.width, viewSizePx.toFloat() / source.height)
    val dx = (viewSizePx - source.width * initialScale) / 2f
    val dy = (viewSizePx - source.height * initialScale) / 2f
    
    matrix.postScale(initialScale, initialScale)
    matrix.postTranslate(dx, dy)
    matrix.postScale(scale, scale, viewSizePx / 2f, viewSizePx / 2f)
    matrix.postTranslate(offset.x, offset.y)
    
    canvas.drawBitmap(source, matrix, paint)
    return result
}

private fun saveBitmapToTempUri(context: android.content.Context, bitmap: Bitmap): Uri {
    val file = File(context.cacheDir, "temp_crop_${UUID.randomUUID()}.jpg")
    val out = FileOutputStream(file)
    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
    out.close()
    return Uri.fromFile(file)
}
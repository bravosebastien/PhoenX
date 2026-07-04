package com.example.phoenx.ui.screens.library

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.example.phoenx.ui.MainViewModel
import com.example.phoenx.ui.components.InfoButton
import com.example.phoenx.ui.theme.AccentPrimary
import com.example.phoenx.ui.theme.BackgroundPrimary
import com.example.phoenx.ui.theme.TextPrimary
import kotlin.math.cos
import kotlin.math.sin

// ── Couleurs ────────────────────────────────────────────────────────────────
private val Accent     = Color(0xFFC97B3A)
private val AccentSoft = Color(0xFFE8A85F)
private val CardBg     = Color(0xFF2E2E35)
private val CardBorder = Color(0xFF3E3E42)
private val SubText    = Color(0xFF9B9590)
private val BgPrimary  = Color(0xFF1A1A1F)

// ── Modèle d'une carte ──────────────────────────────────────────────────────
private data class LibraryCard(
    val id: String,
    val name: String,
    val route: String
)

// ── Liste des 14 sections ───────────────────────────────────────────────────
private val libraryCards = listOf(
    LibraryCard("bibliotheque",   "Bibliothèque",    "book_viewer_recipient"),
    LibraryCard("discotheque",    "Discothèque",     "library_music"),
    LibraryCard("videotheque",    "Vidéothèque",     "library_video"),
    LibraryCard("fil_pensee",     "Fil de pensée",   "fil_pensee"),
    LibraryCard("lettres",        "Lettres",         "lettres"),
    LibraryCard("mes_meilleurs",  "Mes meilleurs",   "mes_meilleurs"),
    LibraryCard("photos",         "Photos",          "photos"),
    LibraryCard("mappemonde",     "Mappemonde",      "mappemonde"),
    LibraryCard("cent_questions", "100 Questions",   "cent_questions"),
    LibraryCard("coffre_fort",    "Coffre fort",     "coffre_fort"),
    LibraryCard("tiroir_secret",  "Tiroir secret",   "tiroir_secret"),
    LibraryCard("le_pacte",       "Le pacte",        "le_pacte"),
    LibraryCard("portrait",       "Portrait proche", "portrait_proche"),
    LibraryCard("reconciliation", "Réconciliation",  "reconciliation")
)

// ── Écran principal ──────────────────────────────────────────────────────────
@androidx.media3.common.util.UnstableApi
@Composable
fun RecipientLibraryScreen(
    navController: NavController,
    isCreatorMode: Boolean = true,
    viewModel: LibraryCoverViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val covers by viewModel.covers.collectAsState()
    
    // Un seul player partagé pour tout l'écran (Règle d'or : MUET)
    val sharedPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            volume = 0f
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    DisposableEffect(Unit) {
        onDispose { sharedPlayer.release() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // En-tête
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text  = "Ta Bibliothèque",
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontSize   = 28.sp,
                    color      = TextPrimary
                )
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                InfoButton(
                    title = "La Grande Bibliothèque",
                    points = listOf(
                        "14 compartiments, chacun représentant une dimension de ta vie.",
                        "Tape sur une carte pour accéder à son contenu.",
                        "Tu peux personnaliser chaque carte avec une photo ou une vidéo silencieuse.",
                        "Le bouton ✏️ sur chaque carte permet de la personnaliser.",
                        "Tes proches verront cette bibliothèque après l'activation du protocole."
                    )
                )
                IconButton(onClick = { /* TODO */ }) {
                    Icon(
                        imageVector        = Icons.Default.Search,
                        contentDescription = "Rechercher",
                        tint               = Accent,
                        modifier           = Modifier.size(26.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Grille 2 colonnes
        LazyVerticalGrid(
            columns             = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier            = Modifier.fillMaxSize()
        ) {
            items(libraryCards) { card ->
                val cover = covers[card.id]
                LibraryCardItem(
                    card = card,
                    cover = cover,
                    isCreatorMode = isCreatorMode,
                    sharedPlayer = sharedPlayer,
                    onClick = {
                        navController.navigate(card.route)
                    },
                    onEditClick = {
                        navController.navigate("library_cover_picker/${card.id}/${card.name}")
                    }
                )
            }
        }
    }
}

// ── Carte individuelle ───────────────────────────────────────────────────────
@androidx.media3.common.util.UnstableApi
@Composable
private fun LibraryCardItem(
    card: LibraryCard,
    cover: LibraryCover?,
    isCreatorMode: Boolean,
    sharedPlayer: ExoPlayer,
    onClick: () -> Unit,
    onEditClick: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .clickable { onClick() }
            .onGloballyPositioned { coordinates ->
                // Détection de visibilité > 50% pour lancer la vidéo
                val windowBounds = coordinates.parentLayoutCoordinates?.size?.height?.toFloat() ?: 2000f
                val positionY = coordinates.positionInWindow().y
                isVisible = positionY > 0 && positionY < (windowBounds * 0.8f)
            }
    ) {
        // --- COUVERTURE PERSONNALISÉE ---
        if (cover != null && cover.mediaType != "none") {
            if (cover.mediaType == "photo") {
                AsyncImage(
                    model = cover.mediaUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (cover.mediaType == "video") {
                VideoCardContent(
                    url = cover.mediaUrl,
                    isVisible = isVisible,
                    sharedPlayer = sharedPlayer
                )
            }
            // Overlay dégradé pour la lisibilité du texte
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                            startY = 100f
                        )
                    )
            )
        }

        // --- CONTENU (CANVAS OU TEXTE) ---
        Column(
            modifier            = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (cover == null || cover.mediaType == "none") {
                // Illustration Canvas
                Canvas(modifier = Modifier.size(64.dp)) {
                    when (card.id) {
                        "bibliotheque"   -> drawBibliotheque()
                        "discotheque"    -> drawDiscotheque()
                        "videotheque"    -> drawVideotheque()
                        "fil_pensee"     -> drawFilPensee()
                        "lettres"        -> drawLettres()
                        "mes_meilleurs"  -> drawMesMeilleurs()
                        "photos"         -> drawPhotos()
                        "mappemonde"     -> drawMappemonde()
                        "cent_questions" -> drawCentQuestions()
                        "coffre_fort"    -> drawCoffreFort()
                        "tiroir_secret"  -> drawTiroirSecret()
                        "le_pacte"       -> drawLePacte()
                        "portrait"       -> drawPortrait()
                        "reconciliation" -> drawReconciliation()
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            // Nom de la section
            Text(
                text  = card.name,
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontSize   = 14.sp,
                    color      = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Sous-titre
            Text(
                text  = "Découvrir",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize   = 11.sp,
                    color      = Accent
                )
            )
            
            if (cover != null && cover.mediaType != "none") {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // BOUTON PERSONNALISER (Crayon)
        if (isCreatorMode) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { onEditClick() },
                color = BgPrimary.copy(alpha = 0.6f)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Éditer",
                    tint = TextPrimary,
                    modifier = Modifier.padding(8.dp).size(16.dp)
                )
            }
        }
    }
}

@androidx.media3.common.util.UnstableApi
@Composable
private fun VideoCardContent(
    url: String,
    isVisible: Boolean,
    sharedPlayer: ExoPlayer
) {
    LaunchedEffect(isVisible) {
        if (isVisible) {
            sharedPlayer.setMediaItem(MediaItem.fromUri(url))
            sharedPlayer.prepare()
            sharedPlayer.play()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = sharedPlayer
                useController = false
                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}


// ════════════════════════════════════════════════════════════════════════════
// ILLUSTRATIONS CANVAS — une fonction par carte
// ════════════════════════════════════════════════════════════════════════════

// 1. Bibliothèque — livres debout
private fun DrawScope.drawBibliotheque() {
    val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
    val bookW  = 8.dp.toPx()
    val gap    = 4.dp.toPx()
    val bottom = size.height - 8.dp.toPx()
    val heights = listOf(32f, 40f, 36f, 28f)
    val colors  = listOf(Accent, AccentSoft, Accent.copy(alpha = 0.7f), AccentSoft.copy(alpha = 0.6f))
    val totalW  = 4 * bookW + 3 * gap
    var x       = (size.width - totalW) / 2f

    heights.forEachIndexed { i, h ->
        val top = bottom - h.dp.toPx()
        drawRect(
            color   = colors[i],
            topLeft = Offset(x, top),
            size    = Size(bookW, h.dp.toPx())
        )
        x += bookW + gap
    }
    // Ligne sous les livres
    drawLine(
        color       = Accent,
        start       = Offset((size.width - totalW) / 2f - 4.dp.toPx(), bottom),
        end         = Offset((size.width + totalW) / 2f + 4.dp.toPx(), bottom),
        strokeWidth = 1.5.dp.toPx(),
        cap         = StrokeCap.Round
    )
}

// 2. Discothèque — vinyle
private fun DrawScope.drawDiscotheque() {
    val cx = size.width  / 2f
    val cy = size.height / 2f
    val stroke = 1.5.dp.toPx()
    listOf(22f, 15f, 8f).forEach { r ->
        drawCircle(
            color  = Accent,
            radius = r.dp.toPx(),
            center = Offset(cx, cy),
            style  = Stroke(width = stroke)
        )
    }
    drawCircle(color = Accent, radius = 3.dp.toPx(), center = Offset(cx, cy))
    drawLine(
        color       = Accent,
        start       = Offset(cx, cy - 22.dp.toPx() - 4.dp.toPx()),
        end         = Offset(cx, cy - 22.dp.toPx() - 1.dp.toPx()),
        strokeWidth = 2.dp.toPx(),
        cap         = StrokeCap.Round
    )
}

// 3. Vidéothèque — caméra
private fun DrawScope.drawVideotheque() {
    val cx     = size.width  / 2f
    val cy     = size.height / 2f
    val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    val rectW  = 30.dp.toPx()
    val rectH  = 20.dp.toPx()
    val left   = cx - rectW / 2f
    val top    = cy - rectH / 2f
    // Corps caméra
    drawRect(
        color   = Accent,
        topLeft = Offset(left, top),
        size    = Size(rectW, rectH),
        style   = stroke
    )
    // Tête de lecture triangle
    val triL = left + rectW
    val triT = cy - 8.dp.toPx()
    val triB = cy + 8.dp.toPx()
    val path = Path().apply {
        moveTo(triL, triT)
        lineTo(triL + 12.dp.toPx(), cy)
        lineTo(triL, triB)
        close()
    }
    drawPath(path = path, color = Accent, style = stroke)
    // Lignes intérieures
    drawLine(
        color       = Accent.copy(alpha = 0.5f),
        start       = Offset(left + 6.dp.toPx(), cy - 4.dp.toPx()),
        end         = Offset(left + rectW - 6.dp.toPx(), cy - 4.dp.toPx()),
        strokeWidth = 1.dp.toPx()
    )
    drawLine(
        color       = Accent.copy(alpha = 0.5f),
        start       = Offset(left + 6.dp.toPx(), cy + 4.dp.toPx()),
        end         = Offset(left + rectW - 6.dp.toPx(), cy + 4.dp.toPx()),
        strokeWidth = 1.dp.toPx()
    )
}

// 4. Fil de pensée — courbe ascendante avec points
private fun DrawScope.drawFilPensee() {
    val points = listOf(
        Offset(10.dp.toPx(), size.height - 12.dp.toPx()),
        Offset(24.dp.toPx(), size.height - 24.dp.toPx()),
        Offset(38.dp.toPx(), size.height - 36.dp.toPx()),
        Offset(52.dp.toPx(), size.height - 50.dp.toPx())
    )
    val sizes = listOf(3f, 4f, 5f, 6f)
    // Courbe
    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
    }
    drawPath(
        path  = path,
        color = Accent.copy(alpha = 0.4f),
        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
    )
    // Points
    points.forEachIndexed { i, pt ->
        drawCircle(color = Accent, radius = sizes[i].dp.toPx() / 2f, center = pt)
    }
    // Ligne base
    drawLine(
        color       = Color(0xFF3E3E45),
        start       = Offset(4.dp.toPx(), size.height - 8.dp.toPx()),
        end         = Offset(size.width - 4.dp.toPx(), size.height - 8.dp.toPx()),
        strokeWidth = 1.dp.toPx()
    )
}

// 5. Lettres — enveloppe
private fun DrawScope.drawLettres() {
    val cx    = size.width  / 2f
    val cy    = size.height / 2f
    val w     = 36.dp.toPx()
    val h     = 24.dp.toPx()
    val left  = cx - w / 2f
    val top   = cy - h / 2f
    val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawRect(color = Accent, topLeft = Offset(left, top), size = Size(w, h), style = stroke)
    val path = Path().apply {
        moveTo(left, top)
        lineTo(cx, cy - 2.dp.toPx())
        lineTo(left + w, top)
    }
    drawPath(path = path, color = Accent, style = stroke)
}

// 6. Mes meilleurs — étoile
private fun DrawScope.drawMesMeilleurs() {
    val cx     = size.width  / 2f
    val cy     = size.height / 2f
    val outer  = 22.dp.toPx()
    val inner  = 10.dp.toPx()
    val path   = Path()
    for (i in 0 until 10) {
        val angle  = Math.PI / 2 + i * Math.PI / 5
        val radius = if (i % 2 == 0) outer else inner
        val x      = cx + (radius * cos(angle)).toFloat()
        val y      = cy - (radius * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path = path, color = Accent.copy(alpha = 0.15f))
    drawPath(
        path  = path,
        color = Accent,
        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

// 7. Photos — appareil photo
private fun DrawScope.drawPhotos() {
    val cx     = size.width  / 2f
    val cy     = size.height / 2f
    val w      = 36.dp.toPx()
    val h      = 26.dp.toPx()
    val left   = cx - w / 2f
    val top    = cy - h / 2f
    val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
    drawRect(color = Accent, topLeft = Offset(left, top), size = Size(w, h), style = stroke)
    drawCircle(color = Accent, radius = 9.dp.toPx(), center = Offset(cx, cy), style = stroke)
    drawCircle(color = Accent.copy(alpha = 0.4f), radius = 3.dp.toPx(), center = Offset(cx, cy))
    drawRect(
        color   = Accent.copy(alpha = 0.5f),
        topLeft = Offset(left + w - 10.dp.toPx(), top + 3.dp.toPx()),
        size    = Size(6.dp.toPx(), 4.dp.toPx()),
        style   = stroke
    )
}

// 8. Mappemonde — globe
private fun DrawScope.drawMappemonde() {
    val cx     = size.width  / 2f
    val cy     = size.height / 2f
    val r      = 20.dp.toPx()
    val stroke = Stroke(width = 1.5.dp.toPx())
    drawCircle(color = Accent, radius = r, center = Offset(cx, cy), style = stroke)
    drawOval(
        color  = Accent.copy(alpha = 0.5f),
        topLeft = Offset(cx - 8.dp.toPx(), cy - r),
        size   = Size(16.dp.toPx(), r * 2),
        style  = stroke
    )
    listOf(-6f, 0f, 6f).forEach { dy ->
        drawLine(
            color       = Accent.copy(alpha = 0.3f),
            start       = Offset(cx - r + 2.dp.toPx(), cy + dy.dp.toPx()),
            end         = Offset(cx + r - 2.dp.toPx(), cy + dy.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )
    }
    drawCircle(color = Accent, radius = 2.5.dp.toPx(), center = Offset(cx + 8.dp.toPx(), cy - 10.dp.toPx()))
}

// 9. Cent questions — point d'interrogation
private fun DrawScope.drawCentQuestions() {
    val cx     = size.width  / 2f
    val cy     = size.height / 2f
    drawCircle(
        color  = Accent.copy(alpha = 0.25f),
        radius = 22.dp.toPx(),
        center = Offset(cx, cy),
        style  = Stroke(width = 1.dp.toPx())
    )
    val path = Path().apply {
        moveTo(cx - 7.dp.toPx(), cy - 8.dp.toPx())
        cubicTo(
            cx - 7.dp.toPx(), cy - 16.dp.toPx(),
            cx + 7.dp.toPx(), cy - 16.dp.toPx(),
            cx + 7.dp.toPx(), cy - 8.dp.toPx()
        )
        cubicTo(
            cx + 7.dp.toPx(), cy - 2.dp.toPx(),
            cx, cy - 2.dp.toPx(),
            cx, cy + 4.dp.toPx()
        )
    }
    drawPath(
        path  = path,
        color = Accent,
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    )
    drawCircle(color = Accent, radius = 2.dp.toPx(), center = Offset(cx, cy + 10.dp.toPx()))
}

// 10. Coffre fort — cadran
private fun DrawScope.drawCoffreFort() {
    val cx     = size.width  / 2f
    val cy     = size.height / 2f
    val w      = 34.dp.toPx()
    val h      = 28.dp.toPx()
    val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
    drawRect(color = Accent, topLeft = Offset(cx - w/2f, cy - h/2f), size = Size(w, h), style = stroke)
    val cr = 9.dp.toPx()
    drawCircle(color = Accent, radius = cr, center = Offset(cx - 4.dp.toPx(), cy), style = stroke)
    drawCircle(color = Accent, radius = 3.dp.toPx(), center = Offset(cx - 4.dp.toPx(), cy), style = stroke)
    listOf(0f, 90f, 180f, 270f).forEach { angle ->
        val rad = Math.toRadians(angle.toDouble())
        val x1  = cx - 4.dp.toPx() + (cr - 3.dp.toPx()) * cos(rad).toFloat()
        val y1  = cy              + (cr - 3.dp.toPx()) * sin(rad).toFloat()
        val x2  = cx - 4.dp.toPx() + cr * cos(rad).toFloat()
        val y2  = cy              + cr * sin(rad).toFloat()
        drawLine(color = Accent, start = Offset(x1, y1), end = Offset(x2, y2), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
    }
    drawRect(
        color   = Accent.copy(alpha = 0.4f),
        topLeft = Offset(cx + w/2f - 8.dp.toPx(), cy - 5.dp.toPx()),
        size    = Size(8.dp.toPx(), 10.dp.toPx())
    )
}

// 11. Tiroir secret — commode
private fun DrawScope.drawTiroirSecret() {
    val cx     = size.width  / 2f
    val cy     = size.height / 2f
    val w      = 34.dp.toPx()
    val h      = 28.dp.toPx()
    val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
    drawRect(color = Accent, topLeft = Offset(cx - w/2f, cy - h/2f), size = Size(w, h), style = stroke)
    val t1 = cy - h/2f + 3.dp.toPx()
    val t2 = cy + 1.dp.toPx()
    listOf(Pair(t1, Accent.copy(alpha = 0.4f)), Pair(t2, Accent.copy(alpha = 0.8f))).forEach { (top, color) ->
        drawRect(
            color   = color,
            topLeft = Offset(cx - w/2f + 4.dp.toPx(), top),
            size    = Size(w - 8.dp.toPx(), 10.dp.toPx()),
            style   = Stroke(width = 1.dp.toPx())
        )
    }
    drawCircle(color = Accent, radius = 2.dp.toPx(), center = Offset(cx, t2 + 5.dp.toPx()))
}

// 12. Le pacte — deux silhouettes reliées
private fun DrawScope.drawLePacte() {
    val cy     = size.height / 2f
    val r      = 7.dp.toPx()
    val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
    val left   = size.width * 0.28f
    val right  = size.width * 0.72f
    drawCircle(color = Accent, radius = r, center = Offset(left, cy - 5.dp.toPx()), style = stroke)
    drawCircle(color = Accent, radius = r, center = Offset(right, cy - 5.dp.toPx()), style = stroke)
    val path = Path().apply {
        moveTo(left, cy + 10.dp.toPx())
        cubicTo(left, cy + 20.dp.toPx(), right, cy + 20.dp.toPx(), right, cy + 10.dp.toPx())
    }
    drawPath(path = path, color = Accent, style = stroke)
    val midX = (left + right) / 2f
    for (i in 0..4) {
        val x = left + (right - left) * i / 4f
        if (i % 2 == 0) drawCircle(color = Accent.copy(alpha = 0.4f), radius = 1.5.dp.toPx(), center = Offset(x, cy - 5.dp.toPx()))
    }
}

// 13. Portrait proche — silhouette dans cadre
private fun DrawScope.drawPortrait() {
    val cx     = size.width  / 2f
    val cy     = size.height / 2f
    val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
    drawCircle(color = Accent, radius = 8.dp.toPx(), center = Offset(cx, cy - 10.dp.toPx()), style = stroke)
    val path = Path().apply {
        moveTo(cx - 14.dp.toPx(), cy + 16.dp.toPx())
        cubicTo(cx - 14.dp.toPx(), cy + 2.dp.toPx(), cx + 14.dp.toPx(), cy + 2.dp.toPx(), cx + 14.dp.toPx(), cy + 16.dp.toPx())
    }
    drawPath(path = path, color = Accent, style = stroke)
    val cornerSize = 5.dp.toPx()
    val pad        = 4.dp.toPx()
    listOf(
        Pair(Offset(cx - 24.dp.toPx(), cy - 24.dp.toPx()), Pair(true, true)),
        Pair(Offset(cx + 24.dp.toPx(), cy - 24.dp.toPx()), Pair(false, true)),
        Pair(Offset(cx - 24.dp.toPx(), cy + 24.dp.toPx()), Pair(true, false)),
        Pair(Offset(cx + 24.dp.toPx(), cy + 24.dp.toPx()), Pair(false, false))
    ).forEach { (pos, dirs) ->
        val (left, top) = dirs
        val xSign = if (left) 1f else -1f
        val ySign = if (top)  1f else -1f
        drawLine(color = Accent.copy(alpha = 0.5f), start = pos, end = Offset(pos.x + xSign * cornerSize, pos.y), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color = Accent.copy(alpha = 0.5f), start = pos, end = Offset(pos.x, pos.y + ySign * cornerSize), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
    }
}

// 14. Réconciliation — lettre avec sceau
private fun DrawScope.drawReconciliation() {
    val cx     = size.width  / 2f
    val cy     = size.height / 2f
    val w      = 30.dp.toPx()
    val h      = 22.dp.toPx()
    val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
    drawRect(
        color   = Color(0xFFF2EDE8).copy(alpha = 0.06f),
        topLeft = Offset(cx - w/2f, cy - h/2f),
        size    = Size(w, h)
    )
    drawRect(color = Accent, topLeft = Offset(cx - w/2f, cy - h/2f), size = Size(w, h), style = stroke)
    drawLine(color = Accent.copy(alpha = 0.3f), start = Offset(cx, cy - h/2f), end = Offset(cx, cy + h/2f), strokeWidth = 1.dp.toPx())
    drawLine(color = Accent.copy(alpha = 0.3f), start = Offset(cx - w/2f, cy), end = Offset(cx + w/2f, cy), strokeWidth = 1.dp.toPx())
    drawCircle(color = Accent.copy(alpha = 0.85f), radius = 5.dp.toPx(), center = Offset(cx, cy))
    drawCircle(color = Accent, radius = 5.dp.toPx(), center = Offset(cx, cy), style = Stroke(width = 1.dp.toPx()))
}

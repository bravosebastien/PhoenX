package com.example.phoenx.ui.screens.book

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.phoenx.data.local.OfflineEntry
import com.example.phoenx.data.media.MediaManager
import com.example.phoenx.ui.components.SecureAsyncImage
import com.example.phoenx.ui.theme.LocalAccentColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue

// v9.8.14 : Limite de photos par chapitre
private const val MAX_PHOTOS_PER_CHAPTER = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookReaderFlowScreen(
    navController: NavController,
    targetCreatorId: String? = null,
    simulatedRecipientUid: String? = null,
    viewModel: BookViewerViewModel = hiltViewModel()
) {
    val bookDraft by viewModel.bookDraft.collectAsState()
    val decryptedChapters by viewModel.decryptedChapters.collectAsState()
    val decryptedIntro by viewModel.decryptedGlobalIntro.collectAsState()
    val mediaMap by viewModel.mediaMap.collectAsState()
    val isLoadingData by viewModel.isLoading.collectAsState()
    val scrollProgress by viewModel.scrollProgress.collectAsState()
    val pagesProgress by viewModel.pagesProgress.collectAsState()
    val fontSizeScale by viewModel.fontSizeScale.collectAsState()
    val ambiance by viewModel.ambiance.collectAsState()
    val readingMode by viewModel.readingMode.collectAsState()

    val fontFamily = BookThemeOptions.getFont(ambiance.fontId)
    val background = BookThemeOptions.getBackground(ambiance.backgroundId)
    val textColor = if (background.darkText) Color(0xFF1A1A1A) else Color(0xFFF2EDE8)
    val accent = LocalAccentColor.current

    val coroutineScope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    var pages by remember { mutableStateOf<List<BookPage>>(emptyList()) }
    var isPaginating by remember { mutableStateOf(false) }

    // v9.8.14 : Dialogue d'exclusion photo
    var photoToExclude by remember { mutableStateOf<OfflineEntry?>(null) }
    val isCreator = targetCreatorId == null || simulatedRecipientUid != null

    LaunchedEffect(targetCreatorId, simulatedRecipientUid) {
        viewModel.loadBook(targetCreatorId, simulatedRecipientUid)
    }

    val systemBarsInsets = WindowInsets.systemBars.asPaddingValues()
    val topInset = systemBarsInsets.calculateTopPadding()
    val bottomInset = systemBarsInsets.calculateBottomPadding()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val availableWidth = with(density) { (maxWidth - 64.dp).toPx() }
        val dynamicDeduction = topInset + bottomInset + 196.dp
        val availableHeight = with(density) { (maxHeight - dynamicDeduction).toPx() }

        LaunchedEffect(bookDraft, decryptedChapters, decryptedIntro, mediaMap, fontSizeScale, ambiance.fontId, availableWidth, availableHeight) {
            val draft = bookDraft ?: return@LaunchedEffect
            if (decryptedChapters.isEmpty() && draft.chapters.isNotEmpty()) return@LaunchedEffect

            isPaginating = true
            withContext(Dispatchers.Default) {
                val newPages = mutableListOf<BookPage>()
                var currentAtoms = mutableListOf<BookAtom>()
                var currentY = 0f
                var currentPageFirstOffset = 0

                val bodyStyle = TextStyle(
                    fontFamily = fontFamily,
                    fontSize = (18 * fontSizeScale).sp,
                    lineHeight = (32 * fontSizeScale).sp,
                    color = textColor
                )

                fun flush(chapterId: String?) {
                    if (currentAtoms.isNotEmpty()) {
                        newPages.add(BookPage(currentAtoms.toList(), newPages.size + 1, chapterId, currentPageFirstOffset))
                        currentAtoms.clear()
                        currentY = 0f
                    }
                }

                // 1. COUVERTURE
                currentAtoms.add(BookAtom.Cover(draft.bookTitle ?: "Livre de Vie", viewModel.creatorName.value))
                flush("cover")

                // 2. INTRODUCTION
                if (decryptedIntro.isNotEmpty()) {
                    currentPageFirstOffset = 0
                    currentAtoms.add(BookAtom.Text(decryptedIntro, 0, isItalic = true))
                    flush("intro")
                }

                // 3. CHAPITRES
                draft.chapters.sortedBy { it.orderIndex }.forEach { chapter ->
                    flush(chapter.id)

                    val content = decryptedChapters[chapter.id] ?: ""
                    val regex = Regex("\\[(PHOTO|AUDIO):([a-f0-9\\-]+)\\]")
                    val parts = content.split(regex)
                    val matches = regex.findAll(content).toList()

                    var globalCharOffset = 0
                    currentPageFirstOffset = 0
                    var photosInChapter = 0
                    var lastWasPhoto = false

                    val headerHeight = with(density) { (26 * fontSizeScale + 60).dp.toPx() }
                    currentAtoms.add(BookAtom.ChapterHeader(chapter.title, chapter.orderIndex))
                    currentY += headerHeight

                    parts.forEachIndexed { index, part ->
                        if (part.isNotBlank()) {
                            var remainingText = part.trim()
                            var blockOffset = 0
                            lastWasPhoto = false

                            while (remainingText.isNotEmpty()) {
                                if (currentAtoms.isEmpty()) currentPageFirstOffset = globalCharOffset + blockOffset

                                val layout = textMeasurer.measure(
                                    AnnotatedString(remainingText),
                                    style = bodyStyle,
                                    constraints = androidx.compose.ui.unit.Constraints(maxWidth = availableWidth.toInt())
                                )

                                val spaceLeft = availableHeight - currentY
                                if (layout.size.height <= spaceLeft) {
                                    currentAtoms.add(BookAtom.Text(remainingText, globalCharOffset + blockOffset))
                                    currentY += layout.size.height + with(density) { 12.dp.toPx() }
                                    remainingText = ""
                                } else {
                                    if (spaceLeft < with(density) { 64.dp.toPx() }) {
                                        flush(chapter.id)
                                    } else {
                                        val lineIndex = layout.getLineForVerticalPosition(spaceLeft)
                                        val cutIndex = if (lineIndex > 0) layout.getLineEnd(lineIndex - 1) else 0

                                        if (cutIndex > 0) {
                                            val pageText = remainingText.substring(0, cutIndex)
                                            currentAtoms.add(BookAtom.Text(pageText, globalCharOffset + blockOffset))
                                            remainingText = remainingText.substring(cutIndex).trim()
                                            blockOffset += cutIndex
                                        }
                                        flush(chapter.id)
                                    }
                                }
                            }
                        }
                        globalCharOffset += part.length

                        // Illustration
                        if (index < matches.size) {
                            val type = matches[index].groupValues[1]
                            val id = matches[index].groupValues[2]
                            val entry = mediaMap[id]

                            // v9.8.14 : Triplé de filtres (1. Explicitement inclus, 2. Non-consécutif, 3. Max 3 par chapitre)
                            if (type == "PHOTO" && entry != null && entry.includedInBook) {
                                if (!lastWasPhoto && photosInChapter < MAX_PHOTOS_PER_CHAPTER) {
                                    lastWasPhoto = true
                                    photosInChapter++

                                    var ratio = 1f
                                    if (!entry.localMediaPath.isNullOrBlank()) {
                                        val file = java.io.File(entry.localMediaPath)
                                        if (file.exists()) {
                                            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                            BitmapFactory.decodeFile(file.absolutePath, options)

                                            if (options.outWidth > 0 && options.outHeight > 0) {
                                                var w = options.outWidth.toFloat()
                                                var h = options.outHeight.toFloat()

                                                try {
                                                    val exif = android.media.ExifInterface(file.absolutePath)
                                                    val orientation = exif.getAttributeInt(
                                                        android.media.ExifInterface.TAG_ORIENTATION,
                                                        android.media.ExifInterface.ORIENTATION_NORMAL
                                                    )
                                                    if (orientation == android.media.ExifInterface.ORIENTATION_ROTATE_90 ||
                                                        orientation == android.media.ExifInterface.ORIENTATION_ROTATE_270) {
                                                        val temp = w
                                                        w = h
                                                        h = temp
                                                    }
                                                } catch (e: Exception) {
                                                    android.util.Log.w("PHOENX_BOOK", "Erreur lecture EXIF: ${e.message}")
                                                }

                                                ratio = w / h
                                            }
                                        }
                                    }

                                    val photoTargetWidth = availableWidth * 0.6f
                                    var photoTargetHeight = photoTargetWidth / ratio
                                    photoTargetHeight = minOf(photoTargetHeight, availableHeight * 0.5f)

                                    val photoTotalBudget = photoTargetHeight + with(density) { 48.dp.toPx() }

                                    if (availableHeight - currentY < photoTotalBudget) flush(chapter.id)

                                    if (currentAtoms.isEmpty()) currentPageFirstOffset = globalCharOffset
                                    currentAtoms.add(BookAtom.Photo(entry, photoTargetWidth, photoTargetHeight))
                                    currentY += photoTotalBudget

                                    if (availableHeight - currentY < with(density) { 60.dp.toPx() }) flush(chapter.id)
                                }
                            }
                            globalCharOffset += matches[index].value.length
                        }
                    }
                    flush(chapter.id)
                }
                flush(null)
                pages = newPages
                isPaginating = false
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(bookDraft?.bookTitle ?: "Livre de Vie", fontFamily = fontFamily) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = textColor)
                        }
                    },
                    actions = {
                        var showComfortMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showComfortMenu = true }) {
                            Icon(Icons.Default.TextFields, "Confort", tint = textColor)
                        }
                        DropdownMenu(
                            expanded = showComfortMenu,
                            onDismissRequest = { showComfortMenu = false },
                            containerColor = background.color,
                        ) {
                            Text("Mode de lecture", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.6f))
                            Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                                FilterChip(
                                    selected = readingMode == BookViewerViewModel.BookReadingMode.SCROLL,
                                    onClick = { viewModel.updateReadingMode(BookViewerViewModel.BookReadingMode.SCROLL) },
                                    label = { Text("Défilement") },
                                    leadingIcon = { Icon(Icons.Default.FormatAlignJustify, null, modifier = Modifier.size(16.dp)) }
                                )
                                Spacer(Modifier.width(8.dp))
                                FilterChip(
                                    selected = readingMode == BookViewerViewModel.BookReadingMode.PAGES,
                                    onClick = { viewModel.updateReadingMode(BookViewerViewModel.BookReadingMode.PAGES) },
                                    label = { Text("Pages") },
                                    leadingIcon = { Icon(Icons.Default.AutoStories, null, modifier = Modifier.size(16.dp)) }
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = textColor.copy(alpha = 0.1f))
                            Text("Taille du texte", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.6f))
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
                                IconButton(onClick = { viewModel.updateFontSize(fontSizeScale - 0.1f) }) { Icon(Icons.Default.Remove, null, tint = textColor) }
                                Text("${(fontSizeScale * 100).toInt()}%", color = textColor)
                                IconButton(onClick = { viewModel.updateFontSize(fontSizeScale + 0.1f) }) { Icon(Icons.Default.Add, null, tint = textColor) }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = background.color.copy(alpha = 0.95f), titleContentColor = textColor)
                )
            },
            containerColor = background.color
        ) { padding ->
            if (isLoadingData || (readingMode == BookViewerViewModel.BookReadingMode.PAGES && isPaginating)) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = accent)
                        if (isPaginating) {
                            Spacer(Modifier.height(16.dp))
                            Text("Mise en page...", style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.6f))
                        }
                    }
                }
            } else {
                if (readingMode == BookViewerViewModel.BookReadingMode.SCROLL) {
                    ScrollModeView(padding, bookDraft, decryptedChapters, decryptedIntro, mediaMap, viewModel, listState = rememberLazyListState(), scrollProgress, fontSizeScale, fontFamily, textColor, accent, targetCreatorId, navController, onMediaExclude = { entry -> photoToExclude = entry })
                } else {
                    PagesModeView(padding, pages, pagesProgress, viewModel, targetCreatorId, bookDraft, fontSizeScale, fontFamily, background, textColor, accent, navController, isCreator = isCreator, onMediaExclude = { entry -> photoToExclude = entry })
                }
            }
        }

        // v9.8.14 : Dialogue de confirmation pour retrait de photo
        if (photoToExclude != null) {
            AlertDialog(
                onDismissRequest = { photoToExclude = null },
                title = { Text("Retirer cette photo de votre Livre ?") },
                text = { Text("Cette photo ne réapparaîtra plus dans votre Livre de Vie à l'avenir.") },
                confirmButton = {
                    Button(
                        onClick = {
                            val entry = photoToExclude
                            if (entry != null) {
                                viewModel.excludePhotoFromBook(entry.id)
                            }
                            photoToExclude = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Retirer")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { photoToExclude = null }) {
                        Text("Annuler")
                    }
                }
            )
        }
    }
}

@Composable
fun ScrollModeView(
    padding: PaddingValues,
    bookDraft: com.example.phoenx.data.model.BookDraft?,
    decryptedChapters: Map<String, String>,
    decryptedIntro: String,
    mediaMap: Map<String, OfflineEntry>,
    viewModel: BookViewerViewModel,
    listState: androidx.compose.foundation.lazy.LazyListState,
    scrollProgress: ScrollPosition?,
    fontSizeScale: Float,
    fontFamily: FontFamily,
    textColor: Color,
    accent: Color,
    targetCreatorId: String?,
    navController: NavController,
    onMediaExclude: ((OfflineEntry) -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    var showResumeBanner by remember { mutableStateOf(false) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collectLatest { (index, offset) ->
                delay(2000)
                if (index > 0) {
                    val userId = targetCreatorId ?: bookDraft?.userId
                    if (userId != null) viewModel.saveScrollProgress(userId, index, offset)
                }
            }
    }

    LaunchedEffect(scrollProgress) {
        if (scrollProgress != null && scrollProgress.itemIndex > 0) showResumeBanner = true
    }

    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp, start = 28.dp, end = 28.dp, top = 20.dp)
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = bookDraft?.bookTitle ?: "Livre de Vie", style = TextStyle(fontFamily = fontFamily, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = textColor), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text(text = "par ${viewModel.creatorName.collectAsState().value}", style = TextStyle(fontFamily = fontFamily, fontSize = 14.sp, fontWeight = FontWeight.Light, color = textColor.copy(alpha = 0.6f)))
                    Spacer(Modifier.height(40.dp))
                    HorizontalDivider(modifier = Modifier.width(60.dp), thickness = 1.dp, color = accent.copy(alpha = 0.4f))
                }
            }

            if (decryptedIntro.isNotEmpty()) {
                item {
                    Text(text = decryptedIntro, style = TextStyle(fontFamily = fontFamily, fontSize = (18 * fontSizeScale).sp, lineHeight = (32 * fontSizeScale).sp, color = textColor, fontStyle = FontStyle.Italic), modifier = Modifier.padding(bottom = 60.dp))
                }
            }

            bookDraft?.chapters?.sortedBy { it.orderIndex }?.forEach { chapter ->
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "Chapitre ${chapter.orderIndex + 1}", style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 11.sp, color = accent.copy(alpha = 0.7f), letterSpacing = 2.sp))
                        Spacer(Modifier.height(8.dp))
                        Text(text = chapter.title, style = TextStyle(fontFamily = fontFamily, fontSize = (26 * fontSizeScale).sp, fontWeight = FontWeight.Bold, color = textColor) )
                        Spacer(Modifier.height(32.dp))

                        val content = decryptedChapters[chapter.id] ?: ""
                        ReaderIllustrableText(
                            text = content, 
                            mediaMap = mediaMap, 
                            mediaManager = viewModel.mediaManager, 
                            fontFamily = fontFamily, 
                            textColor = textColor, 
                            accent = accent, 
                            fontSizeScale = fontSizeScale, 
                            creatorId = targetCreatorId ?: bookDraft?.userId, 
                            onMediaClick = { entry ->
                                navController.navigate(com.example.phoenx.ui.navigation.Screen.MediaViewer.createRoute(entry.id, targetCreatorId ?: bookDraft?.userId, entry.mediaUrl, entry.entryType, entry.aiSummary, "entries", null, entry.mediaUrl?.contains(".enc") ?: true))
                            },
                            onMediaExclude = onMediaExclude
                        )

                        Spacer(Modifier.height(60.dp))
                        HorizontalDivider(modifier = Modifier.fillMaxWidth(0.3f).align(Alignment.CenterHorizontally), color = textColor.copy(alpha = 0.1f))
                        Spacer(Modifier.height(60.dp))
                    }
                }
            }
        }

        if (showResumeBanner) {
            Card(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = accent)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Reprendre défilement ?", color = Color.White, fontWeight = FontWeight.Bold)
                    Row {
                        TextButton(onClick = { showResumeBanner = false }) { Text("Ignorer", color = Color.White.copy(alpha = 0.7f)) }
                        Button(onClick = {
                            showResumeBanner = false
                            coroutineScope.launch { scrollProgress?.let { p ->
                                val ratio = fontSizeScale / p.savedAtScale
                                listState.scrollToItem(p.itemIndex, (p.offset * ratio).toInt())
                            } }
                        }, colors = ButtonDefaults.buttonColors(containerColor = Color.White)) { Text("Reprendre", color = accent) }
                    }
                }
            }
        }
    }
}

@Composable
fun PagesModeView(
    padding: PaddingValues,
    pages: List<BookPage>,
    pagesProgress: PagesPosition?,
    viewModel: BookViewerViewModel,
    targetCreatorId: String?,
    bookDraft: com.example.phoenx.data.model.BookDraft?,
    fontSizeScale: Float,
    fontFamily: FontFamily,
    background: BookBackgroundOption,
    textColor: Color,
    accent: Color,
    navController: NavController,
    isCreator: Boolean = false,
    onMediaExclude: ((OfflineEntry) -> Unit)? = null
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val density = LocalDensity.current

    val returnToPage by navController.currentBackStackEntry?.savedStateHandle
        ?.getStateFlow<Int?>("returnToPage", null)
        ?.collectAsState() ?: remember { mutableStateOf(null) }

    LaunchedEffect(pages, returnToPage) {
        if (pages.isNotEmpty() && returnToPage != null) {
            if (returnToPage!! < pages.size) {
                pagerState.scrollToPage(returnToPage!!)
            }
            navController.currentBackStackEntry?.savedStateHandle?.remove<Int>("returnToPage")
        }
    }

    LaunchedEffect(pages, pagesProgress) {
        if (pages.isNotEmpty() && pagesProgress != null) {
            val targetPage = pages.findLast { it.chapterId == pagesProgress.chapterId && it.firstCharOffset <= pagesProgress.characterOffset }
            targetPage?.let { pagerState.scrollToPage(it.pageNumber - 1) }
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pages.isNotEmpty() && pagerState.currentPage < pages.size) {
            delay(2000)
            val page = pages[pagerState.currentPage]
            val userId = targetCreatorId ?: bookDraft?.userId
            if (userId != null) viewModel.savePagesProgress(userId, page.chapterId, page.firstCharOffset)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 48.dp),
        pageSpacing = 16.dp
    ) { pageIndex ->
        val page = pages[pageIndex]
        Box(modifier = Modifier
            .fillMaxSize()
            .background(background.color)
            .graphicsLayer {
                val pageOffset = ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction)
                if (pageOffset != 0f) {
                    transformOrigin = TransformOrigin(pivotFractionX = if (pageOffset > 0) 0f else 1f, pivotFractionY = 0.5f)
                    rotationY = pageOffset * -45f
                    cameraDistance = 12f * density.density
                }
                clip = true
            }
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp)) {
                page.atoms.forEach { atom ->
                    when (atom) {
                        is BookAtom.Cover -> BookCoverView(atom.title, atom.author, fontFamily, textColor, accent)
                        is BookAtom.ChapterHeader -> {
                            Text(text = "Chapitre ${atom.index + 1}", style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 11.sp, color = accent.copy(alpha = 0.7f), letterSpacing = 2.sp))
                            Spacer(Modifier.height(8.dp))
                            Text(text = atom.title, style = TextStyle(fontFamily = fontFamily, fontSize = (26 * fontSizeScale).sp, fontWeight = FontWeight.Bold, color = textColor))
                            Spacer(Modifier.height(32.dp))
                        }
                        is BookAtom.Text -> Text(text = atom.content, style = TextStyle(fontFamily = fontFamily, fontSize = (18 * fontSizeScale).sp, color = textColor, lineHeight = (32 * fontSizeScale).sp, fontStyle = if (atom.isItalic) FontStyle.Italic else FontStyle.Normal), modifier = Modifier.padding(bottom = 12.dp))
                        is BookAtom.Photo -> {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(vertical = 24.dp)
                                    .size(width = with(density) { atom.widthPx.toDp() }, height = with(density) { atom.heightPx.toDp() })
                            ) {
                                SecureAsyncImage(
                                    mediaUrl = atom.entry.mediaUrl,
                                    localPath = atom.entry.localMediaPath,
                                    mediaManager = viewModel.mediaManager,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .border(0.5.dp, textColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            navController.currentBackStackEntry?.savedStateHandle?.set("returnToPage", pageIndex)
                                            navController.navigate(com.example.phoenx.ui.navigation.Screen.MediaViewer.createRoute(atom.entry.id, targetCreatorId ?: bookDraft?.userId, atom.entry.mediaUrl, atom.entry.entryType, atom.entry.aiSummary, "entries", null, atom.entry.mediaUrl?.contains(".enc") ?: true))
                                        },
                                    contentScale = ContentScale.Crop,
                                    creatorId = targetCreatorId ?: bookDraft?.userId,
                                    docType = "entries",
                                    docId = atom.entry.id,
                                    hideIfEmpty = true,
                                    isEncrypted = atom.entry.mediaUrl?.contains(".enc") ?: true
                                )
                                // v9.8.14 : Icône discrète d'exclusion pour le Créateur
                                if (isCreator && onMediaExclude != null) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(24.dp)
                                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                            .clickable { onMediaExclude(atom.entry) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Retirer du livre", tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Text(text = "${pageIndex + 1}", modifier = Modifier.align(Alignment.BottomCenter), style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.3f))
        }
    }
}

@Composable
fun BookCoverView(title: String, author: String, fontFamily: FontFamily, textColor: Color, accent: Color) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(text = title, style = TextStyle(fontFamily = fontFamily, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = textColor), textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Text(text = "par $author", style = TextStyle(fontFamily = fontFamily, fontSize = 16.sp, fontWeight = FontWeight.Light, color = textColor.copy(alpha = 0.6f)))
        Spacer(Modifier.height(48.dp))
        Box(modifier = Modifier.width(60.dp).height(1.dp).background(accent.copy(alpha = 0.4f)))
    }
}

@Composable
fun ReaderIllustrableText(
    text: String, 
    mediaMap: Map<String, OfflineEntry>, 
    mediaManager: MediaManager, 
    fontFamily: FontFamily, 
    textColor: Color, 
    accent: Color, 
    fontSizeScale: Float, 
    creatorId: String? = null, 
    onMediaClick: (OfflineEntry) -> Unit,
    onMediaExclude: ((OfflineEntry) -> Unit)? = null
) {
    val (parts, matches) = splitTextAndMediaTags(text)
    var lastWasPhoto = false
    var photosInChapter = 0

    Column {
        parts.forEachIndexed { index, part ->
            if (part.trim().isNotEmpty()) {
                lastWasPhoto = false
                Text(text = part.trim(), style = TextStyle(fontFamily = fontFamily, fontSize = (18 * fontSizeScale).sp, color = textColor, lineHeight = (32 * fontSizeScale).sp), modifier = Modifier.padding(vertical = 12.dp))
            }
            if (index < matches.size) {
                val type = matches[index].groupValues[1]
                val id = matches[index].groupValues[2]
                val entry = mediaMap[id]

                // v9.8.14 : Triplé de filtres (Inclus, Non-consécutif, Max 3 par chapitre)
                if (type == "PHOTO" && entry != null && entry.includedInBook) {
                    if (!lastWasPhoto && photosInChapter < MAX_PHOTOS_PER_CHAPTER) {
                        Box(modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 24.dp)) {
                            SecureAsyncImage(
                                mediaUrl = entry.mediaUrl, 
                                localPath = entry.localMediaPath, 
                                mediaManager = mediaManager, 
                                modifier = Modifier
                                    .fillMaxWidth(0.6f)
                                    .aspectRatio(1f)
                                    .border(0.5.dp, textColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onMediaClick(entry) }, 
                                contentScale = ContentScale.Crop, 
                                creatorId = creatorId, 
                                docType = "entries", 
                                docId = entry.id, 
                                hideIfEmpty = true
                            )
                            if (onMediaExclude != null) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(24.dp)
                                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                        .clickable { onMediaExclude(entry) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Retirer du livre", tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        lastWasPhoto = true
                        photosInChapter++
                    }
                }
            }
        }
    }
}

// v9.8.17 : Découpage propre préservant la ponctuation immédiatement rattachée aux balises média
private fun splitTextAndMediaTags(content: String): Pair<List<String>, List<MatchResult>> {
    val regex = Regex("\\[(PHOTO|AUDIO):([a-f0-9\\-]+)\\]")
    val matches = regex.findAll(content).toList()
    val rawParts = content.split(regex).toMutableList()

    val punctuationRegex = Regex("^([,.:;!\\?…])")
    for (i in matches.indices) {
        if (i + 1 < rawParts.size) {
            val nextPart = rawParts[i + 1]
            val pMatch = punctuationRegex.find(nextPart)
            if (pMatch != null) {
                val punct = pMatch.value
                rawParts[i] = rawParts[i] + punct
                rawParts[i + 1] = nextPart.substring(punct.length)
            }
        }
    }
    return Pair(rawParts, matches)
}

sealed class BookAtom {
    data class Cover(val title: String, val author: String) : BookAtom()
    data class ChapterHeader(val title: String, val index: Int) : BookAtom()
    data class Text(val content: String, val charOffset: Int, val isItalic: Boolean = false) : BookAtom()
    data class Photo(val entry: OfflineEntry, val widthPx: Float = 0f, val heightPx: Float = 0f) : BookAtom()
}

data class BookPage(val atoms: List<BookAtom>, val pageNumber: Int, val chapterId: String?, val firstCharOffset: Int = 0)

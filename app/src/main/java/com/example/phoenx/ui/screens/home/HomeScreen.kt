package com.example.phoenx.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.ui.theme.*

@Composable
fun HomeScreen(
    onNavigateToCapture: (String) -> Unit,
    onNavigateToFil: () -> Unit,
    onNavigateToLetters: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = BackgroundPrimary,
        bottomBar = { PhoenXBottomBar() }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(BackgroundSecondary, BackgroundPrimary),
                        radius = 2000f
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                HomeHeader(uiState.userName, uiState.currentDate, onNavigateToSettings)

                Spacer(modifier = Modifier.height(24.dp))

                ImpulseSection(onNavigateToCapture)

                Spacer(modifier = Modifier.height(48.dp))

                TimelinePreviewCard(uiState.entryCount, uiState.minAge, uiState.currentAge, onNavigateToFil)

                Spacer(modifier = Modifier.height(24.dp))

                YoungSelfLetterCard(onNavigateToLetters)

                Spacer(modifier = Modifier.height(48.dp))

                BiographerQuestionSection(uiState.biographerQuestion)

                Spacer(modifier = Modifier.height(24.dp))

                LatestMemoriesSection()

                Spacer(modifier = Modifier.height(40.dp))

                ProofOfLifeBadge(uiState.lastProofOfLifeDays) {
                    viewModel.updateProofOfLife()
                }
            }
        }
    }
}

@Composable
fun HomeHeader(name: String, date: String, onProfileClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Bonjour, $name",
                style = MaterialTheme.typography.displayMedium,
                color = TextPrimary
            )
            Text(
                text = date,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Surface(
            modifier = Modifier.size(52.dp).phoenXMatiere(),
            shape = CircleShape,
            color = SurfaceCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, TextTertiary.copy(alpha = 0.3f)),
            onClick = onProfileClick
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = AccentPrimary,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
fun ImpulseSection(onNavigate: (String) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = PhoenXAnimations.pressScale(isPressed)

    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .scale(scale)
                .clip(MaterialTheme.shapes.large)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(AccentPrimary, Color(0xFF8B4A1A))
                    )
                )
                .phoenXMatiere() // Application de la texture cuir/matière
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { onNavigate("TEXT") }
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Déposer un souvenir",
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 18.sp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            QuickActionIcon(Icons.Default.Edit, "Texte") { onNavigate("TEXT") }
            QuickActionIcon(Icons.Default.Mic, "Voix") { onNavigate("AUDIO") }
            QuickActionIcon(Icons.Default.CameraAlt, "Photo") { onNavigate("PHOTO") }
            QuickActionIcon(Icons.Default.NightsStay, "3h du matin") { onNavigate("NIGHT") }
        }
    }
}

@Composable
fun QuickActionIcon(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier
                .size(56.dp)
                .clickable(onClick = onClick),
            shape = CircleShape,
            color = SurfaceCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, TextTertiary.copy(alpha = 0.2f))
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = AccentPrimary, modifier = Modifier.padding(16.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
    }
}

@Composable
fun TimelinePreviewCard(count: Int, minAge: Int, maxAge: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clickable(onClick = onClick)
            .phoenXMatiere(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard.copy(alpha = 0.6f)),
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, TextTertiary.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text("TON FIL DE PENSÉE", style = MaterialTheme.typography.labelSmall, color = AccentPrimary, letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$count moments capturés", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                }
                
                // Le Sceau de l'Âge (Signature 4.1)
                Surface(
                    color = AccentPrimary,
                    shape = CircleShape,
                    modifier = Modifier.size(60.dp).border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$maxAge\nans",
                            style = MaterialTheme.typography.labelSmall.copy(lineHeight = 12.sp, fontWeight = FontWeight.Bold),
                            color = BackgroundPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth().height(50.dp), verticalAlignment = Alignment.Bottom) {
                repeat(24) { i ->
                    val h = (0.2f + (Math.sin(i.toDouble() / 3.0).toFloat() + 1f) * 0.4f)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 1.5.dp)
                            .fillMaxHeight(fraction = h)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = if (i > 20) listOf(AccentPrimary, AccentSecondary) else listOf(TextTertiary, TextTertiary.copy(alpha = 0.5f))
                                ),
                                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun YoungSelfLetterCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clickable(onClick = onClick)
            .phoenXMatiere(isPaper = true),
        colors = CardDefaults.cardColors(containerColor = MateriauPapier.copy(alpha = 0.1f)),
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(2.dp).height(40.dp).background(AccentPrimary))
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("LETTRE À MON JEUNE MOI", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Text("Écris à celui que tu étais à 20 ans", style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.Normal)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = AccentPrimary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun BiographerQuestionSection(question: String) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text("LA QUESTION DU BIOGRAPHE", style = MaterialTheme.typography.labelSmall, color = AccentPrimary, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "\"$question\"",
            style = MaterialTheme.typography.displaySmall.copy(lineHeight = 34.sp),
            color = TextPrimary,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { /* TODO */ },
            colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.height(48.dp).phoenXMatiere()
        ) {
            Text("Y répondre maintenant", color = TextPrimary)
        }
    }
}

@Composable
fun LatestMemoriesSection() {
    Column {
        Text(
            "Derniers souvenirs", 
            style = MaterialTheme.typography.labelLarge, 
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(5) { i ->
                MemoryCard()
            }
        }
    }
}

@Composable
fun MemoryCard() {
    Surface(
        modifier = Modifier.size(160.dp, 200.dp).phoenXMatiere(),
        color = SurfaceCard.copy(alpha = 0.4f),
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, TextTertiary.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Icon(Icons.Default.AutoStories, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("À 43 ans", style = MaterialTheme.typography.labelSmall, color = AccentPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Le silence n'est pas un oubli, c'est une attente...",
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = TextSecondary,
                maxLines = 4
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.Lock, contentDescription = null, tint = TextTertiary.copy(alpha = 0.5f), modifier = Modifier.size(14.dp).align(Alignment.End))
        }
    }
}

@Composable
fun ProofOfLifeBadge(days: Int, onClick: () -> Unit) {
    val color = if (days < 5) Success else if (days < 10) Warning else Error
    Surface(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = color.copy(alpha = 0.05f),
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
            Spacer(modifier = Modifier.width(12.dp))
            Text("Ma présence • confirmée il y a $days jours", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
        }
    }
}

@Composable
fun PhoenXBottomBar() {
    NavigationBar(
        containerColor = BackgroundPrimary.copy(alpha = 0.95f),
        tonalElevation = 0.dp,
        modifier = Modifier.border(1.dp, TextTertiary.copy(alpha = 0.1f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
    ) {
        NavigationBarItem(
            selected = true,
            onClick = { },
            icon = { Icon(Icons.Default.Home, null) },
            label = { Text("Accueil", style = MaterialTheme.typography.labelSmall) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = AccentPrimary,
                selectedTextColor = AccentPrimary,
                indicatorColor = Color.Transparent,
                unselectedIconColor = TextTertiary,
                unselectedTextColor = TextTertiary
            )
        )
        NavigationBarItem(
            selected = false,
            onClick = { },
            icon = { Icon(Icons.Default.AddCircleOutline, null) },
            label = { Text("Capturer", style = MaterialTheme.typography.labelSmall) }
        )
        NavigationBarItem(
            selected = false,
            onClick = { },
            icon = { Icon(Icons.Default.Timeline, null) },
            label = { Text("Mon Fil", style = MaterialTheme.typography.labelSmall) }
        )
        NavigationBarItem(
            selected = false,
            onClick = { },
            icon = { Icon(Icons.Default.AutoAwesome, null) },
            label = { Text("L'IA", style = MaterialTheme.typography.labelSmall) }
        )
    }
}

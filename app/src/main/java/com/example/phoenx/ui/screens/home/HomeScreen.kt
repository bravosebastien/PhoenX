package com.example.phoenx.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.ui.theme.*

@Composable
fun HomeScreen(
    onNavigateToCapture: (String) -> Unit,
    onNavigateToFil: () -> Unit,
    onNavigateToLetters: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = BackgroundPrimary,
        bottomBar = { PhoenXBottomBar() }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // Section 1: En-tête
            HomeHeader(uiState.userName, uiState.currentDate)

            // Section 2: Bouton d'Impulsion
            ImpulseSection(onNavigateToCapture)

            // Section 3: Aperçu Fil de Pensée
            TimelinePreviewCard(uiState.entryCount, uiState.minAge, uiState.currentAge, onNavigateToFil)

            // Section 4: Lettre à Mon Jeune Moi
            YoungSelfLetterCard(onNavigateToLetters)

            // Section 5: Question du Biographe
            BiographerQuestionSection(uiState.biographerQuestion)

            // Section 6: Derniers souvenirs
            LatestMemoriesSection()

            // Section 7: Preuve de Vie
            ProofOfLifeBadge(uiState.lastProofOfLifeDays)
        }
    }
}

@Composable
fun HomeHeader(name: String, date: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
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
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = SurfaceCard
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}

@Composable
fun ImpulseSection(onNavigate: (String) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(Brush.horizontalGradient(listOf(AccentPrimary, Color(0xFF8B4A1A))))
                .clickable { onNavigate("TEXT") },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+ Déposer un souvenir",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            QuickActionIcon(Icons.Default.Edit, "Texte") { onNavigate("TEXT") }
            QuickActionIcon(Icons.Default.Mic, "Voix") { onNavigate("AUDIO") }
            QuickActionIcon(Icons.Default.CameraAlt, "Photo") { onNavigate("PHOTO") }
            QuickActionIcon(Icons.Default.NightsStay, "Nuit") { onNavigate("NIGHT") }
        }
    }
}

@Composable
fun QuickActionIcon(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier
                .size(48.dp)
                .clickable(onClick = onClick),
            shape = CircleShape,
            color = SurfaceCard
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = AccentPrimary, modifier = Modifier.padding(12.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
    }
}

@Composable
fun TimelinePreviewCard(count: Int, minAge: Int, maxAge: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("TON FIL DE PENSÉE", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Text("$count pensées • de $minAge à $maxAge ans", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.Bottom) {
                repeat(12) { i ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp)
                            .fillMaxHeight(fraction = (0.3f + (i % 3) * 0.2f))
                            .background(if (i == 11) AccentPrimary else TextTertiary, CircleShape)
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
            .padding(horizontal = 24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = MaterialTheme.shapes.large
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(3.dp).height(40.dp).background(AccentPrimary))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("LETTRE À MON JEUNE MOI", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Text("Que dirais-tu à toi-même à 20 ans ?", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            }
            IconButton(onClick = onClick) {
                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = AccentPrimary)
            }
        }
    }
}

@Composable
fun BiographerQuestionSection(question: String) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text("QUESTION DU JOUR", style = MaterialTheme.typography.labelSmall, color = AccentPrimary)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = question,
            style = MaterialTheme.typography.displaySmall,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { /* TODO */ },
            colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Répondre", color = TextPrimary)
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
        modifier = Modifier.size(140.dp, 180.dp),
        color = SurfaceCard,
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(Icons.Default.Description, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("À 43 ans", style = MaterialTheme.typography.labelSmall, color = AccentPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "L'importance de transmettre ce qui ne peut s'écrire...",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 4
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.Lock, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(14.dp).align(Alignment.End))
        }
    }
}

@Composable
fun ProofOfLifeBadge(days: Int) {
    val color = if (days < 5) Success else if (days < 10) Warning else Error
    Surface(
        modifier = Modifier
            .padding(24.dp)
            .fillMaxWidth()
            .clickable { /* logic update */ },
        color = color.copy(alpha = 0.1f),
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
            Spacer(modifier = Modifier.width(12.dp))
            Text("Preuve de vie • il y a $days jours", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
        }
    }
}

@Composable
fun PhoenXBottomBar() {
    NavigationBar(
        containerColor = BackgroundPrimary,
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            selected = true,
            onClick = { },
            icon = { Icon(Icons.Default.Home, null) },
            label = { Text("Accueil") },
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
            icon = { Icon(Icons.Default.Add, null) },
            label = { Text("Capturer") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { },
            icon = { Icon(Icons.Default.Timeline, null) },
            label = { Text("Mon Fil") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { },
            icon = { Icon(Icons.Default.Send, null) },
            label = { Text("Transmettre") }
        )
    }
}

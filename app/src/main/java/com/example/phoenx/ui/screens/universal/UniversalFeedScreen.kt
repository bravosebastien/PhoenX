package com.example.phoenx.ui.screens.universal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.phoenx.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

data class UniversalMessage(
    val id: String = "",
    val creatorFirstName: String = "Un proche",
    val creatorAge: Int = 0,
    val creatorCountry: String = "",
    val creatorProfession: String? = null,
    val creatorCity: String? = null,
    val messageText: String = "",
    val photoUrls: List<String> = emptyList(),
    val category: String = "Sagesse",
    val publishedAt: com.google.firebase.Timestamp? = null,
    val bioLine: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalFeedScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    var messages by remember { mutableStateOf<List<UniversalMessage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf("Toutes") }

    val categories = listOf("Toutes", "Amour", "Espoir", "Sagesse", "Regret", "Transmission", "Foi", "Réconciliation", "Humanité", "Gratitude")

    LaunchedEffect(selectedCategory) {
        isLoading = true
        try {
            var query = db.collection("universalMessages")
                .whereEqualTo("isPublished", true)
                .whereEqualTo("isModerated", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)

            if (selectedCategory != "Toutes") {
                query = query.whereEqualTo("category", selectedCategory)
            }

            val snapshot = query.get().await()
            messages = snapshot.documents.mapNotNull { doc ->
                doc.toObject(UniversalMessage::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            android.util.Log.e("UniversalFeed", "Error: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            Column(modifier = Modifier.background(BackgroundPrimary)) {
                TopAppBar(
                    title = {
                        Column {
                            Text("Lettres à l'Humanité", style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif))
                            Text("Des mots laissés pour toi par ceux qui sont partis.", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AccentPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPrimary)
                )
                
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentPrimary,
                                selectedLabelColor = BackgroundPrimary,
                                containerColor = SurfaceCard,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentPrimary)
            }
        } else if (messages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aucune lettre pour le moment.", color = TextTertiary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(messages) { message ->
                    UniversalMessageCard(message)
                }
            }
        }
    }
}

@Composable
fun UniversalMessageCard(message: UniversalMessage) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = AccentPrimary,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = message.category.uppercase(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = BackgroundPrimary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "${message.creatorFirstName}, ${message.creatorAge} ans • ${message.creatorCountry}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            }

            if (!message.bioLine.isNullOrEmpty()) {
                Text(
                    text = message.bioLine,
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Message
            Text(
                text = message.messageText,
                style = TextStyle(fontFamily = FontFamily.Serif, fontSize = 17.sp, color = TextPrimary, lineHeight = 26.sp),
                maxLines = 5 // "Lire la suite" à implémenter si besoin
            )

            if (message.photoUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(message.photoUrls) { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier.size(120.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
        }
    }
}

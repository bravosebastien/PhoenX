package com.example.phoenx.ui.screens.auth

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.ui.theme.*
import kotlin.random.Random

@Composable
fun RecoveryPhraseScreen(
    phrase: List<String>,
    onConfirmed: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    val context = LocalContext.current

    // Sélection aléatoire de 3 indices pour la vérification
    val verificationIndices = remember {
        val indices = mutableListOf<Int>()
        while (indices.size < 3) {
            val r = Random.nextInt(12)
            if (r !in indices) indices.add(r)
        }
        indices.sorted()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPrimary)
            .padding(24.dp)
    ) {
        when (step) {
            1 -> WarningStep { step = 2 }
            2 -> DisplayStep(phrase) { step = 3 }
            3 -> VerificationStep(
                phrase = phrase,
                indices = verificationIndices,
                onSuccess = {
                    Toast.makeText(context, "Parfait. Ta phrase est en sécurité.", Toast.LENGTH_SHORT).show()
                    onConfirmed()
                }
            )
        }
    }
}

@Composable
fun WarningStep(onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = AccentPrimary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Avant de continuer",
            style = MaterialTheme.typography.displaySmall.copy(fontFamily = FontFamily.Serif),
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "PHOEN-X va générer ta phrase de récupération. Ce sont 12 mots qui protègent l'ensemble de tes souvenirs.\n\n⚠️ Si tu perds ces 12 mots ET ton téléphone, tes données seront perdues pour toujours. Même nous ne pourrons pas les récupérer. Personne. Jamais.\n\nSi tu changes de téléphone un jour, ces 12 mots seront le seul moyen de retrouver tes souvenirs.\n\nPrends un stylo et du papier maintenant.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            lineHeight = 26.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
        ) {
            Text("J'ai un stylo et du papier, je suis prêt(e)", color = BackgroundPrimary)
        }
    }
}

@Composable
fun DisplayStep(phrase: List<String>, onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = "Ta phrase de récupération",
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif),
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Note ces 12 mots dans l'ordre exact, sur papier. Pas de capture d'écran.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(phrase) { index, word ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2E2E35)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = (index + 1).toString(),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = Color(0xFF5C5855)
                        )
                        Text(
                            text = word,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFFF2EDE8)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
        ) {
            Text("J'ai noté tous les mots", color = BackgroundPrimary)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun VerificationStep(
    phrase: List<String>,
    indices: List<Int>,
    onSuccess: () -> Unit
) {
    var inputs by remember { mutableStateOf(List(3) { "" }) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = "Vérifions ensemble",
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif),
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Pour confirmer que tu as bien noté ta phrase, entre les mots numéros ${indices[0] + 1}, ${indices[1] + 1} et ${indices[2] + 1}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(40.dp))

        indices.forEachIndexed { i, index ->
            OutlinedTextField(
                value = inputs[i],
                onValueChange = { newValue ->
                    inputs = inputs.toMutableList().apply { this[i] = newValue }
                    errorMessage = null
                },
                label = { Text("Mot n°${index + 1}") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPrimary,
                    unfocusedBorderColor = Color(0xFF2E2E35)
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (errorMessage != null) {
            Text(errorMessage!!, color = Error, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                val correct = indices.indices.all { i ->
                    inputs[i].trim().lowercase() == phrase[indices[i]].trim().lowercase()
                }
                if (correct) {
                    onSuccess()
                } else {
                    errorMessage = "Ce n'est pas tout à fait ça. Vérifie ce que tu as noté et réessaie."
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
        ) {
            Text("Vérifier et terminer", color = BackgroundPrimary)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

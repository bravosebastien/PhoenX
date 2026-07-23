package com.example.phoenx.ui.screens.auth

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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

    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
            .padding(24.dp)
    ) {
        when (step) {
            1 -> WarningStep(theme = theme, onNext = { step = 2 })
            2 -> DisplayStep(phrase = phrase, theme = theme, onNext = { step = 3 })
            3 -> VerificationStep(
                phrase = phrase,
                indices = verificationIndices,
                theme = theme,
                onSuccess = {
                    Toast.makeText(context, "Parfait. Ta phrase est en sécurité.", Toast.LENGTH_SHORT).show()
                    onConfirmed()
                }
            )
        }
    }
}

@Composable
fun WarningStep(theme: AppThemeState, onNext: () -> Unit) {
    val accent = theme.accentColor
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Avant de continuer",
            style = MaterialTheme.typography.displaySmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold),
            color = theme.contentColor,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "PHOEN-X va générer ta phrase de récupération. Ce sont 12 mots qui protègent l'ensemble de tes souvenirs.\n\n⚠️ Si tu perds ces 12 mots ET ton téléphone, tes données seront perdues pour toujours. Même nous ne pourrons pas les récupérer. Personne. Jamais.\n\nSi tu changes de téléphone un jour, ces 12 mots seront le seul moyen de retrouver tes souvenirs.\n\nPrends un stylo et du papier maintenant.",
            style = MaterialTheme.typography.bodyLarge,
            color = theme.contentColor.copy(alpha = 0.7f),
            lineHeight = 26.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
            colors = ButtonDefaults.buttonColors(containerColor = accent)
        ) {
            Text("J'ai un stylo et du papier, je suis prêt(e)", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DisplayStep(phrase: List<String>, theme: AppThemeState, onNext: () -> Unit) {
    val accent = theme.accentColor
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = "Ta phrase de récupération",
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold),
            color = theme.contentColor
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Note ces 12 mots dans l'ordre exact, sur papier. Pas de capture d'écran.",
            style = MaterialTheme.typography.bodySmall,
            color = theme.contentColor.copy(alpha = 0.6f),
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
                    colors = CardDefaults.cardColors(containerColor = theme.contentColor.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, theme.contentColor.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = (index + 1).toString(),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = theme.contentColor.copy(alpha = 0.3f)
                        )
                        Text(
                            text = word,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = theme.fontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = theme.contentColor
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
            colors = ButtonDefaults.buttonColors(containerColor = accent)
        ) {
            Text("J'ai noté tous les mots", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun VerificationStep(
    phrase: List<String>,
    indices: List<Int>,
    theme: AppThemeState,
    onSuccess: () -> Unit
) {
    val accent = theme.accentColor
    var inputs by remember { mutableStateOf(List(3) { "" }) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = "Vérifions ensemble",
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = theme.fontFamily, fontWeight = FontWeight.Bold),
            color = theme.contentColor
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Pour confirmer que tu as bien noté ta phrase, entre les mots numéros ${indices[0] + 1}, ${indices[1] + 1} et ${indices[2] + 1}",
            style = MaterialTheme.typography.bodyMedium,
            color = theme.contentColor.copy(alpha = 0.7f),
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
                    focusedBorderColor = accent,
                    unfocusedBorderColor = theme.contentColor.copy(alpha = 0.1f),
                    focusedTextColor = theme.contentColor,
                    unfocusedTextColor = theme.contentColor
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
            colors = ButtonDefaults.buttonColors(containerColor = accent)
        ) {
            Text("Vérifier et terminer", color = theme.backgroundColor, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

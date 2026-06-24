package com.example.phoenx.ui.screens.youngselfletters

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YoungSelfLetterScreen(
    onNavigateBack: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    var targetAge by remember { mutableFloatStateOf(20f) }

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = { Text("Lettre à mon jeune moi", style = MaterialTheme.typography.labelLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPrimary, titleContentColor = TextPrimary)
            )
        },
        bottomBar = {
            BottomAppBar(containerColor = BackgroundPrimary) {
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { /* TODO: Envoyer */ },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Déposer", color = BackgroundPrimary)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(
                "À quel âge veux-tu t'adresser ?",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Slider(
                value = targetAge,
                onValueChange = { targetAge = it },
                valueRange = 10f..40f,
                steps = 30,
                colors = SliderDefaults.colors(thumbColor = AccentPrimary, activeTrackColor = AccentPrimary)
            )
            Text(
                "${targetAge.toInt()} ans",
                style = MaterialTheme.typography.displaySmall,
                color = AccentPrimary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = BackgroundSecondary,
                shape = MaterialTheme.shapes.medium
            ) {
                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    Box(modifier = Modifier.width(3.dp).fillMaxHeight().background(AccentPrimary))
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "À toi, à ${targetAge.toInt()} ans —",
                            style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                            color = AccentPrimary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TextField(
                            value = text,
                            onValueChange = { text = it },
                            placeholder = { Text("Écris ce que tu aurais voulu entendre...", color = TextTertiary) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic)
                        )
                    }
                }
            }
        }
    }
}

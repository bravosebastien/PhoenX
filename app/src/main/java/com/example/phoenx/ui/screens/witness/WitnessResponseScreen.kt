package com.example.phoenx.ui.screens.witness

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.phoenx.R
import com.example.phoenx.ui.theme.*

@Composable
fun WitnessResponseScreen(
    creatorId: String,
    witnessId: String,
    token: String,
    navController: NavController,
    viewModel: WitnessViewModel = hiltViewModel()
) {
    var step by remember { mutableIntStateOf(1) } // 1: form, 2: success
    val isLoading by viewModel.isLoading.collectAsState()

    val answers = remember { mutableStateMapOf<String, String>() }
    val questions = listOf(
        "Raconte un souvenir avec ton proche — dans ta version, pas la sienne.",
        "Quelle qualité unique possède-t-il/elle ?",
        "Qu'est-ce qu'il/elle t'a appris ?",
        "Y a-t-il quelque chose que tu voudrais lui dire ?"
    )

    val canSubmit = answers.values.any { it.isNotBlank() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPrimary)
    ) {
        if (step == 1) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Ton témoignage",
                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif),
                    color = TextPrimary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Ton proche ne verra jamais ce que tu écris ici. Tes mots seront découverts par ses héritiers après son départ. Réponds à ce qui te touche. Ignore le reste.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                questions.forEach { question ->
                    TestimonyQuestionItem(
                        question = question,
                        value = answers[question] ?: "",
                        onValueChange = { answers[question] = it }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        viewModel.submitTestimony(creatorId, witnessId, token, answers) {
                            step = 2
                        }
                    },
                    enabled = canSubmit && !isLoading,
                    modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = BackgroundPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Déposer mon témoignage", color = BackgroundPrimary, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Success,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Témoignage déposé.",
                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Merci. Ton proche ne le verra pas. Ses héritiers le découvriront en temps voulu.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun TestimonyQuestionItem(
    question: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column {
        Text(
            text = question,
            style = TextStyle(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontSize = 15.sp),
            color = AccentPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceCard)
                .padding(16.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

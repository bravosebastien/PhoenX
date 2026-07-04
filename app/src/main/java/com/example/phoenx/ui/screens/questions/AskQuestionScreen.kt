package com.example.phoenx.ui.screens.questions

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskQuestionScreen(
    creatorId: String,
    recipientId: String,
    onNavigateBack: () -> Unit,
    viewModel: AskQuestionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    var questionText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Amour & Couple") }

    LaunchedEffect(creatorId, recipientId) {
        viewModel.loadData(creatorId, recipientId)
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            Toast.makeText(context, "Ta question a été scellée.", Toast.LENGTH_SHORT).show()
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, "Erreur : $it", Toast.LENGTH_SHORT).show()
        }
    }
    
    val categories = listOf(
        "Amour & Couple", "Fierté & Jugement", "Secrets de famille", 
        "Regrets & Non-dits", "Identité & Passé", "Sens & Transmission"
    )

    val suggestions = mapOf(
        "Amour & Couple" to listOf(
            "Est-ce que tu as été heureux avec [conjoint] ?",
            "Qui a été le grand amour de ta vie, avant nous ?",
            "Est-ce que tu as eu des regrets amoureux que tu n'as jamais partagés ?",
            "Qu'est-ce que tu pensais vraiment de [prénom] ?",
            "As-tu un jour douté de notre couple/famille ?"
        ),
        "Fierté & Jugement" to listOf(
            "Est-ce que tu étais fier de moi ?",
            "Qu'est-ce que tu aurais voulu que je fasse différemment ?",
            "Qu'as-tu pensé le jour où je t'ai annoncé [un choix] ?",
            "As-tu eu peur pour moi à un moment de ma vie ? Quand ?",
            "Qu'est-ce que tu admirais chez moi sans jamais le dire ?"
        ),
        "Secrets de famille" to listOf(
            "Pourquoi tu ne m'as jamais parlé de [personne] ?",
            "Qu'est-ce qui s'est vraiment passé entre toi et [personne] ?",
            "Y a-t-il quelque chose sur notre famille que tu ne m'as jamais dit ?",
            "Pourquoi on n'a jamais reparlé de [événement] ?",
            "As-tu un secret que tu n'as jamais partagé avec personne ?"
        ),
        "Regrets & Non-dits" to listOf(
            "Qu'est-ce que tu aurais voulu me dire et que tu n'as jamais trouvé le moment de dire ?",
            "Est-ce qu'il y a quelque chose que tu me reproches encore ?",
            "Si tu pouvais recommencer une chose avec moi, ce serait quoi ?",
            "Y a-t-il une dispute entre nous que tu regrettes ?",
            "Qu'est-ce que tu n'as jamais pu me pardonner, ou te pardonner à toi-même ?"
        ),
        "Identité & Passé" to listOf(
            "Comment tu étais avant de devenir mon père/ma mère ?",
            "Qu'est-ce que tu voulais faire de ta vie, avant que tout change ?",
            "Qu'est-ce que tu as sacrifié pour nous, sans jamais nous le dire ?",
            "Qui étais-tu vraiment, au-delà de ton rôle dans la famille ?",
            "Quelle partie de toi je n'ai jamais connue ?"
        ),
        "Sens & Transmission" to listOf(
            "Tu avais peur de mourir ?",
            "Qu'est-ce que tu espérais qu'on retienne de toi ?",
            "Qu'est-ce que tu voudrais qu'on se dise, là, maintenant, si tu pouvais m'entendre ?",
            "Qu'est-ce que tu considères comme ta plus grande réussite, celle que personne ne voit ?",
            "De quoi es-tu le plus fier dans la façon dont tu nous as élevés/aimés ?"
        )
    )

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = { Text("Une question pour ${uiState.creatorName}", style = MaterialTheme.typography.labelLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPrimary)
            )
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
                text = "Certaines personnes trouvent les mots facilement. D'autres non — par pudeur, par habitude du silence, ou simplement parce que certains sujets n'ont jamais trouvé leur moment. Si tu as une question que tu n'as jamais réussi à poser à ${uiState.creatorName} de son vivant, tu peux la déposer ici. Ceci ne remplace jamais une conversation possible aujourd'hui. Si tu peux encore poser cette question à voix haute, fais-le — c'est toujours mieux.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (uiState.questionsRemaining >= 0) {
                Text(
                    text = "${uiState.questionsRemaining} questions restantes sur ${uiState.maxQuestions} autorisées",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (uiState.questionsRemaining > 0) AccentPrimary else Error,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            TextField(
                value = questionText,
                onValueChange = { questionText = it },
                placeholder = { Text("Écris ta question ici...", style = TextStyle(fontFamily = FontFamily.Serif, fontSize = 18.sp, color = TextTertiary)) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                textStyle = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif, color = TextPrimary),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SurfaceCard.copy(alpha = 0.3f),
                    unfocusedContainerColor = SurfaceCard.copy(alpha = 0.3f),
                    focusedIndicatorColor = AccentPrimary,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(40.dp))

            Text("SUGGESTIONS", style = MaterialTheme.typography.labelSmall, color = AccentPrimary, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(16.dp))

            ScrollableTabRow(
                selectedTabIndex = categories.indexOf(selectedCategory),
                containerColor = Color.Transparent,
                contentColor = AccentPrimary,
                edgePadding = 0.dp,
                divider = {},
                indicator = {}
            ) {
                categories.forEach { cat ->
                    Tab(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        text = { Text(cat, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            suggestions[selectedCategory]?.forEach { suggestion ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { questionText = suggestion },
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = suggestion,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        fontStyle = FontStyle.Italic
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    viewModel.sealQuestion(creatorId, recipientId, questionText)
                },
                enabled = questionText.isNotBlank() && !uiState.isSaving && (uiState.questionsRemaining != 0),
                modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(color = BackgroundPrimary, modifier = Modifier.size(24.dp))
                } else if (uiState.questionsRemaining == 0) {
                    Text("Limite atteinte", color = BackgroundPrimary)
                } else {
                    Text("Sceller cette question", color = BackgroundPrimary, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                "Ta question sera scellée. Tu auras la réponse quand son héritage te sera transmis.",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

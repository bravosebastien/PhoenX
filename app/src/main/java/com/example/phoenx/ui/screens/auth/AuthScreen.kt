package com.example.phoenx.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun AuthScreen(
    isSignup: Boolean,
    onAuthSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var currentStep by remember { mutableStateOf(if (isSignup) SignupStep.StepA else SignupStep.Login) }
    
    // États des champs
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf(LocalDate.now().minusYears(25)) }
    var depositaryName by remember { mutableStateOf("") }
    var recoveryConfirmed by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val recoveryPhrase by viewModel.recoveryPhrase.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is AuthState.Success) {
            onAuthSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPrimary)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (currentStep) {
                SignupStep.Login -> LoginContent(
                    email = email,
                    onEmailChange = { email = it },
                    password = password,
                    onPasswordChange = { password = it },
                    onLoginClick = { /* logic login */ },
                    onNavigateToSignup = { currentStep = SignupStep.StepA }
                )
                SignupStep.StepA -> SignupStepA(
                    email = email,
                    onEmailChange = { email = it },
                    password = password,
                    onPasswordChange = { password = it },
                    birthDate = birthDate,
                    onBirthDateChange = { birthDate = it },
                    onNext = { 
                        viewModel.generateRecoveryPhrase()
                        currentStep = SignupStep.StepB 
                    }
                )
                SignupStep.StepB -> SignupStepB(
                    phrase = recoveryPhrase,
                    confirmed = recoveryConfirmed,
                    onConfirmedChange = { recoveryConfirmed = it },
                    onNext = { currentStep = SignupStep.StepC }
                )
                SignupStep.StepC -> SignupStepC(
                    depositaryName = depositaryName,
                    onDepositaryNameChange = { depositaryName = it },
                    onFinish = { viewModel.signUp(email, password, birthDate, depositaryName) },
                    isLoading = uiState is AuthState.Loading
                )
            }

            if (uiState is AuthState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = (uiState as AuthState.Error).message,
                    color = Error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun LoginContent(
    email: String, onEmailChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onNavigateToSignup: () -> Unit
) {
    Text("Content de te revoir", style = MaterialTheme.typography.displayMedium, color = TextPrimary)
    Spacer(modifier = Modifier.height(32.dp))
    
    OutlinedTextField(
        value = email, onValueChange = onEmailChange,
        label = { Text("Email") },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentPrimary)
    )
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(
        value = password, onValueChange = onPasswordChange,
        label = { Text("Mot de passe") },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentPrimary)
    )
    Spacer(modifier = Modifier.height(32.dp))
    Button(
        onClick = onLoginClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
    ) {
        Text("Se connecter", color = BackgroundPrimary)
    }
    TextButton(onClick = onNavigateToSignup) {
        Text("Créer un compte", color = AccentPrimary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupStepA(
    email: String, onEmailChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    birthDate: LocalDate, onBirthDateChange: (LocalDate) -> Unit,
    onNext: () -> Unit
) {
    Text("Ton espace commence ici", style = MaterialTheme.typography.displayMedium, color = TextPrimary)
    Spacer(modifier = Modifier.height(32.dp))

    OutlinedTextField(
        value = email, onValueChange = onEmailChange,
        label = { Text("Email") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(16.dp))

    // Simple date text for MVP
    Text("Date de naissance", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    Text(
        birthDate.format(DateTimeFormatter.ofPattern("dd / MM / yyyy")),
        style = MaterialTheme.typography.bodyLarge,
        color = TextPrimary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
    // TODO: Intégrer un vrai DatePicker
    
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(
        value = password, onValueChange = onPasswordChange,
        label = { Text("Mot de passe") },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        "Chiffrement E2EE : si vous perdez votre mot de passe et votre phrase, vos données seront irrécupérables.",
        style = MaterialTheme.typography.labelSmall,
        color = Warning,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(32.dp))
    Button(
        onClick = onNext,
        enabled = email.isNotEmpty() && password.length >= 12,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
    ) {
        Text("Continuer", color = BackgroundPrimary)
    }
}

@Composable
fun SignupStepB(
    phrase: List<String>,
    confirmed: Boolean,
    onConfirmedChange: (Boolean) -> Unit,
    onNext: () -> Unit
) {
    Text("Ta phrase de récupération", style = MaterialTheme.typography.displaySmall, color = TextPrimary)
    Spacer(modifier = Modifier.height(24.dp))
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceCard,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            phrase.chunked(3).forEach { rowWords ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    rowWords.forEach { word ->
                        Text(word, color = TextPrimary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
    
    Spacer(modifier = Modifier.height(24.dp))
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = confirmed, 
            onCheckedChange = onConfirmedChange,
            colors = CheckboxDefaults.colors(checkedColor = AccentPrimary)
        )
        Text("J'ai noté ma phrase en lieu sûr", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
    }

    Spacer(modifier = Modifier.height(32.dp))
    Button(
        onClick = onNext,
        enabled = confirmed,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
    ) {
        Text("Continuer", color = BackgroundPrimary)
    }
}

@Composable
fun SignupStepC(
    depositaryName: String,
    onDepositaryNameChange: (String) -> Unit,
    onFinish: () -> Unit,
    isLoading: Boolean
) {
    Text("Avant de commencer...", style = MaterialTheme.typography.displaySmall, color = TextPrimary)
    Spacer(modifier = Modifier.height(24.dp))
    
    Text(
        "Quel est le nom de quelqu'un à qui tu veux transmettre quelque chose ?",
        style = MaterialTheme.typography.bodyLarge,
        color = TextSecondary,
        textAlign = TextAlign.Center
    )
    
    Spacer(modifier = Modifier.height(24.dp))
    OutlinedTextField(
        value = depositaryName, onValueChange = onDepositaryNameChange,
        label = { Text("Prénom ou Surnom") },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(32.dp))
    
    if (isLoading) {
        CircularProgressIndicator(color = AccentPrimary)
    } else {
        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
        ) {
            Text("Créer mon espace", color = BackgroundPrimary)
        }
        
        TextButton(onClick = onFinish) {
            Text("Je préfère découvrir d'abord", color = TextTertiary)
        }
    }
}

enum class SignupStep {
    Login, StepA, StepB, StepC
}

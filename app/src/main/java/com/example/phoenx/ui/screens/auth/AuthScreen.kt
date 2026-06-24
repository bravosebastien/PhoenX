package com.example.phoenx.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
    
    // États partagés
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
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    fadeIn(tween(500)) togetherWith fadeOut(tween(500))
                },
                label = "auth_step"
            ) { step ->
                when (step) {
                    SignupStep.Login -> LoginContent(
                        email = email,
                        onEmailChange = { email = it },
                        password = password,
                        onPasswordChange = { password = it },
                        onLoginClick = { viewModel.login(email, password) },
                        onNavigateToSignup = { currentStep = SignupStep.StepA },
                        isLoading = uiState is AuthState.Loading
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
            }

            if (uiState is AuthState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = (uiState as AuthState.Error).message,
                    color = Error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
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
    onNavigateToSignup: () -> Unit,
    isLoading: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Content de te revoir", style = MaterialTheme.typography.displayMedium, color = TextPrimary)
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = email, onValueChange = onEmailChange,
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentPrimary)
        )
        Spacer(modifier = Modifier.height(16.dp))
        var passwordVisible by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = password, onValueChange = onPasswordChange,
            label = { Text("Mot de passe") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = icon, contentDescription = null, tint = TextSecondary)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentPrimary)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onLoginClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
            enabled = !isLoading && email.isNotEmpty() && password.isNotEmpty()
        ) {
            if (isLoading) CircularProgressIndicator(color = BackgroundPrimary, modifier = Modifier.size(24.dp))
            else Text("Se connecter", color = BackgroundPrimary)
        }
        TextButton(onClick = onNavigateToSignup) {
            Text("Créer un compte", color = AccentPrimary)
        }
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
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Ton espace commence ici", style = MaterialTheme.typography.displayMedium, color = TextPrimary, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email, onValueChange = onEmailChange,
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Date de naissance
        Text(
            "POUR TON FIL DE PENSÉE", 
            style = MaterialTheme.typography.labelSmall, 
            color = AccentPrimary,
            modifier = Modifier.align(Alignment.Start)
        )
        var showDatePicker by remember { mutableStateOf(false) }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = birthDate.atStartOfDay(java.time.ZoneId.of("UTC")).toInstant().toEpochMilli()
        )

        OutlinedCard(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = SurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, TextTertiary)
        ) {
            Text(
                birthDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale.FRENCH)),
                modifier = Modifier.padding(16.dp),
                color = TextPrimary
            )
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let {
                            onBirthDateChange(
                                java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.of("UTC")).toLocalDate()
                            )
                        }
                        showDatePicker = false
                    }) { Text("OK", color = AccentPrimary) }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        // Password strength simulation
        val strength = if (password.length < 8) 0.3f else if (password.length < 12) 0.6f else 1f
        val strengthColor = if (strength < 0.4f) Error else if (strength < 0.8f) Warning else Success
        
        OutlinedTextField(
            value = password, onValueChange = onPasswordChange,
            label = { Text("Mot de passe (12+ caractères)") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        LinearProgressIndicator(
            progress = { strength },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            color = strengthColor,
            trackColor = SurfaceCard
        )
        
        Text(
            "Sécurité absolue : perdu = irrécupérable.",
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
}

@Composable
fun SignupStepB(
    phrase: List<String>,
    confirmed: Boolean,
    onConfirmedChange: (Boolean) -> Unit,
    onNext: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Ta phrase de récupération", style = MaterialTheme.typography.displaySmall, color = TextPrimary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Ces 12 mots sont la SEULE clé de ton héritage si tu perds ton téléphone.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SurfaceCard,
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                phrase.chunked(3).forEach { rowWords ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        rowWords.forEach { word ->
                            Text(
                                text = word, 
                                color = TextPrimary, 
                                modifier = Modifier.weight(1f).padding(vertical = 4.dp), 
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Checkbox(
                checked = confirmed, 
                onCheckedChange = onConfirmedChange,
                colors = CheckboxDefaults.colors(checkedColor = AccentPrimary)
            )
            Text(
                "J'ai noté ma phrase sur un support physique.", 
                color = TextPrimary, 
                style = MaterialTheme.typography.bodySmall
            )
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
}

@Composable
fun SignupStepC(
    depositaryName: String,
    onDepositaryNameChange: (String) -> Unit,
    onFinish: () -> Unit,
    isLoading: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Une dernière chose...", style = MaterialTheme.typography.displaySmall, color = TextPrimary)
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "Qui est la personne à qui tu souhaites un jour transmettre ton héritage ?",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = depositaryName, onValueChange = onDepositaryNameChange,
            label = { Text("Prénom ou Surnom") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentPrimary)
        )

        Spacer(modifier = Modifier.height(48.dp))
        
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
            
            TextButton(onClick = onFinish, modifier = Modifier.padding(top = 16.dp)) {
                Text("Je préfère découvrir d'abord", color = TextTertiary)
            }
        }
    }
}

enum class SignupStep {
    Login, StepA, StepB, StepC
}

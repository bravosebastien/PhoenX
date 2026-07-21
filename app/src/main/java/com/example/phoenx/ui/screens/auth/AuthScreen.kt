package com.example.phoenx.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.ui.theme.*
import androidx.compose.material.icons.filled.Email
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun AuthScreen(
    isSignup: Boolean,
    onAuthSuccess: () -> Unit,
    onNavigateToRecovery: () -> Unit,
    isGuestFlow: Boolean = false,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    var currentStep by remember { mutableStateOf(if (isSignup) SignupStep.StepA else SignupStep.Login) }
    
    // États partagés
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var birthDate by remember { 
        mutableStateOf(
            try {
                LocalDate.now().minusYears(25)
            } catch (e: Throwable) {
                // Fallback si LocalDate.now() échoue (très rare)
                LocalDate.of(1995, 1, 1)
            }
        ) 
    }
    var depositaryName by remember { mutableStateOf("") }
    var recoveryConfirmed by remember { mutableStateOf(value = false) }

    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var isVerifying by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is AuthState.Success) {
            onAuthSuccess()
        } else if (uiState is AuthState.PasswordResetSent) {
            android.widget.Toast.makeText(context, "Un email de réinitialisation a été envoyé.", android.widget.Toast.LENGTH_SHORT).show()
        } else if (uiState is AuthState.EmailVerificationSent) {
            isVerifying = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPrimary)
    ) {
        if (uiState is AuthState.EmailVerificationSent || uiState is AuthState.EmailNotVerified) {
            EmailVerificationContent(
                email = email,
                isNotVerifiedError = uiState is AuthState.EmailNotVerified,
                isLoading = isVerifying,
                onConfirmedClick = { 
                    isVerifying = true
                    com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.reload()?.addOnCompleteListener {
                        isVerifying = false
                        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.isEmailVerified == true) {
                            onAuthSuccess()
                        } else {
                            android.widget.Toast.makeText(context, "Email pas encore vérifié, vérifie ta boîte mail.", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onResendClick = { 
                    isVerifying = true
                    viewModel.resendVerificationEmail() 
                    android.widget.Toast.makeText(context, "Email renvoyé !", android.widget.Toast.LENGTH_SHORT).show()
                }
            )
        } else {
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
                            onResetPasswordClick = { viewModel.resetPassword(email) },
                            onNavigateToSignup = { currentStep = SignupStep.StepA },
                            onNavigateToRecovery = onNavigateToRecovery,
                            isLoading = uiState is AuthState.Loading
                        )
                        SignupStep.StepA -> SignupStepA(
                            email = email,
                            onEmailChange = { email = it },
                            password = password,
                            onPasswordChange = { password = it },
                            birthDate = birthDate,
                            onBirthDateChange = { birthDate = it },
                            isGuestFlow = isGuestFlow,
                            onNavigateToLogin = { currentStep = SignupStep.Login }
                        ) { 
                            if (isGuestFlow) {
                                viewModel.signUpGuest(email, password)
                            } else {
                                viewModel.signUp(email, password, birthDate)
                            }
                        }
                        SignupStep.StepB -> Text("Système avancé en veille")
                        SignupStep.StepC -> Text("Système avancé en veille")
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
}

@Composable
fun EmailVerificationContent(
    email: String,
    isNotVerifiedError: Boolean,
    isLoading: Boolean,
    onConfirmedClick: () -> Unit,
    onResendClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.Email,
            contentDescription = null,
            tint = AccentPrimary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Vérifie ta boîte mail",
            style = androidx.compose.ui.text.TextStyle(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                fontSize = 22.sp
            ),
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isNotVerifiedError) 
                "Tu dois d'abord confirmer ton adresse email avant de pouvoir te connecter."
            else 
                "Un email de confirmation a été envoyé à $email. Clique sur le lien pour activer ton compte.",
            style = androidx.compose.ui.text.TextStyle(fontSize = 15.sp),
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onConfirmedClick,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(56.dp).phoenXMatiere(),
            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = BackgroundPrimary, modifier = Modifier.size(24.dp))
            } else {
                Text("J'ai confirmé mon email", color = BackgroundPrimary)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(
            onClick = onResendClick,
            enabled = !isLoading
        ) {
            Text("Renvoyer l'email", color = TextSecondary)
        }
    }
}

@Composable
fun LoginContent(
    email: String, onEmailChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onResetPasswordClick: () -> Unit,
    onNavigateToSignup: () -> Unit,
    onNavigateToRecovery: () -> Unit,
    isLoading: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Content de te revoir", style = MaterialTheme.typography.displayMedium, color = TextPrimary)
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = email, onValueChange = onEmailChange,
            label = { Text("Email") },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentType = ContentType.EmailAddress },
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
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentType = ContentType.Password },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentPrimary)
        )
        
        TextButton(
            onClick = onResetPasswordClick,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Mot de passe oublié ?", color = TextTertiary, style = MaterialTheme.typography.labelSmall)
        }

        Spacer(modifier = Modifier.height(16.dp))
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
        
        // Système avancé en veille
        TextButton(onClick = onNavigateToRecovery) {
            Text("Restaurer via mes 12 mots (Legacy)", color = TextTertiary, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupStepA(
    email: String, onEmailChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    birthDate: LocalDate, onBirthDateChange: (LocalDate) -> Unit,
    isGuestFlow: Boolean = false,
    onNavigateToLogin: () -> Unit,
    onNext: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val title = if (isGuestFlow) "Votre espace commence ici" else "Ton espace commence ici"
        Text(title, style = MaterialTheme.typography.displayMedium, color = TextPrimary, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email, onValueChange = onEmailChange,
            label = { Text("Email") },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentType = ContentType.EmailAddress }
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (!isGuestFlow) {
            // Date de naissance (Masquée pour le flux Invité)
            Text(
                "POUR TON FIL DE PENSÉE", 
                style = MaterialTheme.typography.labelSmall, 
                color = AccentPrimary,
                modifier = Modifier.align(Alignment.Start)
            )
            var showDatePicker by remember { mutableStateOf(value = false) }
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
        }
        
        // Password strength simulation
        val strength = if (password.length < 8) 0.3f else if (password.length < 12) 0.6f else 1f
        val strengthColor = if (strength < 0.4f) Error else if (strength < 0.8f) Warning else Success
        
        OutlinedTextField(
            value = password, onValueChange = onPasswordChange,
            label = { Text("Mot de passe (12+ caractères)") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentType = ContentType.Password }
        )
        LinearProgressIndicator(
            progress = { strength },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            color = strengthColor,
            trackColor = SurfaceCard
        )
        
        Text(
            "Tes souvenirs sont chiffrés. En cas d'oubli, utilise la procédure de récupération par email.",
            style = MaterialTheme.typography.labelSmall,
            color = Warning,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        var termsAccepted by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = termsAccepted,
                onCheckedChange = { termsAccepted = it },
                colors = CheckboxDefaults.colors(checkedColor = AccentPrimary)
            )
            Column {
                Text(
                    text = "J'accepte les Conditions Générales d'Utilisation et la Politique de Confidentialité",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary
                )
                Row {
                    Text(
                        "Lire les CGU",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentPrimary,
                        modifier = Modifier.clickable { /* Navigation placeholder */ }
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Politique de Confidentialité",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentPrimary,
                        modifier = Modifier.clickable { /* Navigation placeholder */ }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onNext,
            enabled = (email.isNotEmpty() && password.length >= 12 && termsAccepted),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
        ) {
            Text("Continuer", color = BackgroundPrimary)
        }

        TextButton(onClick = onNavigateToLogin) {
            Text("J'ai déjà un compte ? Se connecter", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
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

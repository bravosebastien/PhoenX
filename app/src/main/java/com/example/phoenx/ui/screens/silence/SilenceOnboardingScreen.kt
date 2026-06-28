package com.example.phoenx.ui.screens.silence

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.ui.theme.*

@Composable
fun SilenceOnboardingScreen(
    onConfirmRythm: (Int) -> Unit
) {
    var selectedRythm by remember { mutableIntStateOf(30) }
    val rythms = listOf(
        RythmOption(14, "Toutes les 2 semaines", "Recommandé"),
        RythmOption(30, "Une fois par mois", null),
        RythmOption(60, "Tous les 2 mois", null)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPrimary)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Une présence, même dans le silence",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = FontFamily.Serif,
                color = TextPrimary
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "PHOEN-X ne peut transmettre ce que tu as construit que s'il sait que tu es là pour continuer à le construire. De temps en temps, l'app te donnera simplement signe de vie — un tap, et c'est tout.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Column(Modifier.selectableGroup()) {
            rythms.forEach { option ->
                RythmItem(
                    option = option,
                    selected = (selectedRythm == option.days),
                    onClick = { selectedRythm = option.days }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Si tu ne réponds pas après 3 rappels, ton Dépositaire recevra une notification discrète.",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = { onConfirmRythm(selectedRythm) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Je choisis ce rythme", color = BackgroundPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RythmItem(
    option: RythmOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            ),
        color = if (selected) SurfaceCard else Color.Transparent,
        shape = MaterialTheme.shapes.medium,
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, AccentPrimary) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = null, // Handled by selectable Modifier
                colors = RadioButtonDefaults.colors(selectedColor = AccentPrimary, unselectedColor = TextTertiary)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(option.label, color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
                if (option.badge != null) {
                    Text(option.badge, color = AccentPrimary, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

data class RythmOption(val days: Int, val label: String, val badge: String?)

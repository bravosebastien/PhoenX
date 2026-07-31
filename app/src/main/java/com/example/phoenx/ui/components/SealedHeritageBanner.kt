package com.example.phoenx.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.phoenx.ui.theme.LocalAppTheme
import com.example.phoenx.ui.theme.Warning

@Composable
fun SealedHeritageBanner(
    role: String,
    creatorName: String,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    
    val message = when(role) {
        "depositary" -> "L'héritage de $creatorName est scellé. En tant que Gardien, tu veilles sur son silence. Tu n'interviendras que pour confirmer son absence définitive."
        "recipient" -> "L'héritage de $creatorName est scellé. Tu y auras accès le moment venu, une fois que ses Gardiens auront ouvert sa mémoire."
        else -> "Cet héritage est actuellement scellé."
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Warning.copy(alpha = 0.1f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Warning.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Lock, null, tint = Warning, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = theme.contentColor
            )
        }
    }
}

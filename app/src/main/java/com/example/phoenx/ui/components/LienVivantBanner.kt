package com.example.phoenx.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoenx.ui.theme.LocalAppTheme

@Composable
fun LienVivantBanner(
    recipientName: String,
    recipientPhone: String? = null,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val context = LocalContext.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = accent.copy(alpha = 0.03f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Ce souvenir est prêt pour $recipientName.",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = theme.contentColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Et si vous lui envoyiez un petit message aujourd'hui de votre vivant pour prendre des nouvelles ?",
                style = MaterialTheme.typography.bodySmall,
                color = theme.contentColor.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = {
                    if (recipientPhone != null) {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("smsto:$recipientPhone")
                            putExtra("sms_body", "Coucou $recipientName, je pensais à toi...")
                        }
                        context.startActivity(intent)
                    } else {
                        // Ouvrir app messages générique
                        val intent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_APP_MESSAGING)
                        }
                        context.startActivity(intent)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.height(36.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Chat, null, modifier = Modifier.size(14.dp), tint = theme.backgroundColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Envoyer un mot doux", fontSize = 12.sp, color = theme.backgroundColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}

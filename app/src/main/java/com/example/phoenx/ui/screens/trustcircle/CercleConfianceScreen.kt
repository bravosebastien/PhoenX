package com.example.phoenx.ui.screens.trustcircle

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.R
import com.example.phoenx.ui.components.OnboardingPopup
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CercleConfianceScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProtocol: () -> Unit,
    onNavigateToWitnesses: () -> Unit,
    onNavigateToRecipients: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToCharacters: () -> Unit,
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val theme = LocalAppTheme.current
    val accent = theme.accentColor

    OnboardingPopup(
        pageKey = "trust_circle",
        title = stringResource(R.string.trust_circle_onboarding_title),
        contentPoints = stringArrayResource(R.array.trust_circle_onboarding_points).toList(),
        preferenceManager = themeViewModel.preferenceManager
    )

    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.trust_circle_title), 
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = theme.fontFamily, 
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Bold
                        ),
                        color = theme.contentColor
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
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
                stringResource(R.string.trust_circle_description),
                style = MaterialTheme.typography.bodyMedium,
                color = theme.contentColor.copy(alpha = 0.7f),
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 1. DÉPOSITAIRES
            TrustHubItem(
                title = stringResource(R.string.trust_circle_item_depositaries_title),
                subtitle = stringResource(R.string.trust_circle_item_depositaries_subtitle),
                icon = Icons.Default.Lock,
                accent = accent,
                onClick = onNavigateToProtocol
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. TÉMOINS
            TrustHubItem(
                title = stringResource(R.string.trust_circle_item_witnesses_title),
                subtitle = stringResource(R.string.trust_circle_item_witnesses_subtitle),
                icon = Icons.Default.People,
                accent = accent,
                onClick = onNavigateToWitnesses
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. DESTINATAIRES
            TrustHubItem(
                title = stringResource(R.string.trust_circle_item_recipients_title),
                subtitle = stringResource(R.string.trust_circle_item_recipients_subtitle),
                icon = Icons.Default.Person,
                accent = accent,
                onClick = onNavigateToRecipients
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4. NOTIFICATIONS
            TrustHubItem(
                title = stringResource(R.string.trust_circle_item_notifications_title),
                subtitle = stringResource(R.string.trust_circle_item_notifications_subtitle),
                icon = Icons.Default.NotificationsNone,
                accent = accent,
                onClick = onNavigateToNotifications
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 5. PERSONNAGES (v9.0)
            TrustHubItem(
                title = stringResource(R.string.trust_circle_item_characters_title),
                subtitle = stringResource(R.string.trust_circle_item_characters_subtitle),
                icon = Icons.Default.People,
                accent = accent,
                onClick = onNavigateToCharacters
            )

            Spacer(modifier = Modifier.height(48.dp))
            
            // Note de bas de page pédagogique
            Surface(
                color = accent.copy(alpha = 0.05f),
                shape = MaterialTheme.shapes.medium,
                border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        stringResource(R.string.trust_circle_privacy_title),
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.trust_circle_privacy_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.contentColor.copy(alpha = 0.7f),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TrustHubItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit
) {
    val theme = LocalAppTheme.current
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = theme.contentColor.copy(alpha = 0.05f)
        ),
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.medium,
                color = accent.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = accent, modifier = Modifier.size(24.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title, 
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = theme.fontFamily,
                        fontWeight = FontWeight.Bold
                    ), 
                    color = theme.contentColor
                )
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = theme.contentColor.copy(alpha = 0.6f))
            }
            
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = theme.contentColor.copy(alpha = 0.3f))
        }
    }
}

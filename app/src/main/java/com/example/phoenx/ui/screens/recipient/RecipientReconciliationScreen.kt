package com.example.phoenx.ui.screens.recipient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.R
import com.example.phoenx.ui.theme.LocalAppTheme
import com.example.phoenx.ui.theme.LocalBackgroundBrush
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipientReconciliationScreen(
    creatorId: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: RecipientMediaViewModel = hiltViewModel()
) {
    val entries by viewModel.reconciliationEntries.collectAsState()
    val creatorName by viewModel.creatorName.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor

    LaunchedEffect(creatorId) {
        viewModel.setTargetCreator(creatorId)
    }

    val dateFormat = stringResource(R.string.heir_heritage_date_format)
    val dateFormatter = remember(dateFormat) {
        DateTimeFormatter.ofPattern(dateFormat, Locale.FRENCH).withZone(ZoneId.systemDefault())
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        modifier = Modifier.background(LocalBackgroundBrush.current),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.recipient_reconciliation_screen_title),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = theme.fontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        color = theme.contentColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = theme.contentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(
                            R.string.recipient_reconciliation_empty_state,
                            creatorName.ifBlank { "votre proche" }
                        ),
                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                        color = theme.contentColor.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(entries) { entry ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = theme.contentColor.copy(alpha = 0.03f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                accent.copy(alpha = 0.25f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.Mail,
                                            contentDescription = null,
                                            tint = accent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = dateFormatter.format(entry.timestamp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = theme.contentColor.copy(alpha = 0.5f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = String(entry.encryptedContent),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = theme.fontFamily,
                                        lineHeight = 24.sp
                                    ),
                                    color = theme.contentColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

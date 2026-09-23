package com.example.phoenx.ui.screens.recipient

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.phoenx.R
import com.example.phoenx.ui.theme.LocalAppTheme
import androidx.media3.common.util.UnstableApi

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun RecipientMemoriesScreen(
    creatorId: String,
    navController: NavController,
    viewModel: RecipientMediaViewModel = hiltViewModel()
) {
    val heritageEntries by viewModel.heritageEntries.collectAsState()
    val heirKey by viewModel.heirKey.collectAsState()
    val theme = LocalAppTheme.current
    val context = LocalContext.current

    LaunchedEffect(creatorId) {
        viewModel.setTargetCreator(creatorId)
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.home_section_souvenirs),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = theme.fontFamily,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Bold
                        ),
                        color = theme.contentColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        if (heritageEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.home_last_memory_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = theme.contentColor.copy(alpha = 0.4f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(heritageEntries) { entry ->
                    HeritageEntryRow(
                        entry = entry,
                        heirKey = heirKey,
                        mediaManager = viewModel.mediaManager,
                        theme = theme,
                        creatorId = creatorId,
                        onClick = {
                            val contentStr = String(entry.encryptedContent)
                            val url = if (entry.mediaUrl?.startsWith("http") == true) entry.mediaUrl else contentStr

                            val isExternal = url.startsWith("http") &&
                                    (entry.mediaProvider != null || url.contains("spotify") || url.contains("youtube") || url.contains("deezer"))

                            if (isExternal) {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                    context.startActivity(intent)
                                } catch(_: Exception) {
                                    navController.navigate("recipient_memory_detail/${entry.id}/$creatorId")
                                }
                            } else {
                                navController.navigate("recipient_memory_detail/${entry.id}/$creatorId")
                            }
                        }
                    )
                }
            }
        }
    }
}

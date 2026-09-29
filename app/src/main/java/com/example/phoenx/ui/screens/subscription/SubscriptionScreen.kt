package com.example.phoenx.ui.screens.subscription

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.ui.theme.LocalAppTheme
import com.example.phoenx.ui.theme.LocalBackgroundBrush
import com.example.phoenx.ui.theme.phoenXMatiere
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.models.StoreTransaction
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    onNavigateBack: () -> Unit,
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val billing = uiState.billing
    val theme = LocalAppTheme.current
    val accent = theme.accentColor
    val context = LocalContext.current
    val activity = context as? Activity

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH) }
    var selectedTierForConfirmation by remember { mutableStateOf<Pair<com.example.phoenx.data.subscription.BillingTierInfo, Package>?>(null) }

    Scaffold(
        containerColor = theme.backgroundColor,
        modifier = Modifier.background(LocalBackgroundBrush.current),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Mon Abonnement",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = theme.fontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = theme.contentColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = theme.contentColor
                        )
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
            if (billing.isLoading && billing.tiers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = accent)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ── BANDEAU DU PALIER ACTUEL ──
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = theme.contentColor.copy(alpha = 0.05f)
                            ),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.3f))
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
                                    Column {
                                        Text(
                                            text = "Palier actif",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = theme.contentColor.copy(alpha = 0.6f)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = billing.currentTier?.nameFr ?: billing.currentTierKey,
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontFamily = theme.fontFamily,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = theme.contentColor
                                            )
                                            if (billing.isTrial) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = accent.copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        text = "Essai",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = accent,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                if (billing.expiresAtMillis != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Valide jusqu'au : ${dateFormat.format(Date(billing.expiresAtMillis))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = theme.contentColor.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }

                    // ── PROMOTIONS ACTIVES ──
                    if (billing.activePromotions.isNotEmpty()) {
                        items(billing.activePromotions) { promo ->
                            promo.bannerText?.let { text ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = accent.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = text,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = theme.contentColor,
                                        modifier = Modifier.padding(16.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // ── ERREURS EVENTUELLES ──
                    if (billing.loadError != null || uiState.offeringsError != null) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = billing.loadError ?: uiState.offeringsError ?: "Erreur",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(onClick = { viewModel.refresh() }) {
                                        Text("Réessayer", color = accent, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // ── LISTE DES PALIERS D'ABONNEMENT ──
                    items(billing.tiers) { tier ->
                        val isCurrent = tier.key == billing.currentTierKey
                        val targetProductId = tier.monthlyProductId ?: tier.annualProductId
                        val pkg = viewModel.packageForProductId(targetProductId)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (tier.highlight) accent.copy(alpha = 0.08f) else theme.contentColor.copy(alpha = 0.03f)
                            ),
                            border = BorderStroke(
                                if (tier.highlight) 2.dp else 1.dp,
                                if (tier.highlight) accent else theme.contentColor.copy(alpha = 0.15f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tier.nameFr,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = theme.fontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp
                                        ),
                                        color = theme.contentColor
                                    )

                                    if (isCurrent) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = accent
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = theme.backgroundColor,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Palier actuel",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = theme.backgroundColor
                                                )
                                            }
                                        }
                                    } else if (tier.highlight) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = accent.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "Recommandé",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = accent,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = tier.tagline,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = theme.contentColor.copy(alpha = 0.7f)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                if (!isCurrent) {
                                    if (pkg != null && activity != null) {
                                        Button(
                                            onClick = {
                                                selectedTierForConfirmation = tier to pkg
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .phoenXMatiere(),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = accent,
                                                contentColor = theme.backgroundColor
                                            )
                                        ) {
                                            Text(
                                                text = "Choisir ce palier (${pkg.product.price.formatted})",
                                                fontWeight = FontWeight.Bold,
                                                color = theme.backgroundColor
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "Bientôt disponible",
                                            style = MaterialTheme.typography.labelMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                                            color = theme.contentColor.copy(alpha = 0.4f),
                                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── DIALOGUE DE CONFIRMATION DE SOUSCRIPTION (ÉCRAN 1 LOT D) ──
        selectedTierForConfirmation?.let { (tier, pkg) ->
            if (activity != null) {
                SubscriptionConfirmationDialog(
                    targetTier = tier,
                    priceFormatted = pkg.product.price.formatted,
                    onConfirmPurchase = {
                        if (!Purchases.isConfigured) {
                            android.widget.Toast.makeText(
                                context,
                                "Service de paiement non disponible (SDK non configuré)",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            selectedTierForConfirmation = null
                            return@SubscriptionConfirmationDialog
                        }
                        try {
                            val purchaseParams = PurchaseParams.Builder(activity, pkg).build()
                            Purchases.sharedInstance.purchase(
                                purchaseParams,
                                object : PurchaseCallback {
                                    override fun onCompleted(
                                        storeTransaction: StoreTransaction,
                                        customerInfo: CustomerInfo
                                    ) {
                                        viewModel.refresh()
                                        selectedTierForConfirmation = null
                                    }

                                    override fun onError(
                                        error: PurchasesError,
                                        userCancelled: Boolean
                                    ) {
                                        selectedTierForConfirmation = null
                                    }
                                }
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("SubscriptionScreen", "Erreur lors de l'achat", e)
                            selectedTierForConfirmation = null
                        }
                    },
                    onDismiss = {
                        selectedTierForConfirmation = null
                    }
                )
            }
        }
    }
}

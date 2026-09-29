package com.example.phoenx.ui.screens.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.subscription.BillingRepository
import com.example.phoenx.data.subscription.BillingState
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubscriptionUiState(
    val billing: BillingState = BillingState(),
    val availablePackagesByProductId: Map<String, Package> = emptyMap(),
    val offeringsError: String? = null,
    val purchaseInProgress: Boolean = false,
    val purchaseError: String? = null
)

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val billingRepository: BillingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            billingRepository.state.collect { billing ->
                _uiState.value = _uiState.value.copy(billing = billing)
            }
        }
        billingRepository.refresh()
        loadOfferings()
    }

    fun refresh() {
        billingRepository.refresh()
        loadOfferings()
    }

    private fun loadOfferings() {
        if (!Purchases.isConfigured) {
            _uiState.value = _uiState.value.copy(
                offeringsError = "Clé API RevenueCat manquante dans local.properties (REVENUECAT_API_KEY=goog_...)"
            )
            return
        }
        try {
            Purchases.sharedInstance.getOfferings(object : ReceiveOfferingsCallback {
                override fun onReceived(offerings: Offerings) {
                    val current: Offering? = offerings.current
                    val map = mutableMapOf<String, Package>()
                    current?.availablePackages?.forEach { pkg ->
                        val productId = pkg.product.id.substringBefore(":") // ignore le suffixe de base plan Google Play
                        map[productId] = pkg
                    }
                    _uiState.value = _uiState.value.copy(availablePackagesByProductId = map, offeringsError = null)
                }

                override fun onError(error: PurchasesError) {
                    _uiState.value = _uiState.value.copy(offeringsError = error.message)
                }
            })
        } catch (e: Exception) {
            android.util.Log.e("SubscriptionVM", "Erreur lors du chargement des offres RevenueCat", e)
            _uiState.value = _uiState.value.copy(offeringsError = e.message ?: "Erreur inconnue")
        }
    }

    fun packageForProductId(productId: String?): Package? {
        if (productId == null) return null
        return _uiState.value.availablePackagesByProductId[productId]
    }
}

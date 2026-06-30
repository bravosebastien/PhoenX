package com.example.phoenx.ui.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    object Splash : Screen("splash")

    // GRAPHE CRÉATEUR
    object Onboarding : Screen("onboarding/{step}") {
        fun createRoute(step: Int) = "onboarding/$step"
    }
    
    object Auth : Screen("auth") {
        object Login : Screen("auth/login")
        object Signup : Screen("auth/signup")
        object Recovery : Screen("auth/recovery")
    }
    
    object Home : Screen("home")
    
    object Capture : Screen("capture/{type}?prompt={prompt}&pactId={pactId}&lat={lat}&lng={lng}&locationName={locationName}") {
        fun createRoute(type: String, prompt: String? = null, pactId: String? = null): String {
            val encodedPrompt = prompt?.let { URLEncoder.encode(it, StandardCharsets.UTF_8.toString()) }
            var route = "capture/$type"
            val params = mutableListOf<String>()
            if (encodedPrompt != null) params.add("prompt=$encodedPrompt")
            if (pactId != null) params.add("pactId=$pactId")
            if (params.isNotEmpty()) route += "?" + params.joinToString("&")
            return route
        }
        const val TYPE_TEXT = "TEXT"
        const val TYPE_AUDIO = "AUDIO"
        const val TYPE_PHOTO = "PHOTO"
        const val TYPE_NIGHT = "NIGHT"
    }
    
    object Fil : Screen("fil") {
        fun createRoute(ageYear: Int? = null) = if (ageYear != null) "fil/$ageYear" else "fil"
    }
    
    object YoungSelfLetters : Screen("youngselfletters")
    object NewYoungSelfLetter : Screen("youngselfletters/new/{targetAge}") {
        fun createRoute(targetAge: Int) = "youngselfletters/new/$targetAge"
    }
    
    object Worlds : Screen("worlds")
    object Library : Screen("library/preview")
    object Map : Screen("mappemonde")
    object MapRecipient : Screen("mappemonde_recipient")
    object LocationDetail : Screen("location_detail/{locationId}") {
        fun createRoute(locationId: String) = "location_detail/$locationId"
    }
    object Favorites : Screen("favorites")
    object Questions : Screen("questions")
    object PendingQuestions : Screen("questions/pending")
    object AskQuestion : Screen("ask_question/{creatorId}/{recipientId}") {
        fun createRoute(creatorId: String, recipientId: String) = "ask_question/$creatorId/$recipientId"
    }
    
    object Recipients : Screen("recipients")
    object RecipientDetail : Screen("recipients/{recipientId}") {
        fun createRoute(recipientId: String) = "recipients/$recipientId"
    }
    object RecipientPermissions : Screen("recipients/permissions/{recipientId}") {
        fun createRoute(recipientId: String) = "recipients/permissions/$recipientId"
    }
    
    object Portraits : Screen("portraits?recipientId={recipientId}") {
        fun createRoute(recipientId: String? = null) = 
            if (recipientId != null) "portraits?recipientId=$recipientId" else "portraits"
    }
    object NewPortrait : Screen("portraits/new/{recipientId}") {
        fun createRoute(recipientId: String) = "portraits/new/$recipientId"
    }
    
    object Pact : Screen("pact")
    object PactInvite : Screen("pact/invite")
    object PactDetail : Screen("pact/{pactId}") {
        fun createRoute(pactId: String) = "pact/$pactId"
    }
    
    object Legacy : Screen("legacy")
    object NewLegacy : Screen("legacy/new")
    object LegacyDetail : Screen("legacy/{id}") {
        fun createRoute(id: String) = "legacy/$id"
    }
    
    object Depositary : Screen("depositary")
    object DepositaryWelcome : Screen("depositary/welcome/{shortCode}") {
        fun createRoute(shortCode: String) = 
            "depositary/welcome/$shortCode"
    }
    object DepositaryDashboard : Screen("depositary/dashboard/{creatorId}") {
        fun createRoute(creatorId: String) = "depositary/dashboard/$creatorId"
    }
    object DepositaryActivation : Screen("depositary/activation/{creatorId}") {
        fun createRoute(creatorId: String) = "depositary/activation/$creatorId"
    }
    object DepositaryNotifications : Screen("depositary/notifications")

    object SilenceOnboarding : Screen("silence/onboarding")
    object SilenceCheckIn : Screen("silence/checkin")
    object SilenceBlock : Screen("silence/block")

    object Essence : Screen("essence")
    object UniqueKey : Screen("uniquekey")
    object Reconciliation : Screen("reconciliation")
    object Settings : Screen("settings")
    object ProtocolSettings : Screen("settings/protocol")
    object AccessibilitySettings : Screen("settings/accessibility")

    // GRAPHE DESTINATAIRE
    object RecipientWelcome : Screen("recipient/welcome")
    object RecipientCube : Screen("recipient/cube")
    object RecipientFil : Screen("recipient/fil")
    object RecipientLibrary : Screen("recipient/library")
    object RecipientDiscotheque : Screen("recipient/discotheque")
    object RecipientVideotheque : Screen("recipient/videotheque")
    object RecipientFavorites : Screen("recipient/favorites")
    object RecipientDetective : Screen("recipient/detective")
    object RecipientMessage : Screen("recipient/message/{id}") {
        fun createRoute(id: String) = "recipient/message/$id"
    }
    object RecipientMailbox : Screen("recipient/mailbox")
    object RecipientPortraits : Screen("recipient/portraits")
    object RecipientPact : Screen("recipient/pact/{pactId}") {
        fun createRoute(pactId: String) = "pact/$pactId"
    }
}

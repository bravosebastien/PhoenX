package com.example.phoenx.ui.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
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
    
    object Capture : Screen("capture/{type}?prompt={prompt}") {
        fun createRoute(type: String, prompt: String? = null): String {
            val encodedPrompt = prompt?.let { URLEncoder.encode(it, StandardCharsets.UTF_8.toString()) }
            return if (encodedPrompt != null) "capture/$type?prompt=$encodedPrompt" else "capture/$type"
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
    object Favorites : Screen("favorites")
    object Questions : Screen("questions")
    
    object Portraits : Screen("portraits")
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

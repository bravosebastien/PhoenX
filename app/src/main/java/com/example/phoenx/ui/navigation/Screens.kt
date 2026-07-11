package com.example.phoenx.ui.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    object Splash : Screen("splash")

    // GRAPHE CRÉATEUR
    object Onboarding : Screen("onboarding")
    
    object Auth : Screen("auth") {
        object Login : Screen("auth/login?redirectTo={redirectTo}") {
            fun createRoute(redirectTo: String? = null): String {
                return if (redirectTo != null) {
                    val encoded = URLEncoder.encode(redirectTo, StandardCharsets.UTF_8.toString())
                    "auth/login?redirectTo=$encoded"
                } else "auth/login"
            }
        }
        object Signup : Screen("auth/signup?redirectTo={redirectTo}") {
            fun createRoute(redirectTo: String? = null): String {
                return if (redirectTo != null) {
                    val encoded = URLEncoder.encode(redirectTo, StandardCharsets.UTF_8.toString())
                    "auth/signup?redirectTo=$encoded"
                } else "auth/signup"
            }
        }
        object Recovery : Screen("auth/recovery")
    }
    
    object Home : Screen("home")
    
    object Capture : Screen("capture/{type}?prompt={prompt}&pactId={pactId}&pendingQuestionId={pendingQuestionId}&lat={lat}&lng={lng}&locationName={locationName}&locationId={locationId}") {
        fun createRoute(
            type: String, 
            prompt: String? = null, 
            pactId: String? = null, 
            pendingQuestionId: String? = null,
            locationId: String? = null
        ): String {
            val encodedPrompt = prompt?.let { URLEncoder.encode(it, StandardCharsets.UTF_8.toString()) }
            var route = "capture/$type"
            val params = mutableListOf<String>()
            if (encodedPrompt != null) params.add("prompt=$encodedPrompt")
            if (pactId != null) params.add("pactId=$pactId")
            if (pendingQuestionId != null) params.add("pendingQuestionId=$pendingQuestionId")
            if (locationId != null) params.add("locationId=$locationId")
            if (params.isNotEmpty()) route += "?" + params.joinToString("&")
            return route
        }
        const val TYPE_TEXT = "TEXT"
        const val TYPE_AUDIO = "AUDIO"
        const val TYPE_PHOTO = "PHOTO"
        const val TYPE_GALLERY = "GALLERY"
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
    object Map : Screen("mappemonde?returnToEntryId={returnToEntryId}") {
        fun createRoute(returnToEntryId: String? = null): String =
            if (returnToEntryId != null) "mappemonde?returnToEntryId=$returnToEntryId" else "mappemonde"
    }
    object MapRecipient : Screen("mappemonde_recipient")
    object LocationDetail : Screen("location_detail/{locationId}") {
        fun createRoute(locationId: String) = "location_detail/$locationId"
    }
    object Favorites : Screen("favorites")
    object Questions : Screen("questions")
    object PendingQuestions : Screen("questions/pending")
    object DetectiveHome : Screen("detective/home")
    object MemoryDetail : Screen("memory_detail/{entryId}") {
        fun createRoute(entryId: String) = "memory_detail/$entryId"
    }
    object AskQuestion : Screen("ask_question/{creatorId}/{recipientId}") {
        fun createRoute(creatorId: String, recipientId: String) = "ask_question/$creatorId/$recipientId"
    }
    
    object Recipients : Screen("recipients")
    object RecipientDetail : Screen("recipients/{recipientId}") {
        fun createRoute(recipientId: String) = "recipients/$recipientId"
    }
    object RecipientPermissions : Screen("recipient_permissions/{recipientId}") {
        fun createRoute(recipientId: String) = "recipient_permissions/$recipientId"
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
    object Settings : Screen("settings") {
        fun createRoute(showRecovery: Boolean = false) = "settings?showRecovery=$showRecovery"
    }
    object ProtocolSettings : Screen("settings/protocol")
    object AccessibilitySettings : Screen("settings/accessibility")
    object NotificationContacts : Screen("notification_contacts")
    object QuizCreate : Screen("quiz_create")

    // GRAPHE DESTINATAIRE
    object RecipientWelcome : Screen("recipient/welcome")
    object RecipientCube : Screen("recipient/cube")
    object RecipientFil : Screen("recipient/fil")
    object RecipientLibrary : Screen("recipient/library")
    object RecipientDiscotheque : Screen("recipient/discotheque")
    object RecipientVideotheque : Screen("recipient/videotheque")
    object RecipientFavorites : Screen("recipient/favorites")
    object RecipientDetective : Screen("recipient/detective?creatorId={creatorId}") {
        fun createRoute(creatorId: String? = null) = 
            if (creatorId != null) "recipient/detective?creatorId=$creatorId" else "recipient/detective"
    }
    object RecipientMessage : Screen("recipient/message/{id}") {
        fun createRoute(id: String) = "recipient/message/$id"
    }
    object RecipientMailbox : Screen("recipient/mailbox")
    object RecipientPortraits : Screen("recipient/portraits")
    object RecipientPact : Screen("recipient/pact/{pactId}") {
        fun createRoute(pactId: String) = "recipient/pact/$pactId"
    }
}

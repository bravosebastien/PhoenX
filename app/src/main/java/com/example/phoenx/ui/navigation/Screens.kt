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
    
    object StepByStepCapture : Screen("capture/step_by_step") // v9.4.26

    object Capture : Screen("capture/{type}?prompt={prompt}&pactId={pactId}&pendingQuestionId={pendingQuestionId}&lat={lat}&lng={lng}&locationName={locationName}&locationId={locationId}&parentEntryId={parentEntryId}") {
        fun createRoute(
            type: String, 
            prompt: String? = null, 
            pactId: String? = null, 
            pendingQuestionId: String? = null,
            locationId: String? = null,
            locationName: String? = null, // v9.4.26
            parentEntryId: String? = null
        ): String {
            val encodedPrompt = prompt?.let { URLEncoder.encode(it, StandardCharsets.UTF_8.toString()) }
            val encodedLocName = locationName?.let { URLEncoder.encode(it, StandardCharsets.UTF_8.toString()) }
            var route = "capture/$type"
            val params = mutableListOf<String>()
            if (encodedPrompt != null) params.add("prompt=$encodedPrompt")
            if (pactId != null) params.add("pactId=$pactId")
            if (pendingQuestionId != null) params.add("pendingQuestionId=$pendingQuestionId")
            if (locationId != null) params.add("locationId=$locationId")
            if (encodedLocName != null) params.add("locationName=$encodedLocName")
            if (parentEntryId != null) params.add("parentEntryId=$parentEntryId")
            if (params.isNotEmpty()) route += "?" + params.joinToString("&")
            return route
        }
        const val TYPE_TEXT = "TEXT"
        const val TYPE_AUDIO = "AUDIO"
        const val TYPE_PHOTO = "PHOTO"
        const val TYPE_GALLERY = "GALLERY"
        const val TYPE_CAMERA_PHOTO = "CAMERA_PHOTO" // v9.4.27
        const val TYPE_CAMERA_VIDEO = "CAMERA_VIDEO" // v9.4.27
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
    object Map : Screen("mappemonde?returnToEntryId={returnToEntryId}&focusEntryId={focusEntryId}&targetCreatorId={targetCreatorId}") {
        fun createRoute(
            returnToEntryId: String? = null, 
            focusEntryId: String? = null, 
            targetCreatorId: String? = null
        ): String {
            val params = mutableListOf<String>()
            if (returnToEntryId != null) params.add("returnToEntryId=$returnToEntryId")
            if (focusEntryId != null) params.add("focusEntryId=$focusEntryId")
            if (targetCreatorId != null) params.add("targetCreatorId=$targetCreatorId")
            return if (params.isNotEmpty()) "mappemonde?" + params.joinToString("&") else "mappemonde"
        }
    }
    object MapRecipient : Screen("mappemonde_recipient")
    object LocationDetail : Screen("location_detail/{locationId}?targetCreatorId={targetCreatorId}") {
        fun createRoute(locationId: String, targetCreatorId: String? = null): String {
            return if (targetCreatorId != null) "location_detail/$locationId?targetCreatorId=$targetCreatorId"
            else "location_detail/$locationId"
        }
    }
    object Favorites : Screen("favorites")
    object Questions : Screen("questions")
    object PendingQuestions : Screen("questions/pending")
    object DetectiveHome : Screen("detective/home")
    object MemoryDetail : Screen("memory_detail/{entryId}?creatorId={creatorId}&triggerAction={triggerAction}") {
        fun createRoute(entryId: String, creatorId: String? = null, triggerAction: String? = null): String {
            var route = "memory_detail/$entryId"
            val params = mutableListOf<String>()
            if (creatorId != null) params.add("creatorId=$creatorId")
            if (triggerAction != null) params.add("triggerAction=$triggerAction")
            if (params.isNotEmpty()) route += "?" + params.joinToString("&")
            return route
        }
    }
    object MediaViewer : Screen("media_viewer/{entryId}?creatorId={creatorId}&mediaUrl={mediaUrl}&entryType={entryType}&aiSummary={aiSummary}&sourceDocType={sourceDocType}") {
        fun createRoute(
            entryId: String, 
            creatorId: String? = null,
            mediaUrl: String? = null,
            entryType: String? = null,
            aiSummary: String? = null,
            sourceDocType: String? = null
        ): String {
            var route = "media_viewer/$entryId"
            val params = mutableListOf<String>()
            if (creatorId != null) params.add("creatorId=$creatorId")
            if (mediaUrl != null) params.add("mediaUrl=${URLEncoder.encode(mediaUrl, StandardCharsets.UTF_8.toString())}")
            if (entryType != null) params.add("entryType=$entryType")
            if (aiSummary != null) params.add("aiSummary=${URLEncoder.encode(aiSummary, StandardCharsets.UTF_8.toString())}")
            if (sourceDocType != null) params.add("sourceDocType=$sourceDocType")
            
            if (params.isNotEmpty()) route += "?" + params.joinToString("&")
            return route
        }
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
    object RecipientAllocation : Screen("recipient_allocation/{recipientId}") {
        fun createRoute(recipientId: String) = "recipient_allocation/$recipientId"
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
    object DepositaryInfo : Screen("depositary/info")

    object SilenceOnboarding : Screen("silence/onboarding")
    object SilenceCheckIn : Screen("silence/checkin")
    object SilenceBlock : Screen("silence/block")

    object Essence : Screen("essence")
    object UniqueKey : Screen("uniquekey")
    object Reconciliation : Screen("reconciliation")
    object Profile : Screen("profile")
    object CreatorRichProfile : Screen("profile/rich")
    object TrustCircle : Screen("trust_circle")
    object Characters : Screen("characters")
    object CharacterEdit : Screen("characters/edit/{personId}") {
        fun createRoute(personId: String) = "characters/edit/$personId"
    }
    object WitnessInvite : Screen("witness_invite")
    object WitnessResponse : Screen("witness_response/{creatorId}/{witnessId}/{token}") {
        fun createRoute(creatorId: String, witnessId: String, token: String? = "none") = 
            "witness_response/$creatorId/$witnessId/$token"
    }
    object UniversalJoin : Screen("join/{token}") {
        fun createRoute(token: String) = "join/$token"
    }
    object Settings : Screen("settings") {
        fun createRoute(showRecovery: Boolean = false) = "settings?showRecovery=$showRecovery"
    }
    object ProtocolSettings : Screen("settings/protocol")
    object AccessibilitySettings : Screen("settings/accessibility")
    object NotificationContacts : Screen("notification_contacts")
    object QuizCreate : Screen("quiz_create")
    object Genealogy : Screen("genealogy?creatorId={creatorId}") {
        fun createRoute(creatorId: String? = null) = 
            if (creatorId != null) "genealogy?creatorId=$creatorId" else "genealogy"
    }
    object Encounters : Screen("encounters") // v9.5.0
    object EncountersList : Screen("encounters/list")
    object EncounterDetail : Screen("encounter_detail/{personId}?creatorId={creatorId}") {
        fun createRoute(personId: String, creatorId: String? = null): String {
            return if (creatorId != null) "encounter_detail/$personId?creatorId=$creatorId"
            else "encounter_detail/$personId"
        }
    }
    object EncountersMap : Screen("encounters/map")

    object BecomeCreatorPrompt : Screen("become_creator_prompt/{role}/{creatorName}") {
        fun createRoute(role: String, creatorName: String) = "become_creator_prompt/$role/$creatorName"
    }

    // GRAPHE APERÇU (v9.4.27)
    object Preview : Screen("preview") {
        object Root : Screen("preview/root/{recipientUid}") {
            fun createRoute(recipientUid: String) = "preview/root/$recipientUid"
        }
        object Fil : Screen("preview/fil/{recipientUid}") {
            fun createRoute(recipientUid: String) = "preview/fil/$recipientUid"
        }
        object MemoryDetail : Screen("preview/memory/{entryId}/{recipientUid}") {
            fun createRoute(entryId: String, recipientUid: String) = "preview/memory/$entryId/$recipientUid"
        }
        object Media : Screen("preview/media/{type}/{recipientUid}") {
            fun createRoute(type: String, recipientUid: String) = "preview/media/$type/$recipientUid"
        }
        object Book : Screen("preview/book/{recipientUid}") {
            fun createRoute(recipientUid: String) = "preview/book/$recipientUid"
        }
        object Vault : Screen("preview/vault/{recipientUid}") {
            fun createRoute(recipientUid: String) = "preview/vault/$recipientUid"
        }
        object Genealogy : Screen("preview/genealogy/{recipientUid}") {
            fun createRoute(recipientUid: String) = "preview/genealogy/$recipientUid"
        }
        object Encounters : Screen("preview/encounters/{recipientUid}") {
            fun createRoute(recipientUid: String) = "preview/encounters/$recipientUid"
        }
        object EncounterDetail : Screen("preview/encounter_detail/{personId}/{recipientUid}") {
            fun createRoute(personId: String, recipientUid: String) = "preview/encounter_detail/$personId/$recipientUid"
        }
    }

    // GRAPHE DESTINATAIRE
    object RecipientWelcome : Screen("recipient/welcome")
    object RecipientCube : Screen("recipient/cube/{creatorId}") {
        fun createRoute(creatorId: String) = "recipient/cube/$creatorId"
    }
    object HeirHeritage : Screen("recipient/heritage/{creatorId}") {
        fun createRoute(creatorId: String) = "recipient/heritage/$creatorId"
    }
    object RecipientFil : Screen("recipient/fil")
    object RecipientLibrary : Screen("recipient/library/{creatorId}") {
        fun createRoute(creatorId: String) = "recipient/library/$creatorId"
    }
    object RecipientEncounters : Screen("recipient/encounters/{creatorId}") {
        fun createRoute(creatorId: String) = "recipient/encounters/$creatorId"
    }
    object RecipientDiscotheque : Screen("recipient/discotheque/{creatorId}?filterRecipientId={filterRecipientId}") {
        fun createRoute(creatorId: String, filterRecipientId: String? = null) = 
            if (filterRecipientId != null) "recipient/discotheque/$creatorId?filterRecipientId=$filterRecipientId"
            else "recipient/discotheque/$creatorId"
    }
    object RecipientVideotheque : Screen("recipient/videotheque/{creatorId}?filterRecipientId={filterRecipientId}") {
        fun createRoute(creatorId: String, filterRecipientId: String? = null) = 
            if (filterRecipientId != null) "recipient/videotheque/$creatorId?filterRecipientId=$filterRecipientId"
            else "recipient/videotheque/$creatorId"
    }
    object RecipientPhotos : Screen("recipient/photos/{creatorId}?filterRecipientId={filterRecipientId}") {
        fun createRoute(creatorId: String, filterRecipientId: String? = null) = 
            if (filterRecipientId != null) "recipient/photos/$creatorId?filterRecipientId=$filterRecipientId"
            else "recipient/photos/$creatorId"
    }
    object RecipientFavorites : Screen("recipient/favorites/{creatorId}") {
        fun createRoute(creatorId: String) = "recipient/favorites/$creatorId"
    }
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

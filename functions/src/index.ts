/**
 * PHOEN-X Intelligence Layer - Core Entry Point
 * Explicit exports to avoid circular dependencies and analyzer issues.
 */

import {
    analyzeEntry,
    generateBiographerQuestion,
    generateEssencePortrait,
    detectThoughtEvolution,
    generateYoungSelfSuggestions,
    generateBookChapters,
    generateBookPlan,
    modifyBookChapter,
    generateGlobalIntro,
    askAssistant,
    checkPersonalityContent
} from "./ai";

import {
    getCreatorBookStatus,
    getCreatorProtocolStatus
} from "./book";

import {
    lockIdentity
} from "./identity";

import {
    generateDepositaryInviteToken,
    generateDepositaryShortCode,
    redeemDepositaryShortCode,
    joinAsDepositary,
    generateUniversalInvitation,
    getInvitationDetails,
    acceptUniversalInvitation,
    migrateLegacyRoles,
    sendMail,
    becomeCreator
} from "./invitations";

import {
    backfillRecipientUids,
    backfillDepositaryUids,
    onWitnessDeleted,
    onRecipientDeleted,
    onDepositaryDeleted,
    onUserDeletedCleanup
} from "./lifecycle";

import {
    getInheritedFileUrl
} from "./media";

import {
    getEntryComplements
} from "./entries";

import {
    getLivingLinkFileUrl,
    processScheduledLivingLinks
} from "./living_links";

import {
    checkCreatorSilence,
    activateProtocol,
    scheduledNotifications,
    resolveCreatorSilence,
    confirmCreatorProofOfLife,
    markEntryAutoUnlocked,
    checkDeathRecord
} from "./protocol";

import {
    notifyQuestionRightGranted,
    notifyNewPendingQuestion,
    sealPendingQuestion,
    onPendingQuestionUpdated,
    submitGuessResult
} from "./questions";

import {
    sendWitnessInvitation,
    verifyWitnessToken,
    submitWitnessTestimony,
    notifyNewTestimony
} from "./witnesses";

import { onUserCreated, onUserDeleted } from "./stats";

import { revenueCatWebhook } from "./subscriptions";

export {
    // AI
    analyzeEntry,
    generateBiographerQuestion,
    generateEssencePortrait,
    detectThoughtEvolution,
    generateYoungSelfSuggestions,
    generateBookChapters,
    generateBookPlan,
    modifyBookChapter,
    generateGlobalIntro,
    askAssistant,
    checkPersonalityContent,

    // Book
    getCreatorBookStatus,
    getCreatorProtocolStatus,

    // Identity
    lockIdentity,

    // Invitations
    generateDepositaryInviteToken,
    generateDepositaryShortCode,
    redeemDepositaryShortCode,
    joinAsDepositary,
    generateUniversalInvitation,
    getInvitationDetails,
    acceptUniversalInvitation,
    migrateLegacyRoles,
    sendMail,
    becomeCreator,

    // Lifecycle
    backfillRecipientUids,
    backfillDepositaryUids,
    onWitnessDeleted,
    onRecipientDeleted,
    onDepositaryDeleted,
    onUserDeletedCleanup,

    // Media
    getInheritedFileUrl,
    getEntryComplements,
    getLivingLinkFileUrl,
    processScheduledLivingLinks,

    // Protocol
    checkCreatorSilence,
    activateProtocol,
    scheduledNotifications,
    resolveCreatorSilence,
    confirmCreatorProofOfLife,
    markEntryAutoUnlocked,
    checkDeathRecord,

    // Questions
    notifyQuestionRightGranted,
    notifyNewPendingQuestion,
    sealPendingQuestion,
    onPendingQuestionUpdated,
    submitGuessResult,

    // Witnesses
    sendWitnessInvitation,
    verifyWitnessToken,
    submitWitnessTestimony,
    notifyNewTestimony,

    // Stats
    onUserCreated,
    onUserDeleted,

    // Subscriptions (paiement)
    revenueCatWebhook
};

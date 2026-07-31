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
    generateDistractors,
    modifyBookChapter,
    generateGlobalIntro
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
    sealPendingQuestion
} from "./questions";

import {
    sendWitnessInvitation,
    verifyWitnessToken,
    submitWitnessTestimony,
    notifyNewTestimony
} from "./witnesses";

export {
    // AI
    analyzeEntry,
    generateBiographerQuestion,
    generateEssencePortrait,
    detectThoughtEvolution,
    generateYoungSelfSuggestions,
    generateBookChapters,
    generateBookPlan,
    generateDistractors,
    modifyBookChapter,
    generateGlobalIntro,

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

    // Witnesses
    sendWitnessInvitation,
    verifyWitnessToken,
    submitWitnessTestimony,
    notifyNewTestimony
};

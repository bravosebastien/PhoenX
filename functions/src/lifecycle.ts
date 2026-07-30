import { onDocumentDeleted } from "firebase-functions/v2/firestore";
import * as functionsV1 from "firebase-functions/v1";
import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { db } from "./admin";

/**
 * PHOEN-X Lifecycle Module - Data propagation and cleanup
 */

/**
 * PHOEN-X v9.3.6/v9.3.7/v9.3.9 - Propagation de l'UID après liaison
 * Remplace l'ancien DocID par le vrai UID dans toutes les archives du Créateur.
 */
export async function propagateUidLiaison(creatorId: string, oldDocId: string, newUid: string) {
    const userRef = db.collection("users").doc(creatorId);
    const operations: { ref: admin.firestore.DocumentReference, data: any }[] = [];

    // 1. Collections avec tableaux recipientIds (v9.3.6)
    const arrayCollections = ["entries", "quizzes", "standaloneMedia"];
    for (const col of arrayCollections) {
        const snap = await userRef.collection(col)
            .where("recipientIds", "array-contains", oldDocId).get();

        snap.forEach(doc => {
            const currentIds = doc.data().recipientIds as string[];
            const updatedIds = currentIds.map(id => id === oldDocId ? newUid : id);
            operations.push({ ref: doc.ref, data: { recipientIds: updatedIds } });
        });
    }

    // TODO v9.3.9 : amendments sont des sous-collections de entries.
    // Nécessite une requête collectionGroup quand elles seront synchronisées.

    // 2. Collections avec champ simple recipientId (v9.3.6)
    const simpleCollections = [
        "pendingQuestions",
        "legacies",
        "pacts" // "pacts" INACTIF : PactViewModel n'écrit pas encore recipientId.
    ];
    for (const col of simpleCollections) {
        const snap = await userRef.collection(col)
            .where("recipientId", "==", oldDocId).get();
        snap.forEach(doc => {
            operations.push({ ref: doc.ref, data: { recipientId: newUid } });
        });
    }

    // 3. Mise à jour du Livre (Draft)
    const bookRef = userRef.collection("book").doc("current_draft");
    const bookDoc = await bookRef.get();
    if (bookDoc.exists) {
        const bookData = bookDoc.data()!;
        const currentIds = (bookData.recipientIds || []) as string[];
        if (currentIds.includes(oldDocId)) {
            const updatedIds = currentIds.map(id => id === oldDocId ? newUid : id);
            operations.push({ ref: bookRef, data: { recipientIds: updatedIds } });
        }
    }

    // ═══ EXÉCUTION PAR LOTS (v9.3.6) ═══
    for (let i = 0; i < operations.length; i += 450) {
        const batch = db.batch();
        const chunk = operations.slice(i, i + 450);
        chunk.forEach(op => batch.update(op.ref, op.data));
        await batch.commit();
    }

    console.log(`[LIAISON] Propagation v9.3.9 terminée : ${operations.length} documents mis à jour pour ${oldDocId} -> ${newUid}`);
}

/**
 * PHOEN-X v9.4.4 - Révocation d'accès après retrait du Cercle
 * Retire l'UID du Destinataire des tableaux ou annule les liens simples.
 */
export async function revokeUidAccess(creatorId: string, uidToRemove: string) {
    const userRef = db.collection("users").doc(creatorId);
    const operations: { ref: admin.firestore.DocumentReference, data: any }[] = [];

    // 1. Collections avec tableaux recipientIds
    const arrayCollections = ["entries", "quizzes", "standaloneMedia"];
    for (const col of arrayCollections) {
        const snap = await userRef.collection(col)
            .where("recipientIds", "array-contains", uidToRemove).get();

        snap.forEach(doc => {
            const currentIds = doc.data().recipientIds as string[];
            const updatedIds = currentIds.filter(id => id !== uidToRemove);
            operations.push({ ref: doc.ref, data: { recipientIds: updatedIds } });
        });
    }

    // 2. Collections avec champ simple recipientId
    const simpleCollections = ["pendingQuestions", "legacies"];
    for (const col of simpleCollections) {
        const snap = await userRef.collection(col)
            .where("recipientId", "==", uidToRemove).get();
        snap.forEach(doc => {
            operations.push({ ref: doc.ref, data: {
                recipientId: null,
                previousRecipientId: uidToRemove,
                revokedAt: admin.firestore.FieldValue.serverTimestamp()
            } });
        });
    }

    // 3. Mise à jour du Livre (Draft)
    const bookRef = userRef.collection("book").doc("current_draft");
    const bookDoc = await bookRef.get();
    if (bookDoc.exists) {
        const bookData = bookDoc.data()!;
        const currentIds = (bookData.recipientIds || []) as string[];
        if (currentIds.includes(uidToRemove)) {
            const updatedIds = currentIds.filter(id => id !== uidToRemove);
            operations.push({ ref: bookRef, data: { recipientIds: updatedIds } });
        }
    }

    // ═══ EXÉCUTION PAR LOTS ═══
    for (let i = 0; i < operations.length; i += 450) {
        const batch = db.batch();
        const chunk = operations.slice(i, i + 450);
        chunk.forEach(op => batch.update(op.ref, op.data));
        await batch.commit();
    }

    console.log(`[REVOCATION] v9.4.4 terminée : ${operations.length} documents révoqués pour ${uidToRemove} chez ${creatorId}`);
}

/**
 * Script de rattrapage (Backfill) - Réservé Admin
 */
export const backfillRecipientUids = onCall(async (request) => {
    if (request.auth?.uid !== "bLRNen7rArXinv5iQILx5OS3sxh2") {
        throw new HttpsError("permission-denied", "Accès administrateur requis");
    }

    const usersSnap = await db.collection("users").get();
    let totalFixed = 0;

    for (const userDoc of usersSnap.docs) {
        const recipientsSnap = await userDoc.ref.collection("recipients")
            .where("status", "==", "active").get();

        for (const recDoc of recipientsSnap.docs) {
            const data = recDoc.data();
            if (data.linkedUid && data.linkedUid !== recDoc.id) {
                await propagateUidLiaison(userDoc.id, recDoc.id, data.linkedUid);
                totalFixed++;
            }
        }
    }
    return { status: "success", recipientsProcessed: totalFixed };
});

/**
 * Script de rattrapage des Dépositaires (v9.4.4)
 */
export const backfillDepositaryUids = onCall(async (request) => {
    if (request.auth?.uid !== "bLRNen7rArXinv5iQILx5OS3sxh2") {
        throw new HttpsError("permission-denied", "Accès administrateur requis");
    }

    const usersSnap = await db.collection("users").get();
    let totalFixed = 0;

    for (const userDoc of usersSnap.docs) {
        const depositariesSnap = await userDoc.ref.collection("depositaries").get();
        const uids = [...new Set(
            depositariesSnap.docs
                .filter(doc => doc.data().status === "active" && !!doc.data().depositaryUid)
                .map(doc => doc.data().depositaryUid)
        )];

        if (uids.length > 0) {
            await userDoc.ref.update({ depositaryUids: uids });
            totalFixed++;
        }
    }
    return { status: "success", usersProcessed: totalFixed };
});

/**
 * TRIGGERS DE NETTOYAGE
 * Se déclenchent à la suppression d'un membre du cercle pour nettoyer son profil
 * et invalider les invitations en attente.
 */
export async function cleanupMemberRoles(creatorId: string, memberId: string, role: string, linkedUid?: string) {
    const collectionName = role === "depositary" ? "depositaries" : role === "witness" ? "witnesses" : "recipients";
    const sourcePath = `users/${creatorId}/${collectionName}/${memberId}`;

    // 1. Nettoyage myRoles sur le profil de l'invité
    if (linkedUid) {
        try {
            const roleKey = `${creatorId}_${role}`;
            await db.collection("users").doc(linkedUid).update({
                [`myRoles.${roleKey}`]: admin.firestore.FieldValue.delete()
            });
        } catch (e) {
            console.warn(`[CLEANUP] Impossible de nettoyer myRoles pour ${linkedUid} (Profil peut-être déjà supprimé)`);
        }

        // v9.4.4 : Retrait du tableau depositaryUids si c'est un Dépositaire
        if (role === "depositary") {
            await db.collection("users").doc(creatorId).update({
                depositaryUids: admin.firestore.FieldValue.arrayRemove(linkedUid)
            });
        }

        // v9.4.4 : Révocation des accès si c'est un Destinataire
        if (role === "recipient") {
            await revokeUidAccess(creatorId, linkedUid);
        }
    }

    // 2. Invalidation des invitations en attente pour ce membre
    const invitesSnap = await db.collection("invitations")
        .where("sourcePath", "==", sourcePath)
        .where("used", "==", false)
        .get();

    if (!invitesSnap.empty) {
        const batch = db.batch();
        invitesSnap.forEach(doc => {
            batch.update(doc.ref, {
                used: true,
                invalidatedAt: admin.firestore.FieldValue.serverTimestamp(),
                invalidationReason: "member_removed"
            });
        });
        await batch.commit();
    }
}

export const onWitnessDeleted = onDocumentDeleted(
    { document: "users/{creatorId}/witnesses/{witnessId}", region: "us-central1" },
    async (event) => {
    const { creatorId, witnessId } = event.params;
    const data = event.data?.data();
    if (!data) return;
    await cleanupMemberRoles(creatorId, witnessId, "witness", data.linkedUid);
});

export const onRecipientDeleted = onDocumentDeleted(
    { document: "users/{creatorId}/recipients/{recipientId}", region: "us-central1" },
    async (event) => {
    const { creatorId, recipientId } = event.params;
    const data = event.data?.data();
    if (!data) return;
    await cleanupMemberRoles(creatorId, recipientId, "recipient", data.linkedUid);
});

export const onDepositaryDeleted = onDocumentDeleted(
    { document: "users/{creatorId}/depositaries/{depositaryId}", region: "us-central1" },
    async (event) => {
    const { creatorId, depositaryId } = event.params;
    const data = event.data?.data();
    if (!data) return;
    const uid = data.linkedUid || data.depositaryUid;
    await cleanupMemberRoles(creatorId, depositaryId, "depositary", uid);
});

/**
 * PHOEN-X v7.2 - Nettoyage intégral après suppression Auth
 */
export const onUserDeletedCleanup = functionsV1.auth.user().onDelete(async (user) => {
    const { uid, email } = user;

    console.log(`[AUTH DELETE] Début du nettoyage pour ${uid} (${email})`);

    try {
        // SUPPRESSION STORAGE (RGPD v9.4.5)
        const bucket = admin.storage().bucket();
        await bucket.deleteFiles({ prefix: `users/${uid}/` });
        console.log(`[AUTH DELETE] Fichiers Storage users/${uid}/ supprimés.`);

        // 1. SUPPRESSION DU PROFIL (Priorité absolue)
        const userRef = db.collection("users").doc(uid);
        await db.recursiveDelete(userRef);
        console.log(`[AUTH DELETE] Profil users/${uid} supprimé.`);

        const batch = db.batch();
        let deletedCount = 0;

        // 2. RECHERCHE DES LIENS
        try {
            const witnessSnap = await db.collectionGroup("witnesses").where("linkedUid", "==", uid).get();
            const recipientSnap = await db.collectionGroup("recipients").where("linkedUid", "==", uid).get();
            const depositarySnap = await db.collectionGroup("depositaries").where("linkedUid", "==", uid).get();

            [...witnessSnap.docs, ...recipientSnap.docs, ...depositarySnap.docs].forEach(doc => {
                batch.delete(doc.ref);
                deletedCount++;
            });
        } catch (e) {
            console.warn("[AUTH DELETE] Les recherches groupées ont échoué", e);
        }

        // 3. RECHERCHE PAR EMAIL
        if (email) {
            try {
                const wEmail = await db.collectionGroup("witnesses").where("email", "==", email.toLowerCase()).get();
                const rEmail = await db.collectionGroup("recipients").where("email", "==", email.toLowerCase()).get();
                wEmail.docs.forEach(doc => { batch.delete(doc.ref); deletedCount++; });
                rEmail.docs.forEach(doc => { batch.delete(doc.ref); deletedCount++; });
            } catch (e) {
                console.warn("[AUTH DELETE] La recherche par email a échoué.", e);
            }
        }

        // 4. SUPPRESSION DES INVITATIONS & TÂCHES
        const invitesByCreator = await db.collection("invitations").where("creatorId", "==", uid).get();
        const invitesByAccepted = await db.collection("invitations").where("acceptedByUid", "==", uid).get();
        const tasksSnap = await db.collection("tasks").where("creatorId", "==", uid).get();

        invitesByCreator.docs.forEach(doc => { batch.delete(doc.ref); deletedCount++; });
        invitesByAccepted.docs.forEach(doc => { batch.delete(doc.ref); deletedCount++; });
        tasksSnap.docs.forEach(doc => { batch.delete(doc.ref); deletedCount++; });

        // 5. NETTOYAGE DES LOGS MAIL
        if (email) {
            const mailSnap = await db.collection("mail").where("to", "==", email).get();
            mailSnap.docs.forEach(doc => { batch.delete(doc.ref); deletedCount++; });
            console.log(`[AUTH DELETE] ${mailSnap.size} logs mail identifiés pour suppression.`);
        }

        await batch.commit();
        console.log(`[AUTH DELETE] ${deletedCount} documents liés nettoyés. Fin de procédure.`);

    } catch (error) {
        console.error(`[AUTH ERROR] Échec du nettoyage critique pour ${uid}:`, error);
    }
});

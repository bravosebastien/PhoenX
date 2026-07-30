import { onCall, HttpsError } from "firebase-functions/v2/https";
import { db } from "./admin";

/**
 * PHOEN-X v7.6 - Récupération sécurisée du statut du livre pour l'héritier
 */
export const getCreatorBookStatus = onCall(async (request) => {
    if (!request.auth) throw new HttpsError("unauthenticated", "Non authentifié");
    const { creatorId } = request.data;
    if (!creatorId) throw new HttpsError("invalid-argument", "ID Créateur manquant");

    const requesterUid = request.auth.uid;

    // 1. Vérification du rôle héritier
    const userDoc = await db.collection("users").doc(requesterUid).get();
    const myRoles = userDoc.data()?.myRoles || {};
    const hasRole = `${creatorId}_recipient` in myRoles;

    if (!hasRole) throw new HttpsError("permission-denied", "Vous n'êtes pas héritier de ce créateur.");

    // 2. Récupération des infos minimales du créateur
    const creatorDoc = await db.collection("users").doc(creatorId).get();
    if (!creatorDoc.exists) throw new HttpsError("not-found", "Créateur introuvable");

    const data = creatorDoc.data()!;

    // 3. Vérification des accès restreints au livre (v8.5.4)
    const bookDoc = await db.collection("users").doc(creatorId)
        .collection("book").doc("current_draft").get();

    if (bookDoc.exists) {
        const bookData = bookDoc.data();
        const recipientIds = bookData?.recipientIds || [];
        if (recipientIds.length > 0 && !recipientIds.includes(requesterUid)) {
             throw new HttpsError("permission-denied", "Ce livre ne vous est pas destiné.");
        }
    }

    return {
        displayName: data.displayName || "Votre proche",
        isBookOpen: data.protocolStatus === "activated"
    };
});

export const getCreatorProtocolStatus = onCall(async (request) => {
    if (!request.auth) throw new HttpsError("unauthenticated", "Non authentifié");
    const { creatorId } = request.data;
    if (!creatorId) throw new HttpsError("invalid-argument", "ID Créateur manquant");

    const requesterUid = request.auth.uid;

    // 1. Vérification du rôle héritier
    const userDoc = await db.collection("users").doc(requesterUid).get();
    const myRoles = userDoc.data()?.myRoles || {};
    const hasRole = `${creatorId}_recipient` in myRoles;

    if (!hasRole) throw new HttpsError("permission-denied", "Accès non autorisé.");

    const creatorDoc = await db.collection("users").doc(creatorId).get();
    if (!creatorDoc.exists) throw new HttpsError("not-found", "Créateur introuvable");

    return {
        protocolStatus: creatorDoc.data()?.protocolStatus || "pending",
        isActivated: creatorDoc.data()?.protocolStatus === "activated"
    };
});

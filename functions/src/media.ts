import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { db } from "./admin";

/**
 * PHOEN-X v9.4.2 - Génération d'URL signée pour l'héritage média
 */
export const getInheritedFileUrl = onCall(async (request) => {
    if (!request.auth) throw new HttpsError("unauthenticated", "Non authentifié");

    const { creatorId, docType, docId } = request.data;
    const requesterUid = request.auth.uid;

    const ALLOWED_TYPES = ["entries", "standaloneMedia", "book"];
    if (!ALLOWED_TYPES.includes(docType)) {
        throw new HttpsError("invalid-argument", "Type de document non supporté.");
    }

    const userDoc = await db.collection("users").doc(requesterUid).get();
    const myRoles = userDoc.data()?.myRoles || {};
    if (!(`${creatorId}_recipient` in myRoles)) {
        throw new HttpsError("permission-denied", "Accès réservé aux héritiers.");
    }

    const creatorDoc = await db.collection("users").doc(creatorId).get();
    if (creatorDoc.data()?.protocolStatus !== "activated") {
        throw new HttpsError("permission-denied", "Héritage encore scellé.");
    }

    const docRef = db.collection("users").doc(creatorId).collection(docType).doc(docId);
    const itemDoc = await docRef.get();
    if (!itemDoc.exists) throw new HttpsError("not-found", "Document introuvable.");

    const itemData = itemDoc.data()!;
    const recipientIds = (itemData.recipientIds || []) as string[];

    if (docType === "entries") {
        const visibility = itemData.visibility || "RESTRICTED";
        if (visibility !== "EVERYONE" && !recipientIds.includes(requesterUid)) {
            throw new HttpsError("permission-denied", "Accès refusé.");
        }
    } else {
        if (recipientIds.length > 0 && !recipientIds.includes(requesterUid)) {
            throw new HttpsError("permission-denied", "Accès refusé.");
        }
    }

    let storageUrl: string | null = null;
    if (docType === "entries") storageUrl = itemData.mediaUrl;
    else if (docType === "standaloneMedia" && itemData.type === "PHOTO") storageUrl = itemData.content;
    else if (docType === "book") storageUrl = itemData.coverImageUrl;

    if (!storageUrl) throw new HttpsError("not-found", "Aucun fichier.");

    let storagePath: string;
    if (storageUrl.startsWith("users/")) {
        storagePath = storageUrl;
    } else {
        const pathMatch = storageUrl.match(/\/o\/(.*?)\?alt=media/);
        if (!pathMatch) throw new HttpsError("internal", "Format d'URL Storage invalide.");
        storagePath = decodeURIComponent(pathMatch[1]);
    }

    if (!storagePath.startsWith(`users/${creatorId}/`)) {
        throw new HttpsError("permission-denied", "Chemin hors périmètre.");
    }

    const bucket = admin.storage().bucket();
    const [signedUrl] = await bucket.file(storagePath).getSignedUrl({
        action: 'read',
        expires: Date.now() + 15 * 60 * 1000,
    });

    return { url: signedUrl };
});

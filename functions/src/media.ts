import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { db } from "./admin";

/**
 * PHOEN-X v9.4.2 - Génération d'URL signée pour l'héritage média
 * Mise à jour v9.4.11 : Ajout du support pour les portraits Cameo (persons)
 */
export const getInheritedFileUrl = onCall(async (request) => {
    if (!request.auth) throw new HttpsError("unauthenticated", "Non authentifié");

    const { creatorId, docType, docId, field = "default" } = request.data;
    const requesterUid = request.auth.uid;

    // 1. Allowlist étendue aux portraits Cameo
    const ALLOWED_TYPES = ["entries", "standaloneMedia", "book", "persons", "personMedia"];
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

    const docRef = docType === "personMedia"
        ? db.collection("users").doc(creatorId).collection("persons").doc(request.data.personId).collection("media").doc(docId)
        : db.collection("users").doc(creatorId).collection(docType).doc(docId);
    const itemDoc = await docRef.get();
    if (!itemDoc.exists) throw new HttpsError("not-found", "Document introuvable.");

    const itemData = itemDoc.data()!;
    const recipientIds = (itemData.recipientIds || []) as string[];

    // 5. Vérification des permissions
    if (docType === "entries") {
        const visibility = itemData.visibility || "RESTRICTED";
        if (visibility !== "EVERYONE" && !recipientIds.includes(requesterUid)) {
            throw new HttpsError("permission-denied", "Accès refusé.");
        }
    } else {
        // standaloneMedia / book / persons (pas de recipientIds sur persons = accès à tous les héritiers)
        if (recipientIds.length > 0 && !recipientIds.includes(requesterUid)) {
            throw new HttpsError("permission-denied", "Accès refusé.");
        }
    }

    // 6. Extraction du chemin Storage (v9.4.27 : Support du paramètre field)
    let storageUrl: string | null = null;

    if (field === "coverUrl") {
        storageUrl = itemData.coverUrl;
    } else if (field === "encounterImagePath") { // v9.6.5 : Support pour portrait de Rencontre
        storageUrl = itemData.encounterImagePath;
    } else {
        // Fallback par défaut, aucun changement de comportement pour l'existant
        if (docType === "entries") storageUrl = itemData.mediaUrl;
        else if (docType === "standaloneMedia" && itemData.type === "PHOTO") storageUrl = itemData.content;
        else if (docType === "book") storageUrl = itemData.coverImageUrl;
        else if (docType === "persons") storageUrl = itemData.imageUrl;
        else if (docType === "personMedia") storageUrl = itemData.mediaPath;
    }

    if (!storageUrl) throw new HttpsError("not-found", "Aucun fichier.");

    // v9.4.27 : Tolérance au slash initial (Android vs Web/Functions SDKs)
    const normalizedUrl = storageUrl.startsWith("/") ? storageUrl.substring(1) : storageUrl;

    let storagePath: string;
    if (normalizedUrl.startsWith("users/")) {
        storagePath = normalizedUrl;
    } else {
        const pathMatch = normalizedUrl.match(/\/o\/(.*?)\?alt=media/);
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

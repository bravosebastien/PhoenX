import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { db } from "./admin";

/**
 * PHOEN-X v9.4.27 - Accès aux fichiers du module "Lien Vivant"
 * RÈGLE D'OR : Indépendant du protocole (protocolStatus ignoré)
 */
export const getLivingLinkFileUrl = onCall(async (request) => {
    if (!request.auth) throw new HttpsError("unauthenticated", "Non authentifié");

    const { linkId, fileIndex = 0 } = request.data;
    const requesterUid = request.auth.uid;

    const linkDoc = await db.collection("livingLinks").doc(linkId).get();
    if (!linkDoc.exists) throw new HttpsError("not-found", "Lien introuvable.");

    const linkData = linkDoc.data()!;

    // 1. Vérification d'accès : Destinataire (si envoyé) OU Créateur
    const isCreator = linkData.creatorId === requesterUid;
    const isRecipient = linkData.recipientId === requesterUid && linkData.status === "sent";

    if (!isCreator && !isRecipient) {
        throw new HttpsError("permission-denied", "Accès non autorisé à ce Lien Vivant.");
    }

    // 2. Extraction du chemin Storage
    const mediaUrls = (linkData.mediaUrls || []) as string[];
    if (fileIndex >= mediaUrls.length) {
        throw new HttpsError("not-found", "Fichier non trouvé pour cet index.");
    }

    const storagePath = mediaUrls[fileIndex];

    // Sécurité : Vérifier que le chemin appartient bien au dossier living_links
    if (!storagePath.startsWith(`users/${linkData.creatorId}/living_links/`)) {
        throw new HttpsError("permission-denied", "Chemin de fichier hors périmètre Living Link.");
    }

    const bucket = admin.storage().bucket();
    const [signedUrl] = await bucket.file(storagePath).getSignedUrl({
        action: 'read',
        expires: Date.now() + 15 * 60 * 1000, // 15 minutes
    });

    return { url: signedUrl };
});

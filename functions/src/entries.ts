import { onCall, HttpsError } from "firebase-functions/v2/https";
import { db } from "./admin";

/**
 * PHOEN-X v9.4.27 - Récupération sécurisée des compléments d'un souvenir pour un héritier.
 * Contourne les limitations des règles Firestore sur les requêtes filtrées.
 */
export const getEntryComplements = onCall({ region: "us-central1", invoker: "public" }, async (request) => {
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "L'utilisateur doit être authentifié.");
    }

    const { creatorId, entryId } = request.data;
    if (!creatorId || !entryId) {
        throw new HttpsError("invalid-argument", "creatorId et entryId sont requis.");
    }

    const requesterUid = request.auth.uid;

    // 1. Vérification du statut du protocole du créateur
    const creatorDoc = await db.collection("users").doc(creatorId).get();
    if (!creatorDoc.exists) {
        throw new HttpsError("not-found", "Créateur introuvable.");
    }

    if (creatorDoc.data()?.protocolStatus !== "activated") {
        throw new HttpsError("permission-denied", "Le protocole de transmission n'est pas activé.");
    }

    // 2. Vérification que l'appelant est bien un destinataire du créateur
    const requesterDoc = await db.collection("users").doc(requesterUid).get();
    const myRoles = requesterDoc.data()?.myRoles || {};
    if (!(`${creatorId}_recipient` in myRoles)) {
        throw new HttpsError("permission-denied", "Vous n'êtes pas autorisé à accéder à cet héritage.");
    }

    // 3. Récupération des compléments (SDK Admin pour contourner les règles)
    const complementsSnap = await db.collection("users").doc(creatorId)
        .collection("entries")
        .where("parentEntryId", "==", entryId)
        .get();

    // 4. Filtrage de sécurité manuel et conversion des Blobs pour le transport JSON
    const filteredComplements = complementsSnap.docs.map(doc => {
        const data = doc.data();
        const visibility = data.visibility || "RESTRICTED";

        // Normalisation défensive des destinataires (v9.4.27 : Support Tableau ou CSV)
        const rawRecipients = data.recipientIds || [];
        const recipientIds = Array.isArray(rawRecipients)
            ? rawRecipients
            : (typeof rawRecipients === "string" ? rawRecipients.split(",").map((s: string) => s.trim()) : []);

        if (visibility === "EVERYONE" || recipientIds.includes(requesterUid)) {
            const entry: any = { id: doc.id, ...data };

            // Transport sécurisé des Blobs (v9.4.27 : Conversion explicite avant transport JSON)
            if (data.encryptedContent && typeof data.encryptedContent.toBase64 === "function") {
                entry.encryptedContent = { "_base64": data.encryptedContent.toBase64() };
            }

            if (data.aiSummary && typeof data.aiSummary !== "string" && typeof data.aiSummary.toBase64 === "function") {
                entry.aiSummary = { "_base64": data.aiSummary.toBase64() };
            }

            if (data.aiTags && !Array.isArray(data.aiTags) && typeof data.aiTags !== "string" && typeof data.aiTags.toBase64 === "function") {
                entry.aiTags = { "_base64": data.aiTags.toBase64() };
            }

            return entry;
        }
        return null;
    }).filter(item => item !== null);

    return { complements: filteredComplements };
});

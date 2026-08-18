import { onCall, HttpsError } from "firebase-functions/v2/https";
import { db } from "./admin";

/**
 * Utilitaire de conversion Base64 robuste (v9.4.27)
 * Gère les Blobs Firestore et les Buffers Node.js
 */
function toBase64Safe(value: any): string | null {
    if (!value) return null;
    if (typeof value === "string") return value;
    if (typeof value.toBase64 === "function") return value.toBase64();
    if (Buffer.isBuffer(value)) return value.toString("base64");
    if (value instanceof Uint8Array) return Buffer.from(value).toString("base64");
    return null;
}

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

            // Transport sécurisé des Blobs (v9.4.27 : Utilisation de l'utilitaire robuste)
            const b64Content = toBase64Safe(data.encryptedContent);
            if (b64Content) entry.encryptedContent = { "_base64": b64Content };

            const b64Summary = toBase64Safe(data.aiSummary);
            if (b64Summary && typeof data.aiSummary !== "string") {
                entry.aiSummary = { "_base64": b64Summary };
            }

            const b64Tags = toBase64Safe(data.aiTags);
            if (b64Tags && !Array.isArray(data.aiTags) && typeof data.aiTags !== "string") {
                entry.aiTags = { "_base64": b64Tags };
            }

            return entry;
        }
        return null;
    }).filter(item => item !== null);

    if (filteredComplements.length > 0) {
        const first: any = filteredComplements[0];
        console.log(`[PHOENX_DEBUG_SERVER] Processing first complement: id=${first.id}`);
        console.log(`[PHOENX_DEBUG_SERVER] typeof aiSummary in result: ${typeof first.aiSummary}`);
        console.log(`[PHOENX_DEBUG_SERVER] aiSummary result structure: ${JSON.stringify(first.aiSummary).substring(0, 100)}`);
    }

    return { complements: filteredComplements };
});

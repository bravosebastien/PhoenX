import { onCall, HttpsError } from "firebase-functions/v2/https";
import { onDocumentCreated } from "firebase-functions/v2/firestore";
import * as admin from "firebase-admin";
import * as crypto from "crypto";
import { db, messaging } from "./admin";

/**
 * PHOEN-X Witnesses Module - Testimony management
 */

export const sendWitnessInvitation = onCall(async (request) => {
    if (request.auth?.uid !== request.data.creatorId) {
        throw new HttpsError("permission-denied", "Accès refusé");
    }
    const { creatorId, witnessId, witnessEmail, witnessName, creatorName } = request.data;
    const token = crypto.randomBytes(32).toString('hex');

    await db.collection("users").doc(creatorId).collection("witnesses").doc(witnessId).set({
        inviteToken: token,
        creatorName: creatorName
    }, { merge: true });

    const link = `https://phoenx.app/witness?creator=${creatorId}&witness=${witnessId}&token=${token}`;
    await db.collection("mail").add({ to: witnessEmail, message: { subject: `${creatorName} demande ton témoignage`, text: `Lien: ${link}` } });
    return { success: true };
});

export const verifyWitnessToken = onCall(async (request) => {
    const { creatorId, witnessId, token } = request.data;
    const doc = await db.collection("users").doc(creatorId).collection("witnesses").doc(witnessId).get();
    if (!doc.exists) throw new HttpsError("not-found", "Invitation introuvable");

    const witnessData = doc.data()!;

    // Vérification hybride v7.2 : Token valide OU UID lié
    const isAuthorized = (token && witnessData.inviteToken === token) ||
                         (request.auth && witnessData.linkedUid === request.auth.uid);

    if (!isAuthorized) throw new HttpsError("permission-denied", "Accès refusé");

    // Récupérer le nom du créateur depuis le profil
    const creatorDoc = await db.collection("users").doc(creatorId).get();
    const creatorData = creatorDoc.data();

    return {
        creatorName: creatorData?.displayName || witnessData.creatorName || "Ton proche",
        allowCreatorToRead: witnessData.allowCreatorToRead || false,
        allowCreatorToReject: witnessData.allowCreatorToReject || false,
        requestPrompt: witnessData.requestPrompt || null,
        publicEncryptionKey: creatorData?.publicEncryptionKey || null,
        submittedAt: witnessData.submittedAt || null
    };
});

export const submitWitnessTestimony = onCall(async (request) => {
    const { creatorId, witnessId, token, encryptedContent } = request.data;
    const ref = db.collection("users").doc(creatorId).collection("witnesses").doc(witnessId);
    const doc = await ref.get();

    if (!doc.exists) throw new HttpsError("not-found", "Document introuvable");
    const witnessData = doc.data()!;

    // Vérification hybride v7.2
    const isAuthorized = (token && witnessData.inviteToken === token) ||
                         (request.auth && witnessData.linkedUid === request.auth.uid);

    if (!isAuthorized) throw new HttpsError("permission-denied", "Accès refusé");

    const allowReject = witnessData.allowCreatorToReject || false;
    const finalStatus = allowReject ? "submitted" : "validated";

    await ref.update({
        content: encryptedContent,
        status: finalStatus,
        submittedAt: admin.firestore.FieldValue.serverTimestamp(),
        inviteToken: admin.firestore.FieldValue.delete()
    });

    return { success: true };
});

/**
 * PHOEN-X v7.2 - Notification de nouveau témoignage
 */
export const notifyNewTestimony = onDocumentCreated(
    { document: "users/{creatorId}/witnesses/{witnessId}", region: "us-central1" },
    async (event) => {
        const snapshot = event.data;
        if (!snapshot) return;

        const creatorId = event.params.creatorId;
        const witnessData = snapshot.data();

        // On ne notifie que si le témoignage est scellé (status submitted ou content présent)
        if (witnessData.status === "submitted" || witnessData.content) {
            const userDoc = await db.collection("users").doc(creatorId).get();
            const fcmToken = userDoc.data()?.fcmToken;

            if (fcmToken) {
                await messaging.send({
                    token: fcmToken,
                    notification: {
                        title: "Un nouveau témoignage est arrivé",
                        body: `${witnessData.name} vient de sceller son message dans ton Cercle.`
                    }
                });
            }
        }
    });

import { onCall, HttpsError } from "firebase-functions/v2/https";
import { onSchedule } from "firebase-functions/v2/scheduler";
import * as admin from "firebase-admin";
import { db, messaging } from "./admin";

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

/**
 * PHOEN-X v12.7.9 - Dépouillement périodique des Liens Vivants programmés.
 * Vérifie toutes les 15 minutes si des Liens Vivants programmés (status == "pending")
 * ont atteint leur date de déblocage (scheduledAt <= maintenant).
 * Passe leur statut à "sent", enregistre sentAt, et notifie le destinataire par FCM et Email.
 */
export const processScheduledLivingLinks = onSchedule({
    schedule: "every 15 minutes",
    timeZone: "Europe/Paris"
}, async (event) => {
    const now = admin.firestore.Timestamp.now();
    const pendingSnap = await db.collection("livingLinks")
        .where("status", "==", "pending")
        .where("scheduledAt", "<=", now)
        .get();

    if (pendingSnap.empty) {
        console.log("[processScheduledLivingLinks] Aucun Lien Vivant programmé en attente.");
        return;
    }

    console.log(`[processScheduledLivingLinks] ${pendingSnap.size} Lien(s) Vivant(s) à débloquer.`);

    const batch = db.batch();
    for (const doc of pendingSnap.docs) {
        batch.update(doc.ref, {
            status: "sent",
            sentAt: admin.firestore.FieldValue.serverTimestamp()
        });
    }

    await batch.commit();
    console.log(`[processScheduledLivingLinks] Statuts mis à jour à 'sent' pour ${pendingSnap.size} document(s).`);

    // Notifications Push & Email aux destinataires
    for (const doc of pendingSnap.docs) {
        try {
            const data = doc.data();
            const recipientId = data.recipientId;
            const creatorId = data.creatorId;

            if (!recipientId) continue;

            // Nom du créateur pour le message
            let creatorName = "Un proche";
            if (creatorId) {
                const creatorDoc = await db.collection("users").doc(creatorId).get();
                if (creatorDoc.exists) {
                    creatorName = creatorDoc.data()?.displayName || creatorDoc.data()?.email?.split("@")[0] || "Un proche";
                }
            }

            // Document du destinataire pour token FCM & email
            const recipientDoc = await db.collection("users").doc(recipientId).get();
            if (recipientDoc.exists) {
                const recipientData = recipientDoc.data();
                const fcmToken = recipientData?.fcmToken;
                const recipientEmail = recipientData?.email;

                // Push FCM
                if (fcmToken) {
                    await messaging.send({
                        token: fcmToken,
                        notification: {
                            title: "Nouveau Lien Vivant",
                            body: `${creatorName} vous a transmis un souvenir.`
                        }
                    });
                    console.log(`[processScheduledLivingLinks] Push FCM envoyé à ${recipientId}`);
                }

                // Email via déclencheur 'mail'
                if (recipientEmail) {
                    await db.collection("mail").add({
                        to: recipientEmail,
                        message: {
                            subject: "Un nouveau Lien Vivant vous attend sur PHOEN-X",
                            text: `Bonjour,\n\n${creatorName} vous a transmis un souvenir via PHOEN-X. Ouvrez l'application pour le découvrir.\n\nL'équipe PHOEN-X`
                        }
                    });
                    console.log(`[processScheduledLivingLinks] Email programmé pour ${recipientEmail}`);
                }
            }
        } catch (err: any) {
            console.error(`[processScheduledLivingLinks] Erreur notification pour document ${doc.id}:`, err);
        }
    }
});

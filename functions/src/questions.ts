import { onCall, HttpsError } from "firebase-functions/v2/https";
import { onDocumentCreated } from "firebase-functions/v2/firestore";
import * as admin from "firebase-admin";
import { db, messaging } from "./admin";

/**
 * PHOEN-X Questions Module - Sealed questions management
 */

// 17. Notification d'octroi du droit de poser des questions
export const notifyQuestionRightGranted = onCall(async (request) => {
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Non authentifié");
    }

    const { recipientId } = request.data;
    const creatorUid = request.auth.uid;

    // 1. Lecture sécurisée du destinataire (v9.3.6)
    const recipientDoc = await db.collection("users").doc(creatorUid).collection("recipients").doc(recipientId).get();
    if (!recipientDoc.exists) throw new HttpsError("permission-denied", "Destinataire introuvable");

    const recipientData = recipientDoc.data()!;
    const recipientEmail = recipientData.email;
    if (!recipientEmail) throw new HttpsError("failed-precondition", "Email du destinataire manquant");

    const recipientName = recipientData.name || "Proche";

    // 2. Récupérer le nom réel du créateur depuis son profil
    const creatorDoc = await db.collection("users").doc(creatorUid).get();
    const creatorName = creatorDoc.data()?.displayName || "Votre proche";

    const inviteLink = `https://phoenx.app/ask?creator=${creatorUid}&recipient=${recipientId}`;

    await db.collection("mail").add({
        to: recipientEmail,
        message: {
            subject: `${creatorName} t'invite à lui poser une question`,
            text: `${recipientName},\n\n${creatorName} t'a donné la possibilité de lui poser une ou plusieurs questions dans PHOEN-X.\n\nCes questions resteront scellées — tu n'auras la réponse qu'après son départ, le jour où son héritage te sera transmis.\n\nC'est une façon différente de garder le lien : poser aujourd'hui une question que tu n'as peut-être jamais osé formuler.\n\n${inviteLink}`
        }
    });
});

// 18. Notification au Créateur d'une nouvelle question
export const notifyNewPendingQuestion = onDocumentCreated(
    { document: "users/{userId}/pendingQuestions/{questionId}", region: "us-central1" },
    async (event) => {
        const snapshot = event.data;
        if (!snapshot) return;

        const userId = event.params.userId;
        const userDoc = await db.collection("users").doc(userId).get();
        const fcmToken = userDoc.data()?.fcmToken;

        if (fcmToken) {
            await messaging.send({
                token: fcmToken,
                notification: {
                    title: "Une nouvelle question t'attend",
                    body: "Quelqu'un t'a posé une question dans PHOEN-X."
                }
            });
        }
    });

// 19. Sceller une question (Côté Destinataire)
export const sealPendingQuestion = onCall(async (request) => {
    const { creatorId, recipientId, questionText } = request.data;

    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Non authentifié");
    }

    const recipientRef = db.collection("users").doc(creatorId).collection("recipients").doc(recipientId);
    const questionsCol = db.collection("users").doc(creatorId).collection("pendingQuestions");

    try {
        await db.runTransaction(async (transaction) => {
            const recipientDoc = await transaction.get(recipientRef);

            if (!recipientDoc.exists) {
                throw new HttpsError("not-found", "Destinataire introuvable");
            }

            const recipientData = recipientDoc.data()!;

            // ═══ SÉCURITÉ v9.3.4 : Vérification d'identité ═══
            if (recipientData.linkedUid !== request.auth!.uid) {
                throw new HttpsError("permission-denied", "Vous n'êtes pas autorisé à agir pour ce destinataire");
            }

            if (!recipientData.canAskQuestions) {
                throw new HttpsError(
                    "permission-denied",
                    "Ce destinataire n'est pas autorisé à poser des questions"
                );
            }

            // Vérifier la limite si définie
            const max = recipientData.maxQuestionsAllowed;
            const asked = recipientData.questionsAskedCount || 0;
            if (max !== null && max !== undefined && asked >= max) {
                throw new HttpsError(
                    "resource-exhausted",
                    "Limite de questions atteinte"
                );
            }

            // 1. Stocker la question chiffrée
            const newQuestionRef = questionsCol.doc();
            transaction.set(newQuestionRef, {
                recipientId,
                recipientName: recipientData.name || "",
                questionText, // déjà chiffré RSA côté client
                askedAt: admin.firestore.FieldValue.serverTimestamp(),
                status: "pending"
            });

            // 2. Incrémenter le compteur atomiquement
            transaction.update(recipientRef, {
                questionsAskedCount: admin.firestore.FieldValue.increment(1)
            });
        });

        return { success: true };
    } catch (error: any) {
        if (error instanceof HttpsError) throw error;
        throw new HttpsError("internal", error.message || "Erreur lors du scellage de la question");
    }
});

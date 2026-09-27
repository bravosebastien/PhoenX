import { onCall, HttpsError } from "firebase-functions/v2/https";
import { onDocumentCreated, onDocumentUpdated } from "firebase-functions/v2/firestore";
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

// 20. Trigger sur modification de question (Réponse, Déclin, Remboursement quota)
export const onPendingQuestionUpdated = onDocumentUpdated(
    { document: "users/{userId}/pendingQuestions/{questionId}", region: "us-central1" },
    async (event) => {
        const newData = event.data?.after.data();
        const oldData = event.data?.before.data();
        if (!newData || !oldData) return;

        const userId = event.params.userId;
        const status = newData.status;
        const oldStatus = oldData.status;

        // On ne réagit que lors d'un changement de statut depuis "pending"
        if (oldStatus !== "pending") return;

        const recipientId = newData.recipientId;
        if (!recipientId) return;

        // ════ 1. RÉCUPÉRATION DU CRÉDIT (SI DÉCLINÉ) ════
        if (status === "declined") {
            const recipientRef = db.collection("users").doc(userId).collection("recipients").doc(recipientId);
            await db.runTransaction(async (transaction) => {
                const rDoc = await transaction.get(recipientRef);
                if (rDoc.exists) {
                    const currentCount = rDoc.data()?.questionsAskedCount || 0;
                    // Sécurité : Ne jamais descendre sous 0
                    if (currentCount > 0) {
                        transaction.update(recipientRef, {
                            questionsAskedCount: admin.firestore.FieldValue.increment(-1)
                        });
                    }
                }
            });
        }

        // ════ 2. NOTIFICATION PUSH AU DESTINATAIRE ════
        if (status === "answered" || status === "declined") {
            // A. Trouver l'UID réel du destinataire (linkedUid)
            const recipientDoc = await db.collection("users").doc(userId).collection("recipients").doc(recipientId).get();
            const linkedUid = recipientDoc.data()?.linkedUid;

            if (linkedUid) {
                // B. Récupérer son token FCM
                const destUserDoc = await db.collection("users").doc(linkedUid).get();
                const fcmToken = destUserDoc.data()?.fcmToken;

                if (fcmToken) {
                    const message = status === "answered"
                        ? "Votre proche a répondu à votre question scellée."
                        : "Votre proche a pris connaissance de votre question scellée.";

                    await messaging.send({
                        token: fcmToken,
                        notification: {
                            title: "Question PHOEN-X",
                            body: message
                        }
                    });
                }
            }
        }
    });

/**
 * PHOEN-X v12.3 / v14.1 - Enregistrement sécurisé du résultat d'une devinette
 */
export const submitGuessResult = onCall(async (request) => {
    if (!request.auth) throw new HttpsError("unauthenticated", "Non authentifié");

    const { creatorId, entryId, answer, attemptCount } = request.data;
    const recipientUid = request.auth.uid;

    if (!creatorId || !entryId) {
        throw new HttpsError("invalid-argument", "Champs requis manquants.");
    }

    // 1a. Vérification que le document users/{creatorId} a protocolStatus === "activated"
    const creatorDoc = await db.collection("users").doc(creatorId).get();
    if (!creatorDoc.exists || creatorDoc.data()?.protocolStatus !== "activated") {
        throw new HttpsError("permission-denied", "Accès non autorisé à cette devinette.");
    }

    // 1b. Vérifier que l'appelant est bien un destinataire de ce créateur
    const userDoc = await db.collection("users").doc(recipientUid).get();
    const myRoles = userDoc.data()?.myRoles || {};
    const roleKey = `${creatorId}_recipient`;
    const hasRecipientRole = Array.isArray(myRoles)
        ? myRoles.includes(roleKey)
        : (roleKey in myRoles);

    if (!hasRecipientRole) {
        throw new HttpsError("permission-denied", "Accès non autorisé à cette devinette.");
    }

    // 1c. Vérification que le souvenir est destiné à l'appelant (visibility === "EVERYONE" ou recipientIds contient son uid)
    const entryRef = db.collection("users").doc(creatorId).collection("entries").doc(entryId);
    const entryDoc = await entryRef.get();
    if (!entryDoc.exists) throw new HttpsError("not-found", "Souvenir introuvable");

    const entryData = entryDoc.data()!;
    const visibility = entryData.visibility;
    const recipientIds = entryData.recipientIds;

    let isForMe = visibility === "EVERYONE";
    if (!isForMe && recipientIds) {
        if (Array.isArray(recipientIds)) {
            isForMe = recipientIds.includes(recipientUid);
        } else if (typeof recipientIds === "string") {
            isForMe = recipientIds.split(",").map((s: string) => s.trim()).includes(recipientUid);
        }
    }

    if (!isForMe) {
        throw new HttpsError("permission-denied", "Accès non autorisé à cette devinette.");
    }

    // 2. Limite de 20 essais par personne et par souvenir sur 24 h (guessAttempts/{recipientUid}_{entryId})
    const attemptRef = db.collection("guessAttempts").doc(`${recipientUid}_${entryId}`);
    const now = Date.now();
    const TWENTY_FOUR_HOURS_MS = 24 * 60 * 60 * 1000;

    await db.runTransaction(async (transaction) => {
        const attemptDoc = await transaction.get(attemptRef);
        if (!attemptDoc.exists) {
            transaction.set(attemptRef, { count: 1, windowStart: now });
        } else {
            const data = attemptDoc.data() || {};
            const windowStart = data.windowStart || now;
            const count = data.count || 0;

            if (now - windowStart > TWENTY_FOUR_HOURS_MS) {
                transaction.set(attemptRef, { count: 1, windowStart: now });
            } else {
                if (count >= 20) {
                    throw new HttpsError(
                        "resource-exhausted",
                        "Nombre maximal de tentatives dépassé pour aujourd'hui (20 max par 24h)."
                    );
                }
                transaction.update(attemptRef, { count: count + 1 });
            }
        }
    });

    // 3. Vérification de la réponse côté serveur
    const correctHash = entryData.enigmaAnswer;
    const fallbackAnswer = entryData.fallbackAnswer;
    const crypto = require("crypto");

    const rawStr = (answer || "").trim().toLowerCase();
    const withoutAccents = rawStr.normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/\s+/g, " ");
    const hashedInput = crypto.createHash("sha256").update(withoutAccents).digest("hex");
    const legacyHashedInput = crypto.createHash("sha256").update(rawStr).digest("hex");

    const isCorrect = (hashedInput === correctHash) || (hashedInput === fallbackAnswer) ||
                      (legacyHashedInput === correctHash) || (legacyHashedInput === fallbackAnswer);

    // 4. Déverrouillage permanent si correct (log sécurisé sans hash)
    console.log(`[submitGuessResult] entryId=${entryId}, isCorrect=${isCorrect}`);
    if (isCorrect) {
        console.log(`[submitGuessResult] Tentative d'écriture unlockedAt sur ${entryRef.path}...`);
        await entryRef.update({ unlockedAt: admin.firestore.FieldValue.serverTimestamp() });
        console.log(`[submitGuessResult] Écriture unlockedAt RÉUSSIE sur ${entryRef.path}`);
    } else {
        console.log(`[submitGuessResult] N'A PAS écrit unlockedAt car isCorrect=false`);
    }

    // 5. Récupérer le nom du destinataire pour le classement
    const recipientDoc = await db.collection("users").doc(creatorId).collection("recipients")
        .where("linkedUid", "==", recipientUid).limit(1).get();

    const recipientName = recipientDoc.empty ? "Anonyme" : (recipientDoc.docs[0].data().name || "Proche");

    // 6. Enregistrer le résultat dans la collection
    const resultRef = db.collection("users").doc(creatorId).collection("guessResults").doc();
    await resultRef.set({
        recipientId: recipientUid,
        recipientName,
        entryId,
        isCorrect,
        attemptCount,
        completedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    return { success: true, isCorrect };
});

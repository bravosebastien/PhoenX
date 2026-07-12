import { onCall, HttpsError } from "firebase-functions/v2/https";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { onDocumentCreated, onDocumentDeleted } from "firebase-functions/v2/firestore";
import * as admin from "firebase-admin";
import { VertexAI } from "@google-cloud/vertexai";
import axios from "axios";
import * as crypto from "crypto";

admin.initializeApp();

/**
 * PHOEN-X Intelligence Layer (v7.1)
 */

const PROJECT_ID = admin.instanceId().app.options.projectId;
const vertexAI = new VertexAI({ project: PROJECT_ID || "", location: "us-central1" });
const generativeModel = vertexAI.getGenerativeModel({
    model: "gemini-1.5-flash-001",
});

const AI_RULES = `
Tu es l'IA de PHOEN-X, une plateforme de mémoire vivante.
Tu traites des contenus personnels et intimes.
- Ne génère JAMAIS de contenu à la première personne du présent.
- Utilise TOUJOURS le conditionnel pour tes interprétations.
- Tu n'es jamais clinique. Tu es un accompagnateur chaleureux.
- Réponds UNIQUEMENT en JSON valide si demandé.
`;

const VALID_COMPARTMENTS = [
    "LIBRARY_BOOKS", "LIBRARY_MUSIC", "LIBRARY_VIDEO", "FIL_PENSEE",
    "LETTRES", "MES_MEILLEURS", "PHOTOS", "MAPPEMONDE", "CENT_QUESTIONS",
    "COFFRE_FORT", "TIROIR_SECRET", "LE_PACTE", "PORTRAIT_PROCHE", "RECONCILIATION"
];

async function sendSMSViaPartner(params: { to: string; body: string }): Promise<void> {
    try {
        await axios.post("https://api.smspartner.fr/v1/send", {
            apiKey: process.env.SMSPARTNER_API_KEY,
            phoneNumbers: params.to,
            sms: params.body,
            sender: "PHOENX"
        });
    } catch (error) {
        console.error("Erreur SMS Partner:", error);
    }
}

// 1. Analyse approfondie
export const analyzeEntry = onCall(async (request) => {
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Non authentifié");
    }
    const { summary } = request.data;
    if (!summary) throw new HttpsError("invalid-argument", "Résumé manquant");

    const prompt = `${AI_RULES} Analyse ce résumé en JSON (themes, persons, lifePeriod, emotionalTone, universalCategory, suggestedCompartments).
    universalCategory doit être l'une des valeurs suivantes : Amour, Espoir, Sagesse, Regret, Transmission, Foi, Réconciliation, Humanité, Gratitude.
    suggestedCompartments doit être un tableau de chaînes choisies UNIQUEMENT parmi cette liste : ${VALID_COMPARTMENTS.join(", ")}.
    Choisis les compartiments les plus pertinents où ranger ce souvenir.
    Résumé : ${summary}`;

    const result = await generativeModel.generateContent(prompt);
    const text = result.response.candidates?.[0]?.content.parts[0]?.text || "{}";

    const analysis = JSON.parse(text.replace(/```json|```/g, "").trim());

    // Filtrage de sécurité (Allowlist)
    if (Array.isArray(analysis.suggestedCompartments)) {
        analysis.suggestedCompartments = analysis.suggestedCompartments.filter(
            (comp: string) => VALID_COMPARTMENTS.includes(comp)
        );
    } else {
        analysis.suggestedCompartments = [];
    }

    return analysis;
});

// 2. Question du Biographe
export const generateBiographerQuestion = onCall(async (request) => {
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Non authentifié");
    }
    const { themes } = request.data;
    const prompt = `${AI_RULES} Génère UNE question de biographe (15 mots max). Thèmes : ${themes || "vie"}.`;
    const result = await generativeModel.generateContent(prompt);
    return result.response.candidates?.[0]?.content.parts[0]?.text || "Quel souvenir te fait sourire ?";
});

// 3. Portrait d'Essence
export const generateEssencePortrait = onCall(async (request) => {
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Non authentifié");
    }
    const { summaries } = request.data;
    if (!summaries?.length) return "Continue à déposer tes pensées...";
    const prompt = `${AI_RULES} Portrait d'Essence au CONDITIONNEL. Données : ${summaries.join(" | ")}`;
    const result = await generativeModel.generateContent(prompt);
    return result.response.candidates?.[0]?.content.parts[0]?.text || "";
});

// 4. Détection d'Évolution
export const detectThoughtEvolution = onCall(async (request) => {
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Non authentifié");
    }
    const { entriesByAge } = request.data;
    const prompt = `${AI_RULES} Transitions thématiques par âge en JSON. Données : ${JSON.stringify(entriesByAge)}`;
    const result = await generativeModel.generateContent(prompt);
    const text = result.response.candidates?.[0]?.content.parts[0]?.text || '{"transitions":[]}';
    return JSON.parse(text.replace(/```json|```/g, "").trim());
});

// 5. Suggestions Jeune Moi
export const generateYoungSelfSuggestions = onCall(async (request) => {
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Non authentifié");
    }
    const { targetAge, summariesAtThatAge } = request.data;
    const prompt = `${AI_RULES} Suggestions pour lettre à soi-même à ${targetAge} ans. Résumés: ${summariesAtThatAge.join(" | ")}`;
    const result = await generativeModel.generateContent(prompt);
    return result.response.candidates?.[0]?.content.parts[0]?.text || "";
});

// 8. Génération du livre
export const generateBookChapters = onCall(async (request) => {
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Non authentifié");
    }
    const { summaries, tags, ageMin, ageMax } = request.data;
    const prompt = `${AI_RULES} Rédige un livre de vie structuré. ${summaries.length} souvenirs, de ${ageMin} à ${ageMax} ans.`;
    const result = await generativeModel.generateContent(prompt);
    const text = result.response.candidates?.[0]?.content.parts[0]?.text || '{"chapters":[]}';
    return JSON.parse(text.replace(/```json|```/g, "").trim());
});

// 10. Surveillance du silence
export const checkCreatorSilence = onSchedule({
    schedule: "every 24 hours",
    secrets: ["SMSPARTNER_API_KEY"]
}, async (event) => {
    const db = admin.firestore();
    const snap = await db.collection("users").get();
    for (const doc of snap.docs) {
        const data = doc.data();
        const conf = data.silenceConfig;
        if (!conf?.lastCheckInAt) continue;
        const daysSinceLastCheckIn = Math.floor((Date.now() - conf.lastCheckInAt.toMillis()) / 86400000);
        const rhythmDays = conf.rhythmDays || 30;

        let l = 0;
        if (daysSinceLastCheckIn >= rhythmDays + 21) {
            l = 3; // Dépositaire notifié (Équivalent au point rouge pulsant)
        } else if (daysSinceLastCheckIn >= rhythmDays + 14) {
            l = 2; // 2ème relance Créateur
        } else if (daysSinceLastCheckIn >= rhythmDays + 7) {
            l = 1; // 1ère relance Créateur
        }

        if (l === (conf.escalationLevel || 0)) continue;
        await doc.ref.update({
            "silenceConfig.escalationLevel": l,
            "silenceConfig.missedCycles": l // Maintenir cohérence avec UI actuelle
        });
        if (l < 3) continue;
        const deps = await doc.ref.collection("depositaries").where("status", "==", "active").get();
        const target = l === 3 ? deps.docs.find(d => d.data().role === "primary") : deps.docs.find(d => d.data().role === "secondary");
        if (!target) continue;
        const msg = `PHOEN-X: ${data.displayName || "Un proche"} est silencieux depuis ${daysSinceLastCheckIn} jours. https://phoenx.app/depositary-alert?level=${l}&uid=${doc.id}`;
        const tData = target.data();
        if (tData.phone) await sendSMSViaPartner({ to: tData.phone, body: msg });
        if (tData.email) await db.collection("mail").add({ to: tData.email, message: { subject: "Action requise PHOEN-X", text: msg } });
    }
});

// 11. Activation protocole
export const activateProtocol = onCall(async (request) => {
    const { creatorId, depositaryId, contactAttemptNote,
            contactAttemptDetails, depositaryNote } = request.data;

    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Non authentifié");
    }

    // Vérifier que l'appelant est bien le Dépositaire déclaré
    const depositaryDoc = await admin.firestore()
        .collection("users").doc(creatorId)
        .collection("depositaries").doc(depositaryId)
        .get();

    if (!depositaryDoc.exists ||
        depositaryDoc.data()?.depositaryUid !== request.auth.uid) {
        throw new HttpsError("permission-denied", "Accès refusé");
    }

    // Récupérer le délai de contestation personnalisé du Créateur
    const creatorDoc = await admin.firestore()
        .collection("users").doc(creatorId).get();
    const thresholdHours = creatorDoc.data()?.silenceConfig?.thresholdHours ?? 72;
    const thresholdMillis = thresholdHours * 60 * 60 * 1000;

    // Vérifier la preuve de tentative de contact (Étape 0)
    const checkedCount = Object.values(contactAttemptDetails || {})
        .filter((v) => v === true).length;
    if (checkedCount < 2 || !contactAttemptNote ||
        contactAttemptNote.length < 20) {
        throw new HttpsError(
            "failed-precondition",
            "Confirmation de tentative de contact insuffisante"
        );
    }

    const now = admin.firestore.Timestamp.now();
    const contestDeadline = admin.firestore.Timestamp.fromMillis(
        now.toMillis() + thresholdMillis
    );

    const ref = await admin.firestore()
        .collection("activationProtocols").add({
            creatorId, depositaryId,
            status: "pending_contest",
            confirmedAt: now,
            contestDeadline,
            contactAttemptNote,
            contactAttemptDetails,
            depositaryNote: depositaryNote || null
        });

    await admin.firestore().collection("tasks").add({
        type: "notifyDeathContacts",
        creatorId: creatorId,
        scheduledFor: admin.firestore.Timestamp.fromMillis(Date.now() + thresholdMillis),
        status: "pending"
    });

    return { protocolId: ref.id, contestDeadline: contestDeadline.toMillis() };
});

// 12. Notification Contacts de Notification (Email sobre après 72h)
async function notifyDeathContactsInternal(creatorId: string): Promise<void> {
    const creatorDoc = await admin.firestore().collection("users").doc(creatorId).get();
    const creatorName = creatorDoc.data()?.displayName || "Votre proche";

    const contactsSnap = await admin.firestore().collection("users").doc(creatorId)
        .collection("notificationContacts").get();

    if (contactsSnap.empty) return;

    const emailPromises = contactsSnap.docs.map(doc => {
        const contact = doc.data();
        return admin.firestore().collection("mail").add({
            to: contact.email,
            message: {
                subject: "Un message important",
                text: `${contact.name || "Madame, Monsieur"},\n\n` +
                    `${creatorName} nous a quittés.\n` +
                    `Il/elle avait souhaité que vous soyez informé(e) de son départ.\n\n` +
                    `Avec nos sincères condoléances.`
            }
        });
    });

    await Promise.all(emailPromises);
}

export const scheduledNotifications = onSchedule("every 60 minutes", async () => {
    const now = admin.firestore.Timestamp.now();
    const tasksSnap = await admin.firestore().collection("tasks")
        .where("status", "==", "pending")
        .where("scheduledFor", "<=", now)
        .get();

    for (const taskDoc of tasksSnap.docs) {
        const task = taskDoc.data();
        try {
            if (task.type === "notifyDeathContacts") {
                await notifyDeathContactsInternal(task.creatorId);
            }
            await taskDoc.ref.update({ status: "done" });
        } catch (e) {
            console.error("Erreur tâche:", e);
            await taskDoc.ref.update({ status: "failed" });
        }
    }
});

// 16. Résolution silence
export const resolveCreatorSilence = onCall(async (request) => {
    const { creatorId, depositaryId, note } = request.data;

    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Non authentifié");
    }

    // Vérifier que l'appelant est bien le Dépositaire
    const depositaryDoc = await admin.firestore()
        .collection("users").doc(creatorId)
        .collection("depositaries").doc(depositaryId)
        .get();

    if (!depositaryDoc.exists ||
        depositaryDoc.data()?.depositaryUid !== request.auth.uid) {
        throw new HttpsError("permission-denied", "Accès refusé");
    }

    const db = admin.firestore();
    const batch = db.batch();

    const userRef = db.collection("users").doc(creatorId);
    batch.update(userRef, {
        "silenceConfig.missedCycles": 0,
        "silenceConfig.lastCheckInAt": admin.firestore.Timestamp.now(),
        "silenceConfig.lastSilenceStatus": "present",
        "silenceConfig.escalationLevel": 0
    });

    const notifRef = userRef.collection("silenceNotifications").doc();
    batch.set(notifRef, {
        type: "resolved_by_depositary",
        depositaryId,
        note: note || null,
        timestamp: admin.firestore.Timestamp.now()
    });

    await batch.commit();
    return { success: true };
});

// 17. Notification d'octroi du droit de poser des questions
export const notifyQuestionRightGranted = onCall(async (request) => {
    const { recipientEmail, recipientName, creatorName, inviteLink } = request.data;

    await admin.firestore().collection("mail").add({
        to: recipientEmail,
        message: {
            subject: `${creatorName} t'invite à lui poser une question`,
            text: `${recipientName},\n\n${creatorName} t'a donné la possibilité de lui poser une ou plusieurs questions dans PHOEN-X.\n\nCes questions resteront scellées — tu n'auras la réponse qu'après son départ, le jour où son héritage te sera transmis.\n\nC'est une façon différente de garder le lien : poser aujourd'hui une question que tu n'as peut-être jamais osé formuler.\n\n${inviteLink}`
        }
    });
});

// 18. Notification au Créateur d'une nouvelle question
export const notifyNewPendingQuestion = onDocumentCreated(
    "users/{userId}/pendingQuestions/{questionId}",
    async (event) => {
        const snapshot = event.data;
        if (!snapshot) return;

        const userId = event.params.userId;
        const userDoc = await admin.firestore().collection("users").doc(userId).get();
        const fcmToken = userDoc.data()?.fcmToken;

        if (fcmToken) {
            await admin.messaging().send({
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

    const db = admin.firestore();
    const recipientRef = db.collection("users").doc(creatorId).collection("recipients").doc(recipientId);
    const questionsCol = db.collection("users").doc(creatorId).collection("pendingQuestions");

    try {
        await db.runTransaction(async (transaction) => {
            const recipientDoc = await transaction.get(recipientRef);

            if (!recipientDoc.exists) {
                throw new HttpsError("not-found", "Destinataire introuvable");
            }

            const recipientData = recipientDoc.data()!;

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

// Fonctions d'invitation Dépositaire
export const generateDepositaryInviteToken = onCall(async (request) => {
    if (request.auth?.uid !== request.data.creatorId) {
        throw new HttpsError("permission-denied", "Accès refusé");
    }
    const { creatorId, depositaryId } = request.data;
    const token = crypto.randomBytes(32).toString('hex');
    await admin.firestore().collection("users").doc(creatorId).collection("depositaries").doc(depositaryId).set({ inviteToken: token, inviteTokenUsed: false }, { merge: true });
    return { token };
});

export const generateDepositaryShortCode = onCall(async (request) => {
    if (request.auth?.uid !== request.data.creatorId) {
        throw new HttpsError("permission-denied", "Accès refusé");
    }
    const { creatorId, depositaryId } = request.data;
    const code = crypto.randomBytes(4).toString('hex');
    await admin.firestore().collection("depositaryInviteCodes").doc(code).set({ creatorId, depositaryId, expiresAt: admin.firestore.Timestamp.fromMillis(Date.now() + 900000), used: false });
    return { shortCode: code };
});

export const redeemDepositaryShortCode = onCall(async (request) => {
    const { shortCode } = request.data;
    const ref = admin.firestore().collection("depositaryInviteCodes").doc(shortCode);
    const doc = await ref.get();
    if (!doc.exists || doc.data()?.expiresAt.toMillis() < Date.now() || doc.data()?.used) throw new HttpsError("permission-denied", "Invalide");
    await ref.update({ used: true });

    const creatorId = doc.data()?.creatorId;
    const depositaryId = doc.data()?.depositaryId;

    const creatorDoc = await admin.firestore().collection("users").doc(creatorId).get();
    const dDoc = await admin.firestore().collection("users").doc(creatorId).collection("depositaries").doc(depositaryId).get();

    return {
        creatorId: creatorId,
        depositaryId: depositaryId,
        token: dDoc.data()?.inviteToken,
        creatorName: creatorDoc.data()?.displayName || "Ton proche"
    };
});

export const joinAsDepositary = onCall(async (request) => {
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Non authentifié");
    }
    const { creatorId, depositaryId, token } = request.data;
    const depositaryUid = request.auth.uid;
    const ref = admin.firestore().collection("users").doc(creatorId).collection("depositaries").doc(depositaryId);
    const doc = await ref.get();

    if (!doc.exists || doc.data()?.inviteToken !== token || doc.data()?.inviteTokenUsed) {
        throw new HttpsError("permission-denied", "Invalide");
    }

    const batch = admin.firestore().batch();

    // 1. Liaison sur le document du Créateur
    batch.update(ref, {
        depositaryUid: depositaryUid,
        inviteTokenUsed: true
    });

    // 2. Lien inverse sur le document du Dépositaire (Approche 2 - Liste)
    const depositaryUserRef = admin.firestore().collection("users").doc(depositaryUid);
    batch.set(depositaryUserRef, {
        protectedCreatorIds: admin.firestore.FieldValue.arrayUnion(creatorId)
    }, { merge: true });

    await batch.commit();

    return { success: true };
});

// Témoins
export const sendWitnessInvitation = onCall(async (request) => {
    if (request.auth?.uid !== request.data.creatorId) {
        throw new HttpsError("permission-denied", "Accès refusé");
    }
    const { creatorId, witnessId, witnessEmail, witnessName, creatorName } = request.data;
    const token = crypto.randomBytes(32).toString('hex');

    // Correction : On enregistre aussi creatorName pour que verifyWitnessToken puisse le renvoyer au témoin
    await admin.firestore().collection("users").doc(creatorId).collection("witnesses").doc(witnessId).set({
        inviteToken: token,
        creatorName: creatorName
    }, { merge: true });

    const link = `https://phoenx.app/witness?creator=${creatorId}&witness=${witnessId}&token=${token}`;
    await admin.firestore().collection("mail").add({ to: witnessEmail, message: { subject: `${creatorName} demande ton témoignage`, text: `Lien: ${link}` } });
    return { success: true };
});

export const verifyWitnessToken = onCall(async (request) => {
    const { creatorId, witnessId, token } = request.data;
    const doc = await admin.firestore().collection("users").doc(creatorId).collection("witnesses").doc(witnessId).get();
    if (!doc.exists) throw new HttpsError("not-found", "Invitation introuvable");

    const witnessData = doc.data()!;

    // Vérification hybride v7.2 : Token valide OU UID lié
    const isAuthorized = (token && witnessData.inviteToken === token) ||
                         (request.auth && witnessData.linkedUid === request.auth.uid);

    if (!isAuthorized) throw new HttpsError("permission-denied", "Accès refusé");

    // Récupérer le nom du créateur depuis le profil
    const creatorDoc = await admin.firestore().collection("users").doc(creatorId).get();
    const creatorData = creatorDoc.data();

    return {
        creatorName: creatorData?.displayName || witnessData.creatorName || "Ton proche",
        allowCreatorToRead: witnessData.allowCreatorToRead || false,
        allowCreatorToReject: witnessData.allowCreatorToReject || false,
        publicEncryptionKey: creatorData?.publicEncryptionKey || null,
        submittedAt: witnessData.submittedAt || null
    };
});

export const submitWitnessTestimony = onCall(async (request) => {
    const { creatorId, witnessId, token, encryptedContent } = request.data;
    const ref = admin.firestore().collection("users").doc(creatorId).collection("witnesses").doc(witnessId);
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
 * PHOEN-X v7.2 - Liaison Universelle
 * Permet à un utilisateur (Dépositaire, Témoin, Destinataire) d'accepter son rôle
 * au sein d'une transaction atomique sécurisée.
 */
export const generateUniversalInvitation = onCall(async (request) => {
    const { email, role, sourceId, label, expiresHours } = request.data;
    const auth = request.auth;
    if (!auth) throw new HttpsError("unauthenticated", "Non authentifié");
    if (!sourceId) throw new HttpsError("invalid-argument", "sourceId manquant");

    // Récupérer le nom du créateur pour dénormalisation
    const creatorDoc = await admin.firestore().collection("users").doc(auth.uid).get();
    const creatorName = creatorDoc.data()?.displayName || "Votre proche";

    // Sécurisation v7.2 : On construit le chemin nous-mêmes pour éviter les injections
    let sourcePath = "";
    if (role === "depositary") sourcePath = `users/${auth.uid}/depositaries/${sourceId}`;
    else if (role === "witness") sourcePath = `users/${auth.uid}/witnesses/${sourceId}`;
    else if (role === "recipient") sourcePath = `users/${auth.uid}/recipients/${sourceId}`;
    else throw new HttpsError("invalid-argument", "Rôle invalide");

    const tokenId = crypto.randomBytes(32).toString('hex');
    const expiresAt = admin.firestore.Timestamp.fromMillis(Date.now() + (expiresHours || 168) * 3600000);

    const inviteData = {
        email: email.toLowerCase(),
        creatorId: auth.uid,
        creatorName,
        role,
        sourceId,
        sourcePath,
        label,
        expiresAt,
        used: false,
        createdAt: admin.firestore.FieldValue.serverTimestamp()
    };

    await admin.firestore().collection("invitations").doc(tokenId).set(inviteData);

    return { tokenId };
});

export const getInvitationDetails = onCall(async (request) => {
    const { tokenId } = request.data;
    if (!tokenId) throw new HttpsError("invalid-argument", "Token manquant");

    const inviteDoc = await admin.firestore().collection("invitations").doc(tokenId).get();
    if (!inviteDoc.exists) throw new HttpsError("not-found", "Invitation introuvable");

    const inviteData = inviteDoc.data()!;
    if (inviteData.expiresAt.toDate() < new Date()) throw new HttpsError("permission-denied", "Invitation expirée");
    if (inviteData.used) throw new HttpsError("already-exists", "Invitation déjà utilisée");

    const creatorDoc = await admin.firestore().collection("users").doc(inviteData.creatorId).get();

    return {
        creatorName: creatorDoc.data()?.displayName || "Votre proche",
        creatorId: inviteData.creatorId,
        role: inviteData.role,
        label: inviteData.label,
        targetEmail: inviteData.email
    };
});

export const acceptUniversalInvitation = onCall(async (request) => {
    const { tokenId } = request.data;
    const auth = request.auth;

    if (!auth || !auth.token.email) {
        throw new HttpsError("unauthenticated", "Vous devez être connecté avec un email valide.");
    }

    const userEmail = auth.token.email.toLowerCase();
    const db = admin.firestore();
    const inviteRef = db.collection("invitations").doc(tokenId);
    const userRef = db.collection("users").doc(auth.uid);

    try {
        return await db.runTransaction(async (transaction) => {
            const inviteDoc = await transaction.get(inviteRef);

            // 1. Existence
            if (!inviteDoc.exists) {
                throw new HttpsError("not-found", "Invitation introuvable.");
            }

            const inviteData = inviteDoc.data()!;
            const { creatorId, role } = inviteData;

            // 2. Expiration
            if (inviteData.expiresAt && inviteData.expiresAt.toDate() < new Date()) {
                throw new HttpsError("permission-denied", "Cette invitation a expiré.");
            }

            // 3. Idempotence & Réutilisation
            if (inviteData.used) {
                if (inviteData.acceptedByUid === auth.uid) {
                    return { status: "already_accepted", message: "Vous avez déjà accepté ce rôle." };
                } else {
                    throw new HttpsError("already-exists", "Cette invitation a déjà été utilisée par un autre compte.");
                }
            }

            // 4. Vérification Identitaire (Normalisée)
            if (inviteData.email.toLowerCase() !== userEmail) {
                throw new HttpsError("permission-denied", `Cette invitation est destinée à ${inviteData.email}.`);
            }

            // 6. FIX isCreator : On vérifie si l'utilisateur est déjà Créateur
            const userDoc = await transaction.get(userRef);
            const userData = userDoc.data();
            const currentIsCreator = userData?.isCreator === true;

            // 7. MISE À JOUR ATOMIQUE
            const roleKey = `${creatorId}_${role}`;
            const newRoleData = {
                creatorId: creatorId,
                creatorName: inviteData.creatorName || "Votre proche",
                role: role,
                status: "active",
                label: inviteData.label || role,
                joinedAt: admin.firestore.FieldValue.serverTimestamp(),
                sourceId: inviteData.sourceId || null
            };

            // Mise à jour du profil de l'invité
            transaction.set(userRef, {
                myRoles: { [roleKey]: newRoleData },
                isCreator: currentIsCreator // On préserve le statut true s'il existe
            }, { merge: true });

            // Marquage du token comme consommé
            transaction.update(inviteRef, {
                used: true,
                acceptedByUid: auth.uid,
                acceptedAt: admin.firestore.FieldValue.serverTimestamp()
            });

            // 8. Mise à jour du statut dans la liste du Créateur
            if (inviteData.sourcePath) {
                const sourceRef = db.doc(inviteData.sourcePath);
                transaction.update(sourceRef, {
                    status: "active",
                    linkedUid: auth.uid,
                    linkedAt: admin.firestore.FieldValue.serverTimestamp()
                });
            }

            return { status: "success", role: role };
        });
    } catch (error: any) {
        if (error instanceof HttpsError) throw error;
        console.error("Erreur acceptUniversalInvitation:", error);
        throw new HttpsError("internal", error.message || "Erreur lors de l'acceptation de l'invitation");
    }
});

export const migrateLegacyRoles = onCall(async (request) => {
    const uid = request.auth?.uid;
    if (!uid) throw new HttpsError("unauthenticated", "Utilisateur non connecté");

    const userRef = admin.firestore().collection("users").doc(uid);
    const doc = await userRef.get();
    const data = doc.data();

    if (!doc.exists || data?.myRoles) return { status: "already_migrated" };

    const legacyIds = data?.protectedCreatorIds || [];
    const newRoles: any = {};

    for (const creatorId of legacyIds) {
        const creatorDoc = await admin.firestore().collection("users").doc(creatorId).get();
        const creatorName = creatorDoc.data()?.displayName || "Votre proche";

        newRoles[`${creatorId}_depositary`] = {
            creatorId: creatorId,
            creatorName: creatorName,
            role: "depositary",
            status: "active",
            label: "Gardien de confiance",
            joinedAt: admin.firestore.FieldValue.serverTimestamp(),
            migratedAt: admin.firestore.FieldValue.serverTimestamp()
        };
    }

    await userRef.update({
        myRoles: newRoles,
        isCreator: !data?.isDepositaryOnly, // v7.2 Correct logic for existing creators
        migrationVersion: 7.2
    });

    return { status: "success", count: legacyIds.length };
});

/**
 * TRIGGERS DE NETTOYAGE (v7.2)
 * Se déclenchent à la suppression d'un membre du cercle pour nettoyer son profil
 * et invalider les invitations en attente.
 */

async function cleanupMemberRoles(creatorId: string, memberId: string, role: string, linkedUid?: string) {
    const db = admin.firestore();
    const collectionName = role === "depositary" ? "depositaries" : role === "witness" ? "witnesses" : "recipients";
    const sourcePath = `users/${creatorId}/${collectionName}/${memberId}`;

    // 1. Nettoyage myRoles sur le profil de l'invité
    if (linkedUid) {
        const roleKey = `${creatorId}_${role}`;
        await db.collection("users").doc(linkedUid).update({
            [`myRoles.${roleKey}`]: admin.firestore.FieldValue.delete()
        });
    }

    // 2. Invalidation des invitations en attente pour ce membre
    const invitesSnap = await db.collection("invitations")
        .where("sourcePath", "==", sourcePath)
        .where("used", "==", false)
        .get();

    if (!invitesSnap.empty) {
        const batch = db.batch();
        invitesSnap.forEach(doc => {
            batch.update(doc.ref, {
                used: true,
                invalidatedAt: admin.firestore.FieldValue.serverTimestamp(),
                invalidationReason: "member_removed"
            });
        });
        await batch.commit();
    }
}

export const onWitnessDeleted = onDocumentDeleted("users/{creatorId}/witnesses/{witnessId}", async (event) => {
    const { creatorId, witnessId } = event.params;
    const data = event.data?.data();
    if (!data) return;
    await cleanupMemberRoles(creatorId, witnessId, "witness", data.linkedUid);
});

export const onRecipientDeleted = onDocumentDeleted("users/{creatorId}/recipients/{recipientId}", async (event) => {
    const { creatorId, recipientId } = event.params;
    const data = event.data?.data();
    if (!data) return;
    await cleanupMemberRoles(creatorId, recipientId, "recipient", data.linkedUid);
});

export const onDepositaryDeleted = onDocumentDeleted("users/{creatorId}/depositaries/{depositaryId}", async (event) => {
    const { creatorId, depositaryId } = event.params;
    const data = event.data?.data();
    if (!data) return;
    const uid = data.linkedUid || data.depositaryUid; // Support historique et v7.2
    await cleanupMemberRoles(creatorId, depositaryId, "depositary", uid);
});

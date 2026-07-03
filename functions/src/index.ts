import { onCall, HttpsError } from "firebase-functions/v2/https";
import { onSchedule } from "firebase-functions/v2/scheduler";
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
    const { summary } = request.data;
    if (!summary) throw new HttpsError("invalid-argument", "Résumé manquant");
    const prompt = `${AI_RULES} Analyse ce résumé en JSON (themes, persons, lifePeriod, emotionalTone, universalCategory).
    universalCategory doit être l'une des valeurs suivantes : Amour, Espoir, Sagesse, Regret, Transmission, Foi, Réconciliation, Humanité, Gratitude.
    Résumé : ${summary}`;
    const result = await generativeModel.generateContent(prompt);
    const text = result.response.candidates?.[0]?.content.parts[0]?.text || "{}";
    return JSON.parse(text.replace(/```json|```/g, "").trim());
});

// 2. Question du Biographe
export const generateBiographerQuestion = onCall(async (request) => {
    const { themes } = request.data;
    const prompt = `${AI_RULES} Génère UNE question de biographe (15 mots max). Thèmes : ${themes || "vie"}.`;
    const result = await generativeModel.generateContent(prompt);
    return result.response.candidates?.[0]?.content.parts[0]?.text || "Quel souvenir te fait sourire ?";
});

// 3. Portrait d'Essence
export const generateEssencePortrait = onCall(async (request) => {
    const { summaries } = request.data;
    if (!summaries?.length) return "Continue à déposer tes pensées...";
    const prompt = `${AI_RULES} Portrait d'Essence au CONDITIONNEL. Données : ${summaries.join(" | ")}`;
    const result = await generativeModel.generateContent(prompt);
    return result.response.candidates?.[0]?.content.parts[0]?.text || "";
});

// 4. Détection d'Évolution
export const detectThoughtEvolution = onCall(async (request) => {
    const { entriesByAge } = request.data;
    const prompt = `${AI_RULES} Transitions thématiques par âge en JSON. Données : ${JSON.stringify(entriesByAge)}`;
    const result = await generativeModel.generateContent(prompt);
    const text = result.response.candidates?.[0]?.content.parts[0]?.text || '{"transitions":[]}';
    return JSON.parse(text.replace(/```json|```/g, "").trim());
});

// 5. Suggestions Jeune Moi
export const generateYoungSelfSuggestions = onCall(async (request) => {
    const { targetAge, summariesAtThatAge } = request.data;
    const prompt = `${AI_RULES} Suggestions pour lettre à soi-même à ${targetAge} ans. Résumés: ${summariesAtThatAge.join(" | ")}`;
    const result = await generativeModel.generateContent(prompt);
    return result.response.candidates?.[0]?.content.parts[0]?.text || "";
});

// 8. Génération du livre
export const generateBookChapters = onCall(async (request) => {
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
        const diff = Math.floor((Date.now() - conf.lastCheckInAt.toMillis()) / 86400000);
        const r = conf.rhythmDays || 30;
        let l = 0;
        if (diff >= r * 3) l = 4; else if (diff >= r * 2) l = 3; else if (diff >= r + 7) l = 2; else if (diff >= r) l = 1;
        if (l === (conf.escalationLevel || 0)) continue;
        await doc.ref.update({ "silenceConfig.escalationLevel": l });
        if (l < 3) continue;
        const deps = await doc.ref.collection("depositaries").where("status", "==", "active").get();
        const target = l === 3 ? deps.docs.find(d => d.data().role === "primary") : deps.docs.find(d => d.data().role === "secondary");
        if (!target) continue;
        const msg = `PHOEN-X: ${data.displayName || "Un proche"} est silencieux depuis ${diff} jours. https://phoenx.app/depositary-alert?level=${l}&uid=${doc.id}`;
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
        now.toMillis() + 72 * 60 * 60 * 1000
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

    return { protocolId: ref.id, contestDeadline: contestDeadline.toMillis() };
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

// Fonctions d'invitation Dépositaire
export const generateDepositaryInviteToken = onCall(async (request) => {
    const { creatorId, depositaryId } = request.data;
    const token = crypto.randomBytes(32).toString('hex');
    await admin.firestore().collection("users").doc(creatorId).collection("depositaries").doc(depositaryId).update({ inviteToken: token, inviteTokenUsed: false });
    return { token };
});

export const generateDepositaryShortCode = onCall(async (request) => {
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
    const dDoc = await admin.firestore().collection("users").doc(doc.data()?.creatorId).collection("depositaries").doc(doc.data()?.depositaryId).get();
    return { creatorId: doc.data()?.creatorId, depositaryId: doc.data()?.depositaryId, token: dDoc.data()?.inviteToken };
});

export const joinAsDepositary = onCall(async (request) => {
    const { creatorId, depositaryId, token } = request.data;
    const ref = admin.firestore().collection("users").doc(creatorId).collection("depositaries").doc(depositaryId);
    const doc = await ref.get();
    if (!doc.exists || doc.data()?.inviteToken !== token || doc.data()?.inviteTokenUsed) throw new HttpsError("permission-denied", "Invalide");
    await ref.update({ depositaryUid: request.auth?.uid, inviteTokenUsed: true });
    return { success: true };
});

// Témoins
export const sendWitnessInvitation = onCall(async (request) => {
    const { creatorId, witnessId, witnessEmail, witnessName, creatorName } = request.data;
    const token = crypto.randomBytes(32).toString('hex');
    await admin.firestore().collection("users").doc(creatorId).collection("witnesses").doc(witnessId).update({ inviteToken: token });
    const link = `https://phoenx.app/witness?creator=${creatorId}&witness=${witnessId}&token=${token}`;
    await admin.firestore().collection("mail").add({ to: witnessEmail, message: { subject: `${creatorName} demande ton témoignage`, text: `Lien: ${link}` } });
    return { success: true };
});

export const verifyWitnessToken = onCall(async (request) => {
    const { creatorId, witnessId, token } = request.data;
    const doc = await admin.firestore().collection("users").doc(creatorId).collection("witnesses").doc(witnessId).get();
    if (!doc.exists || doc.data()?.inviteToken !== token || doc.data()?.submittedAt) throw new HttpsError("permission-denied", "Invalide");
    return { creatorName: doc.data()?.creatorName || "Ton proche" };
});

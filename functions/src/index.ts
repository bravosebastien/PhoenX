import { onCall, HttpsError } from "firebase-functions/v2/https";
import { onSchedule } from "firebase-functions/v2/scheduler";
import * as admin from "firebase-admin";
import { VertexAI } from "@google-cloud/vertexai";
import axios from "axios";
import * as crypto from "crypto";

admin.initializeApp();

/**
 * PHOEN-X Intelligence Layer (v5.0)
 * Utilisation de Gemini 3.1 Flash Lite via Vertex AI.
 * RÈGLE D'OR : Ces fonctions ne traitent JAMAIS le texte brut chiffré,
 * mais uniquement les métadonnées (résumés IA) générées sur l'appareil.
 */

const PROJECT_ID = admin.instanceId().app.options.projectId;
const vertexAI = new VertexAI({ project: PROJECT_ID || "", location: "us-central1" });
const generativeModel = vertexAI.getGenerativeModel({
    model: "gemini-1.5-flash-001",
});

const AI_RULES = `
Tu es l'IA de PHOEN-X, une plateforme de mémoire vivante.
Tu traites des contenus personnels et intimes.
- Ne génère JAMAIS de contenu à la première personne du présent (ex: "Je suis").
- Utilise TOUJOURS le conditionnel pour tes interprétations.
- Tu n'es jamais clinique ou psychologique. Tu es un accompagnateur chaleureux.
- Réponds UNIQUEMENT en JSON valide si demandé.
`;

// Helper pour l'envoi de SMS via SMS Partner
async function sendSMSViaPartner(params: {
    to: string;
    body: string;
}): Promise<void> {
    try {
        await axios.post("https://api.smspartner.fr/v1/send", {
            apiKey: process.env.SMSPARTNER_API_KEY,
            phoneNumbers: params.to,
            sms: params.body,
            sender: "PHOENX"
        });
        console.log(`SMS envoyé avec succès à ${params.to}`);
    } catch (error) {
        console.error("Erreur envoi SMS SMS Partner:", error);
    }
}

// 1. Analyse approfondie d'une entrée (Trigger Firestore)
export const analyzeEntry = onCall(async (request) => {
    const { summary } = request.data;
    if (!summary) throw new HttpsError("invalid-argument", "Résumé manquant");

    const prompt = `${AI_RULES}
    Analyse ce résumé de pensée et réponds UNIQUEMENT en JSON :
    {
      "themes": ["max 3 parmi Famille, Amour, Travail, Santé, Peur, Joie, Regret, Sagesse, Enfance, Voyage, Amitié, Foi"],
      "persons": ["prénoms mentionnés"],
      "lifePeriod": "Enfance|Adolescence|JeuneAdulte|Adulte|Actuel",
      "emotionalTone": "Nostalgique|Joyeux|Mélancolique|Reconnaissant|Inquiet|Apaisé"
    }
    Résumé : ${summary}`;

    const result = await generativeModel.generateContent(prompt);
    const responseText = result.response.candidates?.[0]?.content.parts[0]?.text || "{}";
    const cleaned = responseText.replace(/```json|```/g, "").trim();
    return JSON.parse(cleaned);
});

// 2. Génération de la Question du Biographe (Hebdomadaire)
export const generateBiographerQuestion = onCall(async (request) => {
    const { themes } = request.data;

    const prompt = `${AI_RULES}
    Génère UNE question de biographe de maximum 15 mots.
    Elle doit être ouverte, jamais morbide, jamais clinique.
    Elle doit inciter à la transmission.
    Thèmes récents de l'utilisateur : ${themes || "vie en général"}.
    Réponds juste par la question entre guillemets.`;

    const result = await generativeModel.generateContent(prompt);
    return result.response.candidates?.[0]?.content.parts[0]?.text || "Quel est le souvenir qui te fait sourire aujourd'hui ?";
});

// 3. Portrait d'Essence (Synthèse de vie)
export const generateEssencePortrait = onCall(async (request) => {
    const { summaries } = request.data;
    if (!summaries || summaries.length === 0) return "Continue à déposer tes pensées pour que je puisse dessiner ton portrait d'essence.";

    const prompt = `${AI_RULES}
    Portrait d'Essence — Rédige 5 sections courtes au CONDITIONNEL :
    1. CE QUI REVIENT SOUVENT (thèmes récurrents)
    2. LES PERSONNES AU CŒUR (ceux qui comptent)
    3. CE QUI SEMBLE COMPTER (valeurs perçues)
    4. LES PÉRIODES RICHES (les moments de vie les plus narrés)
    5. UNE PHRASE SYNTHÈSE (poétique, 1 phrase)
    Données : ${summaries.join(" | ")}`;

    const result = await generativeModel.generateContent(prompt);
    return result.response.candidates?.[0]?.content.parts[0]?.text || "";
});

// 4. Détection d'Évolution (Dialogue Temporel)
export const detectThoughtEvolution = onCall(async (request) => {
    const { entriesByAge } = request.data;

    const prompt = `${AI_RULES}
    Identifie les transitions thématiques majeures par âge dans ces pensées.
    Réponds en JSON : {"transitions": [{"fromAge": number, "toAge": number, "description": "15 mots max, conditionnel"}]}
    Données : ${JSON.stringify(entriesByAge)}`;

    const result = await generativeModel.generateContent(prompt);
    const responseText = result.response.candidates?.[0]?.content.parts[0]?.text || '{"transitions":[]}';
    const cleaned = responseText.replace(/```json|```/g, "").trim();
    return JSON.parse(cleaned);
});

// 5. Suggestions pour Lettre à Mon Jeune Moi
export const generateYoungSelfSuggestions = onCall(async (request) => {
    const { targetAge, summariesAtThatAge } = request.data;

    const prompt = `${AI_RULES}
    L'utilisateur écrit à lui-même à ${targetAge} ans.
    À cet âge, il pensait à : ${summariesAtThatAge.join(" | ")}.
    Suggère 3 thèmes à aborder au conditionnel. 10 mots max chacun.
    Réponds en liste à puces.`;

    const result = await generativeModel.generateContent(prompt);
    return result.response.candidates?.[0]?.content.parts[0]?.text || "";
});

// 6. Synthèse Narrative d'un Portrait de Proche
export const generatePortraitNarrative = onCall(async (request) => {
    const { answers } = request.data;

    const prompt = `${AI_RULES}
    Rédige une synthèse narrative chaleureuse (3-5 phrases) du regard de l'utilisateur sur son proche.
    Utilise le conditionnel et la 1ère personne de l'utilisateur.
    Réponses aux questions : ${answers.join(" | ")}`;

    const result = await generativeModel.generateContent(prompt);
    return result.response.candidates?.[0]?.content.parts[0]?.text || "";
});

// 7. Aide à la Réconciliation
export const generateReconciliationHelp = onCall(async (request) => {
    const { recipient, intent } = request.data;

    const prompt = `${AI_RULES}
    L'utilisateur veut se réconcilier avec ${recipient}.
    Son intention est : ${intent}.
    Propose 3 façons différentes de formuler ce message (1 courte, 1 profonde, 1 explicative).
    Reste dans la bienveillance et le conditionnel.`;

    const result = await generativeModel.generateContent(prompt);
    return result.response.candidates?.[0]?.content.parts[0]?.text || "";
});

// 8. Génération du livre complet
export const generateBookChapters = onCall(async (request) => {
    const { summaries, tags, categoryCounts, places, ageMin, ageMax, totalEntries } = request.data;

    const prompt = `
Tu es un biographe de luxe, bienveillant et respectueux.
On te confie les jalons d'une vie humaine sous forme de métadonnées.
Ta mission : rédiger un livre de vie structuré en chapitres.

RÈGLES ABSOLUES :
- N'invente JAMAIS de faits non présents dans les données.
- Écris à la première personne (je, mon, ma, mes).
- Ton chaleureux, jamais morbide ni clinique.
- Entre 300 et 500 mots par chapitre.
- Réponds en JSON strict uniquement, sans markdown.
- Langue : français.
- Utilise le conditionnel pour les passages incertains.

DONNÉES DE CETTE VIE :
Nombre total de souvenirs : ${totalEntries}
Résumés des moments clés : ${summaries.slice(0, 50).join(" | ")}
Thèmes récurrents : ${tags.slice(0, 30).join(", ")}
Répartition émotionnelle : ${JSON.stringify(categoryCounts)}
Lieux de vie et voyages : ${places.slice(0, 20).join(", ")}
Période couverte : de ${ageMin} à ${ageMax}

STRUCTURE DES CHAPITRES (adapte selon les données) :
1. Les années fondatrices
2. Ce que j'ai construit
3. Ce que j'ai aimé
4. Les lieux de ma vie
5. Ce que je regrette
6. Ce que je transmets
(Omets les chapitres sans données suffisantes)

FORMAT JSON OBLIGATOIRE :
{
  "chapters": [
    {
      "orderIndex": 0,
      "title": "Titre du chapitre",
      "content": "Texte complet du chapitre..."
    }
  ]
}`;

    const result = await generativeModel.generateContent(prompt);
    const responseText = result.response.candidates?.[0]?.content.parts[0]?.text || "";
    const cleaned = responseText.replace(/```json|```/g, "").trim();
    const parsed = JSON.parse(cleaned);
    return { chapters: parsed.chapters };
});

// 9. Modification d'un chapitre par l'IA
export const modifyBookChapter = onCall(async (request) => {
    const { currentContent, instruction } = request.data;

    const prompt = `
Tu es un éditeur littéraire bienveillant.
Voici un chapitre d'un livre de vie :

---
${currentContent}
---

Instruction de l'auteur : "${instruction}"

Réécris ce chapitre en appliquant l'instruction.
Garde le même sens général.
Écris à la première personne.
Réponds UNIQUEMENT avec le nouveau texte,
sans explication ni markdown.`;

    const result = await generativeModel.generateContent(prompt);
    const newContent = result.response.candidates?.[0]?.content.parts[0]?.text || currentContent;
    return { newContent };
});

// 10. Surveillance du silence et alertes multi-canal (Réel avec SMS Partner)
export const checkCreatorSilence = onSchedule({
    schedule: "every 24 hours",
    secrets: ["SMSPARTNER_API_KEY"]
}, async (event) => {
    const db = admin.firestore();

    const usersSnapshot = await db.collection("users").get();

    for (const userDoc of usersSnapshot.docs) {
        const userData = userDoc.data();
        const userId = userDoc.id;
        const silenceConfig = userData.silenceConfig;

        if (!silenceConfig || !silenceConfig.lastCheckInAt) continue;

        const lastCheckIn = silenceConfig.lastCheckInAt.toDate();
        const now = new Date();
        const diffDays = Math.floor((now.getTime() - lastCheckIn.getTime()) / (1000 * 60 * 60 * 24));
        const rhythmDays = silenceConfig.rhythmDays || 30;

        let level = 0;
        if (diffDays >= rhythmDays * 3) level = 4; // Urgence (Secours)
        else if (diffDays >= rhythmDays * 2) level = 3; // Action demandée (Principal)
        else if (diffDays >= rhythmDays + 7) level = 2; // Rappel 2
        else if (diffDays >= rhythmDays) level = 1; // Rappel 1

        const previousLevel = silenceConfig.escalationLevel || 0;
        if (level === previousLevel) continue;

        await userDoc.ref.update({ "silenceConfig.escalationLevel": level });

        if (level < 3) continue;

        const creatorName = userData.displayName || "Un proche";
        const appLink = `https://phoenx.app/depositary-alert?level=${level}&uid=${userId}`;

        const depositariesSnap = await userDoc.ref.collection("depositaries").where("status", "==", "active").get();
        const primary = depositariesSnap.docs.find((d) => d.data().role === "primary");
        const secondary = depositariesSnap.docs.find((d) => d.data().role === "secondary");

        const target = level === 3 ? primary : secondary;
        if (!target) continue;

        const targetData = target.data();
        const message = level === 3
            ? `PHOEN-X : ${creatorName} n'a pas confirmé sa présence depuis ${diffDays} jours. Essaie de le/la contacter. Ouvre l'app : ${appLink}`
            : `PHOEN-X : ${creatorName} n'a pas confirmé sa présence depuis ${diffDays} jours, et le Dépositaire principal n'a pas répondu. Peux-tu essayer de le/la contacter ? ${appLink}`;

        // ENVOI RÉEL DU SMS VIA SMS PARTNER
        if (targetData.phone) {
            await sendSMSViaPartner({ to: targetData.phone, body: message });
        }

        // ENVOI RÉEL DE L'EMAIL
        if (targetData.email) {
            await db.collection("mail").add({
                to: targetData.email,
                message: {
                    subject: level === 3 ? `Action requise — ${creatorName}` : `Action requise — ${creatorName} (urgent)`,
                    text: message
                }
            });
        }

        await userDoc.ref.collection("silenceNotifications").add({
            escalationLevel: level,
            sentAt: admin.firestore.FieldValue.serverTimestamp(),
            daysSilent: diffDays,
            sentTo: target.id,
            channel: "sms+email"
        });
    }
});

// 11. Activation sécurisée du protocole
export const activateProtocol = onCall(async (request) => {
    const { creatorId, depositaryId, contactAttemptNote,
            contactAttemptDetails, depositaryNote } = request.data;

    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Non authentifié");
    }

    const depositaryDoc = await admin.firestore()
        .collection("users").doc(creatorId)
        .collection("depositaries").doc(depositaryId)
        .get();

    if (!depositaryDoc.exists || depositaryDoc.data()?.depositaryUid !== request.auth.uid) {
        throw new HttpsError("permission-denied", "Accès refusé");
    }

    const checkedCount = Object.values(contactAttemptDetails).filter((v) => v === true).length;
    if (checkedCount < 2 || !contactAttemptNote || contactAttemptNote.length < 20) {
        throw new HttpsError("failed-precondition", "Confirmation de tentative de contact insuffisante");
    }

    const now = admin.firestore.Timestamp.now();
    const contestDeadline = admin.firestore.Timestamp.fromMillis(now.toMillis() + 72 * 60 * 60 * 1000);

    const protocolRef = await admin.firestore()
        .collection("activationProtocols").add({
            creatorId,
            depositaryId,
            status: "pending_contest",
            confirmedAt: now,
            contestDeadline,
            contactAttemptNote,
            contactAttemptDetails,
            depositaryNote: depositaryNote || null
        });

    return { protocolId: protocolRef.id, contestDeadline: contestDeadline.toMillis() };
});

// 16. Résolution de l'alerte par le Dépositaire (SÉCURISÉ)
export const resolveCreatorSilence = onCall(async (request) => {
    const { creatorId, depositaryId, note } = request.data;

    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Non authentifié");
    }

    const depositaryRef = admin.firestore()
        .collection("users").doc(creatorId)
        .collection("depositaries").doc(depositaryId);

    const doc = await depositaryRef.get();
    if (!doc.exists || doc.data()?.depositaryUid !== request.auth.uid) {
        throw new HttpsError("permission-denied", "Accès refusé ou non lié");
    }

    const db = admin.firestore();
    const batch = db.batch();

    // 1. Reset du silence
    const userRef = db.collection("users").doc(creatorId);
    batch.update(userRef, {
        "silenceConfig.missedCycles": 0,
        "silenceConfig.lastCheckInAt": admin.firestore.Timestamp.now(),
        "silenceConfig.lastSilenceStatus": "present"
    });

    // 2. Log de la résolution
    const notifRef = userRef.collection("silenceNotifications").doc();
    batch.set(notifRef, {
        type: "resolved_by_depositary",
        depositaryId: depositaryId,
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
export const notifyNewPendingQuestion = admin.firestore
    .document("users/{userId}/pendingQuestions/{questionId}")
    .onCreate(async (snapshot, context) => {
        const userId = context.params.userId;
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

// 12. Génération d'un token d'invitation sécurisé
export const generateDepositaryInviteToken = onCall(async (request) => {
    const { creatorId, depositaryId } = request.data;
    if (!request.auth || request.auth.uid !== creatorId) {
        throw new HttpsError("permission-denied", "Seul le créateur peut générer une invitation.");
    }

    const token = crypto.randomBytes(32).toString('hex');

    await admin.firestore()
        .collection("users").doc(creatorId)
        .collection("depositaries").doc(depositaryId)
        .update({
            inviteToken: token,
            inviteTokenUsed: false
        });

    return { token };
});

// 13. Liaison sécurisée du Dépositaire via Token
export const joinAsDepositary = onCall(async (request) => {
    const { creatorId, depositaryId, token } = request.data;
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Non authentifié");
    }

    const depositaryRef = admin.firestore()
        .collection("users").doc(creatorId)
        .collection("depositaries").doc(depositaryId);

    const doc = await depositaryRef.get();
    if (!doc.exists) {
        throw new HttpsError("not-found", "Invitation introuvable.");
    }

    const data = doc.data();
    if (data?.inviteToken !== token || data?.inviteTokenUsed === true) {
        throw new HttpsError("permission-denied", "Token invalide ou déjà utilisé.");
    }

    await depositaryRef.update({
        depositaryUid: request.auth.uid,
        inviteTokenUsed: true
    });

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
export const notifyNewPendingQuestion = admin.firestore
    .document("users/{userId}/pendingQuestions/{questionId}")
    .onCreate(async (snapshot, context) => {
        const userId = context.params.userId;
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

// 14. Génération d'un code court temporaire (15 min)
export const generateDepositaryShortCode = onCall(async (request) => {
    const { creatorId, depositaryId } = request.data;
    if (!request.auth || request.auth.uid !== creatorId) {
        throw new HttpsError("permission-denied", "Accès refusé");
    }

    // Code court : 8 caractères alphanumériques
    const shortCode = crypto.randomBytes(4).toString('hex');

    // Stocké temporairement dans Firestore, expire dans 15 minutes
    await admin.firestore()
        .collection("depositaryInviteCodes")
        .doc(shortCode)
        .set({
            creatorId,
            depositaryId,
            expiresAt: admin.firestore.Timestamp.fromMillis(
                Date.now() + 15 * 60 * 1000
            ),
            used: false
        });

    return { shortCode };
});

// 15. Échange du code court contre le vrai token
export const redeemDepositaryShortCode = onCall(async (request) => {
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Non authentifié");
    }

    const { shortCode } = request.data;

    const codeRef = admin.firestore()
        .collection("depositaryInviteCodes")
        .doc(shortCode);
    const codeDoc = await codeRef.get();

    if (!codeDoc.exists) {
        throw new HttpsError("not-found", "Code introuvable");
    }

    const codeData = codeDoc.data()!;

    // Vérifier expiration
    if (codeData.expiresAt.toMillis() < Date.now()) {
        throw new HttpsError("deadline-exceeded", "Ce lien a expiré");
    }

    // Vérifier usage unique
    if (codeData.used === true) {
        throw new HttpsError("already-exists", "Ce lien a déjà été utilisé");
    }

    // Marquer comme utilisé immédiatement
    await codeRef.update({ used: true });

    // Récupérer le vrai token depuis le document Dépositaire
    const depositaryDoc = await admin.firestore()
        .collection("users").doc(codeData.creatorId)
        .collection("depositaries").doc(codeData.depositaryId)
        .get();

    const realToken = depositaryDoc.data()?.inviteToken;
    if (!realToken) {
        throw new HttpsError("not-found", "Token introuvable");
    }

    return {
        creatorId: codeData.creatorId,
        depositaryId: codeData.depositaryId,
        token: realToken
    };
});

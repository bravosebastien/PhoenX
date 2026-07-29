import { onCall, HttpsError } from "firebase-functions/v2/https";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { onDocumentCreated, onDocumentDeleted } from "firebase-functions/v2/firestore";
import * as functionsV1 from "firebase-functions/v1";
import * as admin from "firebase-admin";
import { GoogleGenAI } from "@google/genai";
import axios from "axios";
import * as crypto from "crypto";

admin.initializeApp();

/**
 * PHOEN-X Intelligence Layer (v8.2 - Real Migration to SDK @google/genai)
 */

const API_KEY = process.env.GEMINI_API_KEY || "";
const ai = new GoogleGenAI({ apiKey: API_KEY });
const AI_MODEL = "gemini-3.5-flash";

// Helper pour simplifier les appels avec le nouveau SDK pérenne
async function generateWithGemini(prompt: string): Promise<string> {
    try {
        const result = await ai.models.generateContent({
            model: AI_MODEL,
            contents: [prompt]
        });
        return result.text || "";
    } catch (e: any) {
        console.error(`[GEMINI ERROR] sur modèle ${AI_MODEL}:`, e.message);

        // Diagnostic : Liste des modèles si 404 détectée
        if (e.message.includes("404") || e.message.includes("not found")) {
            try {
                const modelsPager = await ai.models.list();
                console.log("[GEMINI DIAGNOSTIC] Modèles disponibles pour cette clé :");
                for await (const m of modelsPager) {
                    console.log(` - ${m.name} (${m.displayName})`);
                }
            } catch (listError) {
                console.error("[GEMINI DIAGNOSTIC] Impossible de lister les modèles:", listError);
            }
        }

        return ""; // Fallback propre
    }
}

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
export const analyzeEntry = onCall({
    secrets: ["GEMINI_API_KEY"]
}, async (request) => {
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

    const text = await generateWithGemini(prompt) || "{}";

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
export const generateBiographerQuestion = onCall({
    secrets: ["GEMINI_API_KEY"]
}, async (request) => {
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Non authentifié");
    }
    const { themes } = request.data;
    const prompt = `${AI_RULES} Génère UNE question de biographe (15 mots max). Thèmes : ${themes || "vie"}.`;
    return await generateWithGemini(prompt) || "Quel souvenir te fait sourire ?";
});

// 3. Portrait d'Essence
export const generateEssencePortrait = onCall({
    secrets: ["GEMINI_API_KEY"]
}, async (request) => {
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Non authentifié");
    }
    const { summaries } = request.data;
    if (!summaries?.length) return "Continue à déposer tes pensées...";
    const prompt = `${AI_RULES} Portrait d'Essence au CONDITIONNEL. Données : ${summaries.join(" | ")}`;
    return await generateWithGemini(prompt) || "";
});

// 4. Détection d'Évolution
export const detectThoughtEvolution = onCall({
    secrets: ["GEMINI_API_KEY"]
}, async (request) => {
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Non authentifié");
    }
    const { entriesByAge } = request.data;
    const prompt = `${AI_RULES} Transitions thématiques par âge en JSON. Données : ${JSON.stringify(entriesByAge)}`;
    const text = await generateWithGemini(prompt) || '{"transitions":[]}';
    return JSON.parse(text.replace(/```json|```/g, "").trim());
});

// 5. Suggestions Jeune Moi
export const generateYoungSelfSuggestions = onCall({
    secrets: ["GEMINI_API_KEY"]
}, async (request) => {
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Non authentifié");
    }
    const { targetAge, summariesAtThatAge } = request.data;
    const prompt = `${AI_RULES} Suggestions pour lettre à soi-même à ${targetAge} ans. Résumés: ${summariesAtThatAge.join(" | ")}`;
    return await generateWithGemini(prompt) || "";
});

// 8. Génération du livre (v7.6 Multimédia)
export const generateBookChapters = onCall({
    secrets: ["GEMINI_API_KEY"]
}, async (request) => {
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Non authentifié");
    }

    const { scenes, ageMin, ageMax, soulTone, plan, evolutionInsights } = request.data;
    if (!scenes || scenes.length === 0) throw new HttpsError("invalid-argument", "Pas de souvenirs à traiter");

    const toneInstruction = soulTone ? `Le ton de ce récit doit être : ${soulTone}.` : "Le ton doit être celui d'un biographe bienveillant, respectueux et narratif.";

    let planInstruction = "";
    if (plan && plan.length > 0) {
        planInstruction = `Respecte IMPÉRATIVEMENT ce plan de chapitres validé : ${JSON.stringify(plan)}.
        Chaque chapitre doit traiter uniquement les scenes dont les IDs sont listés pour lui.`;
    }

    const insightsInstruction = evolutionInsights ? `Utilise ces analyses sur l'évolution de la pensée de l'auteur pour donner du relief et de la profondeur au récit : ${evolutionInsights}` : "";

    const prompt = `${AI_RULES}
    Tu es le biographe attitré de l'utilisateur. Tu dois rédiger un Livre de Vie structuré en chapitres.
    ${toneInstruction}
    ${planInstruction}
    ${insightsInstruction}
    Données source (Scènes) : ${JSON.stringify(scenes)}

    Instructions :
    1. Rédige un récit fluide, à la première personne du singulier ("Je"), en couvrant la période de ${ageMin} à ${ageMax} ans.
    2. Pour chaque photo fournie (avec id et description), insère la balise [PHOTO:id_exact] à l'endroit le plus opportun dans ton texte. Utilise la description uniquement pour comprendre le contexte, ne l'affiche pas.
    3. Pour chaque enregistrement vocal (id et description), intègre son essence émotionnelle dans le récit. Tu peux aussi insérer une balise [AUDIO:id_exact] si c'est un message clé.
    4. Réponds UNIQUEMENT en JSON avec cette structure : {"chapters": [{"title": "Nom du chapitre", "content": "Texte avec balises [PHOTO:id] incluses", "orderIndex": 0}]}`;

    const text = await generateWithGemini(prompt) || '{"chapters":[]}';
    return JSON.parse(text.replace(/```json|```/g, "").trim());
});

// 8b. Génération du plan du livre (v9.3.1)
export const generateBookPlan = onCall({
    secrets: ["GEMINI_API_KEY"]
}, async (request) => {
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Non authentifié");
    }

    const { scenes } = request.data;
    if (!scenes || scenes.length === 0) throw new HttpsError("invalid-argument", "Pas de souvenirs à traiter");

    const prompt = `${AI_RULES}
    Tu es un architecte narratif. À partir des scènes de vie suivantes, propose un plan de livre cohérent.
    Regroupe les scènes par thèmes ou périodes chronologiques logiques.
    Scènes : ${JSON.stringify(scenes)}

    Instructions :
    1. Propose entre 3 et 8 chapitres.
    2. Pour chaque chapitre, donne un titre poétique et la liste des IDs des scènes incluses.
    3. Réponds UNIQUEMENT en JSON avec cette structure : {"plan": [{"title": "Titre du chapitre", "sceneIds": ["id1", "id2"], "description": "Brève intention narrative"}]}`;

    const text = await generateWithGemini(prompt) || '{"plan":[]}';
    return JSON.parse(text.replace(/```json|```/g, "").trim());
});

// 9. Génération de distracteurs (v8.3 Quiz 2.0)
export const generateDistractors = onCall({
    secrets: ["GEMINI_API_KEY"]
}, async (request) => {
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Non authentifié");
    }

    const { question, correctAnswer } = request.data;
    if (!question || !correctAnswer) throw new HttpsError("invalid-argument", "Question ou réponse manquante");

    const prompt = `${AI_RULES}
    Génère 3 fausses réponses (distracteurs) crédibles mais distinctes pour le quiz de l'utilisateur.
    Question : ${question}
    Vraie réponse : ${correctAnswer}

    Instructions :
    1. Sois cohérent avec la vraie réponse (même catégorie, même format).
    2. Ne propose pas de réponses absurdes ou offensantes.
    3. Réponds UNIQUEMENT en JSON avec cette structure : {"distractors": ["Choix 1", "Choix 2", "Choix 3"]}`;

    const text = await generateWithGemini(prompt) || '{"distractors":[]}';
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
        if (daysSinceLastCheckIn >= rhythmDays + 28) {
            l = 4; // Dépositaire secondaire notifié (v9.3.5)
        } else if (daysSinceLastCheckIn >= rhythmDays + 21) {
            l = 3; // Dépositaire primaire notifié
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

        if (l < 3) continue; // On ne notifie les dépositaires qu'à partir du niveau 3

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
        protocolId: ref.id, // LIEN AVEC LE PROTOCOLE (v7.6)
        scheduledFor: admin.firestore.Timestamp.fromMillis(Date.now() + thresholdMillis),
        status: "pending"
    });

    return { protocolId: ref.id, contestDeadline: contestDeadline.toMillis() };
});

// 12. Notification Contacts de Notification (Email sobre après 72h)
async function notifyDeathContactsInternal(creatorId: string, protocolId: string): Promise<void> {
    const db = admin.firestore();

    // 1. VÉRIFICATION STRICTE DU PROTOCOLE (Logique fermée par défaut v7.6)
    if (!protocolId) throw new Error("MISSING_PROTOCOL_ID");

    const protocolDoc = await db.collection("activationProtocols").doc(protocolId).get();
    if (!protocolDoc.exists) throw new Error("PROTOCOL_NOT_FOUND");

    const protocolData = protocolDoc.data();
    const status = protocolData?.status;
    const protocolCreatedAt = protocolData?.confirmedAt || protocolData?.createdAt;

    if (status === "contested") throw new Error("PROTOCOL_CONTESTED");
    if (status !== "pending_contest") throw new Error("UNEXPECTED_STATUS");

    // ═══ DÉFENSE EN PROFONDEUR v9.3.8 : Vérification de signe de vie ═══
    const creatorDoc = await db.collection("users").doc(creatorId).get();
    if (!creatorDoc.exists) throw new Error("CREATOR_NOT_FOUND");

    const creatorData = creatorDoc.data()!;
    const silenceConf = creatorData.silenceConfig;
    const lastCheckIn = silenceConf?.lastCheckInAt;
    const escalation = silenceConf?.escalationLevel || 0;

    // Si le créateur s'est manifesté APRÈS la création du protocole, ou si l'escalade est retombée
    if (escalation < 3 || (lastCheckIn && protocolCreatedAt && lastCheckIn.toMillis() > protocolCreatedAt.toMillis())) {
        console.warn(`[SECURITY] Annulation de l'activation pour ${creatorId} : Signe de vie détecté.`);

        // Marquer le protocole comme contesté pour arrêter les futures tentatives
        await db.collection("activationProtocols").doc(protocolId).update({
            status: "contested",
            cancellationReason: "creator_alive",
            cancelledAt: admin.firestore.FieldValue.serverTimestamp()
        });

        throw new Error("CREATOR_ALIVE");
    }

    const creatorName = creatorData.displayName || "Votre proche";

    // Action A : Ouverture de l'héritage
    await db.collection("users").doc(creatorId).update({
        protocolStatus: "activated"
    });

    // Action B : Clôture du protocole (Succès)
    await db.collection("activationProtocols").doc(protocolId).update({
        status: "completed",
        completedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    console.log(`[PROTOCOL] Héritage activé pour le créateur ${creatorId}`);

    const contactsSnap = await db.collection("users").doc(creatorId)
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
    const db = admin.firestore();
    const tasksSnap = await db.collection("tasks")
        .where("status", "==", "pending")
        .where("scheduledFor", "<=", now)
        .get();

    for (const taskDoc of tasksSnap.docs) {
        const task = taskDoc.data();
        try {
            if (task.type === "notifyDeathContacts") {
                await notifyDeathContactsInternal(task.creatorId, task.protocolId);
            }
            await taskDoc.ref.update({ status: "done" });
        } catch (e: any) {
            const errorMap: Record<string, string> = {
                "MISSING_PROTOCOL_ID": "missing_protocol_id",
                "PROTOCOL_NOT_FOUND": "protocol_not_found",
                "PROTOCOL_CONTESTED": "contested",
                "UNEXPECTED_STATUS": "unexpected_status",
                "CREATOR_ALIVE": "creator_alive",
                "CREATOR_NOT_FOUND": "creator_not_found"
            };

            const reason = errorMap[e.message];

            if (reason) {
                await taskDoc.ref.update({ status: "cancelled", reason: reason });
                console.warn(`[TASK] Tâche ${taskDoc.id} annulée : ${reason}`);
            } else {
                console.error("Erreur tâche:", e);
                await taskDoc.ref.update({ status: "failed", error: e.message });
            }
        }
    }
});

// 16. Résolution silence
export const resolveCreatorSilence = onCall(async (request) => {
    const { creatorId, depositaryId, note } = request.data;

    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Non authentifié");
    }

    const db = admin.firestore();

    try {
        await db.runTransaction(async (transaction) => {
            // 1. Lectures (v9.3.8 : Toutes les lectures avant les écritures)
            const depositaryRef = db.collection("users").doc(creatorId).collection("depositaries").doc(depositaryId);
            const creatorRef = db.collection("users").doc(creatorId);
            const protocolsQuery = db.collection("activationProtocols")
                .where("creatorId", "==", creatorId)
                .where("status", "==", "pending_contest");
            const tasksQuery = db.collection("tasks")
                .where("creatorId", "==", creatorId)
                .where("type", "==", "notifyDeathContacts")
                .where("status", "==", "pending");

            const [depositaryDoc, creatorDoc, protocolsSnap, tasksSnap] = await Promise.all([
                transaction.get(depositaryRef),
                transaction.get(creatorRef),
                transaction.get(protocolsQuery),
                transaction.get(tasksQuery)
            ]);

            // 2. Contrôles
            if (!depositaryDoc.exists || depositaryDoc.data()?.depositaryUid !== request.auth!.uid) {
                throw new HttpsError("permission-denied", "Accès refusé");
            }

            const currentStatus = creatorDoc.data()?.protocolStatus;

            // 3. Écritures
            const updates: any = {
                "silenceConfig.missedCycles": 0,
                "silenceConfig.lastCheckInAt": admin.firestore.Timestamp.now(),
                "silenceConfig.lastSilenceStatus": "present",
                "silenceConfig.escalationLevel": 0
            };

            if (currentStatus === "activated") {
                console.warn(`[SECURITY] Tentative de fermeture d'un héritage activé pour ${creatorId} par ${request.auth!.uid}`);
            } else {
                updates.protocolStatus = "dormant";
            }

            transaction.update(creatorRef, updates);

            // Log de résolution
            const notifRef = creatorRef.collection("silenceNotifications").doc();
            transaction.set(notifRef, {
                type: "resolved_by_depositary",
                depositaryId,
                note: note || null,
                timestamp: admin.firestore.Timestamp.now()
            });

            // Annulation des protocoles d'activation
            protocolsSnap.forEach(doc => {
                transaction.update(doc.ref, {
                    status: "contested",
                    resolvedAt: admin.firestore.FieldValue.serverTimestamp(),
                    resolvedBy: depositaryId
                });
            });

            // Annulation des tâches planifiées
            tasksSnap.forEach(doc => {
                transaction.update(doc.ref, {
                    status: "cancelled",
                    reason: "resolved_by_depositary",
                    cancelledAt: admin.firestore.FieldValue.serverTimestamp()
                });
            });
        });

        return { success: true };
    } catch (error: any) {
        if (error instanceof HttpsError) throw error;
        throw new HttpsError("internal", error.message);
    }
});

// 17. Notification d'octroi du droit de poser des questions
export const notifyQuestionRightGranted = onCall(async (request) => {
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Non authentifié");
    }

    const { recipientId } = request.data;
    const creatorUid = request.auth.uid;
    const db = admin.firestore();

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

/**
 * PHOEN-X v7.2 - Notification de nouveau témoignage
 */
export const notifyNewTestimony = onDocumentCreated(
    { document: "users/{creatorId}/witnesses/{witnessId}", region: "us-central1" },
    async (event) => {
        // Note: On peut aussi utiliser onDocumentUpdated si on veut détecter le passage à "submitted"
        // Mais comme le contenu est souvent ajouté à la création ou juste après, on surveille les deux.
        const snapshot = event.data;
        if (!snapshot) return;

        const creatorId = event.params.creatorId;
        const witnessData = snapshot.data();

        // On ne notifie que si le témoignage est scellé (status submitted ou content présent)
        if (witnessData.status === "submitted" || witnessData.content) {
            const userDoc = await admin.firestore().collection("users").doc(creatorId).get();
            const fcmToken = userDoc.data()?.fcmToken;

            if (fcmToken) {
                await admin.messaging().send({
                    token: fcmToken,
                    notification: {
                        title: "Un nouveau témoignage est arrivé",
                        body: `${witnessData.name} vient de sceller son message dans ton Cercle.`
                    }
                });
            }
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
    // ═══ SÉCURITÉ v9.3.6 : Authentification requise ═══
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Non authentifié");
    }

    const { shortCode } = request.data;
    const uid = request.auth.uid;
    const db = admin.firestore();

    try {
        const result = await db.runTransaction(async (transaction) => {
            const now = Date.now();
            const windowStart = now - (15 * 60 * 1000); // 15 mins

            // 1. Lectures (v9.3.8 : TOUTES les lectures avant les écritures)
            const limitRef = db.collection("rateLimits").doc(uid);
            const codeRef = db.collection("depositaryInviteCodes").doc(shortCode);

            const [limitDoc, codeDoc] = await Promise.all([
                transaction.get(limitRef),
                transaction.get(codeRef)
            ]);

            // 2. Évaluation Rate Limit (v9.3.9 : Plus de throw ici pour ne pas annuler l'écriture)
            if (limitDoc.exists) {
                const lData = limitDoc.data()!;
                const lastAttempt = lData.lastAttemptAt?.toMillis() || 0;
                if (lastAttempt > windowStart && lData.shortCodeAttempts >= 10) {
                    return { ok: false, reason: "rate_limited" };
                }
            }

            // 3. Incrémenter les tentatives (Même si le code n'existe pas ou est expiré)
            updateRateLimitTransactional(transaction, limitRef, limitDoc, windowStart);

            // 4. Évaluation Code
            if (!codeDoc.exists) {
                return { ok: false, reason: "not_found" };
            }

            const cData = codeDoc.data()!;
            if (cData.expiresAt.toMillis() < now || cData.used) {
                return { ok: false, reason: "expired_or_used" };
            }

            // 5. Marquage comme utilisé (Uniquement si OK)
            transaction.update(codeRef, {
                used: true,
                redeemedByUid: uid,
                redeemedAt: admin.firestore.FieldValue.serverTimestamp()
            });

            return {
                ok: true,
                creatorId: cData.creatorId,
                depositaryId: cData.depositaryId
            };
        });

        // ═══ TRADUCTION DES ERREURS (v9.3.9) ═══
        if (!result.ok) {
            const errorMap: Record<string, { code: any, msg: string }> = {
                "rate_limited": { code: "resource-exhausted", msg: "Trop de tentatives. Réessayez dans 15 minutes." },
                "not_found": { code: "not-found", msg: "Code invalide" },
                "expired_or_used": { code: "permission-denied", msg: "Code expiré ou déjà utilisé" }
            };
            const err = errorMap[result.reason!];
            throw new HttpsError(err.code, err.msg);
        }

        const { creatorId, depositaryId } = result;
        const creatorDoc = await db.collection("users").doc(creatorId).get();
        const dDoc = await db.collection("users").doc(creatorId).collection("depositaries").doc(depositaryId).get();

        return {
            creatorId: creatorId,
            depositaryId: depositaryId,
            token: dDoc.data()?.inviteToken,
            creatorName: creatorDoc.data()?.displayName || "Ton proche"
        };

    } catch (error: any) {
        if (error instanceof HttpsError) throw error;
        throw new HttpsError("internal", error.message);
    }
});

function updateRateLimitTransactional(transaction: admin.firestore.Transaction, limitRef: admin.firestore.DocumentReference, limitDoc: admin.firestore.DocumentSnapshot, windowStart: number) {
    if (limitDoc.exists) {
        const lData = limitDoc.data()!;
        const lastAttempt = lData.lastAttemptAt?.toMillis() || 0;
        if (lastAttempt <= windowStart) {
            transaction.set(limitRef, { shortCodeAttempts: 1, lastAttemptAt: admin.firestore.FieldValue.serverTimestamp() }, { merge: true });
        } else {
            transaction.set(limitRef, { shortCodeAttempts: admin.firestore.FieldValue.increment(1), lastAttemptAt: admin.firestore.FieldValue.serverTimestamp() }, { merge: true });
        }
    } else {
        transaction.set(limitRef, { shortCodeAttempts: 1, lastAttemptAt: admin.firestore.FieldValue.serverTimestamp() }, { merge: true });
    }
}

export const joinAsDepositary = onCall(async (request) => {
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Non authentifié");
    }
    const { creatorId, depositaryId, token } = request.data;
    const depositaryUid = request.auth.uid;
    const db = admin.firestore();

    try {
        await db.runTransaction(async (transaction) => {
            const ref = db.collection("users").doc(creatorId).collection("depositaries").doc(depositaryId);
            const doc = await transaction.get(ref);

            if (!doc.exists || doc.data()?.inviteToken !== token || doc.data()?.inviteTokenUsed) {
                throw new HttpsError("permission-denied", "Invalide");
            }

            const creatorDoc = await transaction.get(db.collection("users").doc(creatorId));
            const creatorName = creatorDoc.data()?.displayName || "Votre proche";

            // 1. Liaison sur le document du Créateur
            transaction.update(ref, {
                depositaryUid: depositaryUid,
                status: "active",
                inviteTokenUsed: true,
                inviteToken: admin.firestore.FieldValue.delete() // v9.3.9 : Suppression du token après usage
            });

            // 2. Lien inverse sur le document du Dépositaire (Système myRoles v9.3.5)
            const depositaryUserRef = db.collection("users").doc(depositaryUid);
            const roleKey = `${creatorId}_depositary`;
            const newRoleData = {
                creatorId: creatorId,
                creatorName: creatorName,
                role: "depositary",
                status: "active",
                label: "Gardien de confiance",
                joinedAt: admin.firestore.FieldValue.serverTimestamp(),
                sourceId: depositaryId
            };

            transaction.set(depositaryUserRef, {
                myRoles: { [roleKey]: newRoleData }
            }, { merge: true });
        });

        return { success: true };
    } catch (error: any) {
        if (error instanceof HttpsError) throw error;
        throw new HttpsError("internal", error.message);
    }
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
        requestPrompt: witnessData.requestPrompt || null,
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

/**
 * PHOEN-X v7.6 - Récupération sécurisée du statut du livre pour l'héritier
 */
export const getCreatorBookStatus = onCall(async (request) => {
    if (!request.auth) throw new HttpsError("unauthenticated", "Non authentifié");
    const { creatorId } = request.data;
    if (!creatorId) throw new HttpsError("invalid-argument", "ID Créateur manquant");

    const db = admin.firestore();
    const requesterUid = request.auth.uid;

    // 1. Vérification du rôle héritier
    const userDoc = await db.collection("users").doc(requesterUid).get();
    const myRoles = userDoc.data()?.myRoles || {};
    const hasRole = `${creatorId}_recipient` in myRoles;

    if (!hasRole) throw new HttpsError("permission-denied", "Vous n'êtes pas héritier de ce créateur.");

    // 2. Récupération des infos minimales du créateur
    const creatorDoc = await db.collection("users").doc(creatorId).get();
    if (!creatorDoc.exists) throw new HttpsError("not-found", "Créateur introuvable");

    const data = creatorDoc.data()!;

    // 3. Vérification des accès restreints au livre (v8.5.4)
    const bookDoc = await db.collection("users").doc(creatorId)
        .collection("book").doc("current_draft").get();

    if (bookDoc.exists) {
        const bookData = bookDoc.data();
        const recipientIds = bookData?.recipientIds || [];
        if (recipientIds.length > 0 && !recipientIds.includes(requesterUid)) {
             throw new HttpsError("permission-denied", "Ce livre ne vous est pas destiné.");
        }
    }

    return {
        displayName: data.displayName || "Votre proche",
        isBookOpen: data.protocolStatus === "activated"
    };
});

export const getCreatorProtocolStatus = onCall(async (request) => {
    if (!request.auth) throw new HttpsError("unauthenticated", "Non authentifié");
    const { creatorId } = request.data;
    if (!creatorId) throw new HttpsError("invalid-argument", "ID Créateur manquant");

    const db = admin.firestore();
    const requesterUid = request.auth.uid;

    // 1. Vérification du rôle héritier
    const userDoc = await db.collection("users").doc(requesterUid).get();
    const myRoles = userDoc.data()?.myRoles || {};
    const hasRole = `${creatorId}_recipient` in myRoles;

    if (!hasRole) throw new HttpsError("permission-denied", "Accès non autorisé.");

    const creatorDoc = await db.collection("users").doc(creatorId).get();
    if (!creatorDoc.exists) throw new HttpsError("not-found", "Créateur introuvable");

    return {
        protocolStatus: creatorDoc.data()?.protocolStatus || "pending",
        isActivated: creatorDoc.data()?.protocolStatus === "activated"
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
        const result = await db.runTransaction(async (transaction) => {
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
                label: inviteData.label || role, // Rétabli v9.3.6
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
                const sourceUpdates: any = {
                    status: "active",
                    linkedUid: auth.uid,
                    linkedAt: admin.firestore.FieldValue.serverTimestamp()
                };

                // v9.1 : Champ spécifique requis par les Security Rules pour les Dépositaires
                if (role === "depositary") {
                    sourceUpdates.depositaryUid = auth.uid;
                }

                transaction.update(sourceRef, sourceUpdates);
            }

            return { status: "success", role: role };
        });

        // ═══ PROPAGATION DE LIAISON (v9.3.2) ═══
        // On effectue la propagation APRÈS le succès de la transaction de liaison
        // On couvre aussi le cas "already_accepted" pour assurer l'idempotence de la propagation
        if (result.status === "success" || result.status === "already_accepted") {
            const inviteDocAfter = await inviteRef.get();
            const inviteData = inviteDocAfter.data();
            if (inviteData && inviteData.sourceId && inviteData.role === "recipient") {
                await propagateUidLiaison(db, inviteData.creatorId, inviteData.sourceId, auth.uid);
            }
        }

        return result;

    } catch (error: any) {
        if (error instanceof HttpsError) throw error;
        console.error("Erreur acceptUniversalInvitation:", error);
        throw new HttpsError("internal", error.message || "Erreur lors de l'acceptation de l'invitation");
    }
});

/**
 * PHOEN-X v9.3.6/v9.3.7/v9.3.9 - Propagation de l'UID après liaison
 * Remplace l'ancien DocID par le vrai UID dans toutes les archives du Créateur.
 */
async function propagateUidLiaison(db: FirebaseFirestore.Firestore, creatorId: string, oldDocId: string, newUid: string) {
    const userRef = db.collection("users").doc(creatorId);
    const operations: { ref: admin.firestore.DocumentReference, data: any }[] = [];

    // 1. Collections avec tableaux recipientIds (v9.3.6)
    const arrayCollections = ["entries", "quizzes", "standaloneMedia"];
    for (const col of arrayCollections) {
        const snap = await userRef.collection(col)
            .where("recipientIds", "array-contains", oldDocId).get();

        snap.forEach(doc => {
            const currentIds = doc.data().recipientIds as string[];
            const updatedIds = currentIds.map(id => id === oldDocId ? newUid : id);
            operations.push({ ref: doc.ref, data: { recipientIds: updatedIds } });
        });
    }

    // TODO v9.3.9 : amendments sont des sous-collections de entries.
    // Nécessite une requête collectionGroup quand elles seront synchronisées.

    // 2. Collections avec champ simple recipientId (v9.3.6)
    // TODO INACTIF : PactViewModel n'écrit pas encore recipientId.
    // Cette boucle ne remonte rien tant que ce n'est pas le cas.
    const simpleCollections = ["pendingQuestions", "portraits", "legacies", "pacts"];
    for (const col of simpleCollections) {
        const snap = await userRef.collection(col)
            .where("recipientId", "==", oldDocId).get();
        snap.forEach(doc => {
            operations.push({ ref: doc.ref, data: { recipientId: newUid } });
        });
    }

    // 3. Mise à jour du Livre (Draft)
    const bookRef = userRef.collection("book").doc("current_draft");
    const bookDoc = await bookRef.get();
    if (bookDoc.exists) {
        const bookData = bookDoc.data()!;
        const currentIds = (bookData.recipientIds || []) as string[];
        if (currentIds.includes(oldDocId)) {
            const updatedIds = currentIds.map(id => id === oldDocId ? newUid : id);
            operations.push({ ref: bookRef, data: { recipientIds: updatedIds } });
        }
    }

    // ═══ EXÉCUTION PAR LOTS (v9.3.6) ═══
    // On découpe en lots de 450 pour éviter la limite de 500 de db.batch()
    for (let i = 0; i < operations.length; i += 450) {
        const batch = db.batch();
        const chunk = operations.slice(i, i + 450);
        chunk.forEach(op => batch.update(op.ref, op.data));
        await batch.commit();
    }

    console.log(`[LIAISON] Propagation v9.3.9 terminée : ${operations.length} documents mis à jour pour ${oldDocId} -> ${newUid}`);
}

/**
 * PHOEN-X v9.4.4 - Révocation d'accès après retrait du Cercle
 * Retire l'UID du Destinataire des tableaux ou annule les liens simples.
 */
async function revokeUidAccess(db: FirebaseFirestore.Firestore, creatorId: string, uidToRemove: string) {
    const userRef = db.collection("users").doc(creatorId);
    const operations: { ref: admin.firestore.DocumentReference, data: any }[] = [];

    // 1. Collections avec tableaux recipientIds
    const arrayCollections = ["entries", "quizzes", "standaloneMedia"];
    for (const col of arrayCollections) {
        const snap = await userRef.collection(col)
            .where("recipientIds", "array-contains", uidToRemove).get();

        snap.forEach(doc => {
            const currentIds = doc.data().recipientIds as string[];
            const updatedIds = currentIds.filter(id => id !== uidToRemove);
            operations.push({ ref: doc.ref, data: { recipientIds: updatedIds } });
        });
    }

    // 2. Collections avec champ simple recipientId
    // Stratégie validée v9.4.4 : recipientId à null + archivage (Pas de suppression)
    const simpleCollections = ["pendingQuestions", "legacies"];
    for (const col of simpleCollections) {
        const snap = await userRef.collection(col)
            .where("recipientId", "==", uidToRemove).get();
        snap.forEach(doc => {
            operations.push({ ref: doc.ref, data: {
                recipientId: null,
                previousRecipientId: uidToRemove,
                revokedAt: admin.firestore.FieldValue.serverTimestamp()
            } });
        });
    }

    // 3. Mise à jour du Livre (Draft)
    const bookRef = userRef.collection("book").doc("current_draft");
    const bookDoc = await bookRef.get();
    if (bookDoc.exists) {
        const bookData = bookDoc.data()!;
        const currentIds = (bookData.recipientIds || []) as string[];
        if (currentIds.includes(uidToRemove)) {
            const updatedIds = currentIds.filter(id => id !== uidToRemove);
            operations.push({ ref: bookRef, data: { recipientIds: updatedIds } });
        }
    }

    // ═══ EXÉCUTION PAR LOTS ═══
    for (let i = 0; i < operations.length; i += 450) {
        const batch = db.batch();
        const chunk = operations.slice(i, i + 450);
        chunk.forEach(op => batch.update(op.ref, op.data));
        await batch.commit();
    }

    console.log(`[REVOCATION] v9.4.4 terminée : ${operations.length} documents révoqués pour ${uidToRemove} chez ${creatorId}`);
}

/**
 * Script de rattrapage (Backfill) - Réservé Admin
 */
export const backfillRecipientUids = onCall(async (request) => {
    if (request.auth?.uid !== "bLRNen7rArXinv5iQILx5OS3sxh2") {
        throw new HttpsError("permission-denied", "Accès administrateur requis");
    }

    const db = admin.firestore();
    const usersSnap = await db.collection("users").get();
    let totalFixed = 0;

    for (const userDoc of usersSnap.docs) {
        const recipientsSnap = await userDoc.ref.collection("recipients")
            .where("status", "==", "active").get();

        for (const recDoc of recipientsSnap.docs) {
            const data = recDoc.data();
            if (data.linkedUid && data.linkedUid !== recDoc.id) {
                await propagateUidLiaison(db, userDoc.id, recDoc.id, data.linkedUid);
                totalFixed++;
            }
        }
    }
    return { status: "success", recipientsProcessed: totalFixed };
});

/**
 * Script de rattrapage des Dépositaires (v9.4.4)
 */
export const backfillDepositaryUids = onCall(async (request) => {
    if (request.auth?.uid !== "bLRNen7rArXinv5iQILx5OS3sxh2") {
        throw new HttpsError("permission-denied", "Accès administrateur requis");
    }

    const db = admin.firestore();
    const usersSnap = await db.collection("users").get();
    let totalFixed = 0;

    for (const userDoc of usersSnap.docs) {
        const depositariesSnap = await userDoc.ref.collection("depositaries").get();
        const uids = [...new Set(
            depositariesSnap.docs
                .filter(doc => doc.data().status === "active" && !!doc.data().depositaryUid)
                .map(doc => doc.data().depositaryUid)
        )];

        if (uids.length > 0) {
            await userDoc.ref.update({ depositaryUids: uids });
            totalFixed++;
        }
    }
    return { status: "success", usersProcessed: totalFixed };
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
        isCreator: data?.isCreator === true, // v9.3.5 : Sécurisation du rôle Créateur (pas de true par défaut)
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

        // v9.4.4 : Retrait du tableau depositaryUids si c'est un Dépositaire
        if (role === "depositary") {
            await db.collection("users").doc(creatorId).update({
                depositaryUids: admin.firestore.FieldValue.arrayRemove(linkedUid)
            });
        }

        // v9.4.4 : Révocation des accès si c'est un Destinataire
        if (role === "recipient") {
            await revokeUidAccess(db, creatorId, linkedUid);
        }
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

export const onWitnessDeleted = onDocumentDeleted(
    { document: "users/{creatorId}/witnesses/{witnessId}", region: "us-central1" },
    async (event) => {
    const { creatorId, witnessId } = event.params;
    const data = event.data?.data();
    if (!data) return;
    await cleanupMemberRoles(creatorId, witnessId, "witness", data.linkedUid);
});

export const onRecipientDeleted = onDocumentDeleted(
    { document: "users/{creatorId}/recipients/{recipientId}", region: "us-central1" },
    async (event) => {
    const { creatorId, recipientId } = event.params;
    const data = event.data?.data();
    if (!data) return;
    await cleanupMemberRoles(creatorId, recipientId, "recipient", data.linkedUid);
});

export const onDepositaryDeleted = onDocumentDeleted(
    { document: "users/{creatorId}/depositaries/{depositaryId}", region: "us-central1" },
    async (event) => {
    const { creatorId, depositaryId } = event.params;
    const data = event.data?.data();
    if (!data) return;
    const uid = data.linkedUid || data.depositaryUid; // Support historique et v7.2
    await cleanupMemberRoles(creatorId, depositaryId, "depositary", uid);
});

/**
 * PHOEN-X v7.2 - Nettoyage intégral après suppression Auth
 * Déclenchée manuellement via la console Auth ou API Admin.
 */
export const onUserDeletedCleanup = functionsV1.auth.user().onDelete(async (user) => {
    const { uid, email } = user;
    const db = admin.firestore();

    console.log(`[AUTH DELETE] Début du nettoyage pour ${uid} (${email})`);

    try {
        // 1. SUPPRESSION DU PROFIL (Priorité absolue)
        // On le fait en premier pour que même si le reste plante, le profil disparaisse.
        const userRef = db.collection("users").doc(uid);
        await db.recursiveDelete(userRef);
        console.log(`[AUTH DELETE] Profil users/${uid} supprimé.`);

        const batch = db.batch();
        let deletedCount = 0;

        // 2. RECHERCHE DES LIENS (Try/Catch interne car nécessite des index)
        try {
            const witnessSnap = await db.collectionGroup("witnesses").where("linkedUid", "==", uid).get();
            const recipientSnap = await db.collectionGroup("recipients").where("linkedUid", "==", uid).get();
            const depositarySnap = await db.collectionGroup("depositaries").where("linkedUid", "==", uid).get();

            [...witnessSnap.docs, ...recipientSnap.docs, ...depositarySnap.docs].forEach(doc => {
                batch.delete(doc.ref);
                deletedCount++;
            });
        } catch (e) {
            console.warn("[AUTH DELETE] Les recherches groupées ont échoué (index manquants ?)", e);
        }

        // 3. RECHERCHE PAR EMAIL (Try/Catch car nécessite aussi des index)
        if (email) {
            try {
                const wEmail = await db.collectionGroup("witnesses").where("email", "==", email.toLowerCase()).get();
                const rEmail = await db.collectionGroup("recipients").where("email", "==", email.toLowerCase()).get();
                wEmail.docs.forEach(doc => { batch.delete(doc.ref); deletedCount++; });
                rEmail.docs.forEach(doc => { batch.delete(doc.ref); deletedCount++; });
            } catch (e) {
                console.warn("[AUTH DELETE] La recherche par email a échoué.", e);
            }
        }

        // 4. SUPPRESSION DES INVITATIONS (ÉMISES & ACCEPTÉES) & TÂCHES
        const invitesByCreator = await db.collection("invitations").where("creatorId", "==", uid).get();
        const invitesByAccepted = await db.collection("invitations").where("acceptedByUid", "==", uid).get(); // Nouveau (v8.4.4)
        const tasksSnap = await db.collection("tasks").where("creatorId", "==", uid).get();

        invitesByCreator.docs.forEach(doc => { batch.delete(doc.ref); deletedCount++; });
        invitesByAccepted.docs.forEach(doc => { batch.delete(doc.ref); deletedCount++; });
        tasksSnap.docs.forEach(doc => { batch.delete(doc.ref); deletedCount++; });

        // 5. NETTOYAGE DES LOGS MAIL (Nouveau v8.4.4)
        if (email) {
            const mailSnap = await db.collection("mail").where("to", "==", email).get();
            mailSnap.docs.forEach(doc => { batch.delete(doc.ref); deletedCount++; });
            console.log(`[AUTH DELETE] ${mailSnap.size} logs mail identifiés pour suppression.`);
        }

        await batch.commit();
        console.log(`[AUTH DELETE] ${deletedCount} documents liés nettoyés. Fin de procédure.`);

    } catch (error) {
        console.error(`[AUTH ERROR] Échec du nettoyage critique pour ${uid}:`, error);
    }
});

/**
 * PHOEN-X v8.4.8 - Conversion sécurisée en Créateur
 * Seule autorisée à modifier isCreator et la config silence initiale.
 */
export const becomeCreator = onCall(async (request) => {
    const uid = request.auth?.uid;
    if (!uid) throw new HttpsError("unauthenticated", "Utilisateur non connecté");

    const { rhythmDays } = request.data;
    if (!rhythmDays || typeof rhythmDays !== "number") {
        throw new HttpsError("invalid-argument", "Rythme de silence invalide");
    }

    try {
        const userRef = admin.firestore().collection("users").doc(uid);
        await userRef.update({
            isCreator: true,
            "silenceConfig.rhythmDays": rhythmDays,
            "silenceConfig.lastCheckInAt": admin.firestore.FieldValue.serverTimestamp(),
            "silenceConfig.missedCycles": 0,
            "silenceConfig.escalationLevel": 0,
            "silenceConfig.lastSilenceStatus": "present",
            convertedAt: admin.firestore.FieldValue.serverTimestamp()
        });

        console.log(`[CONVERSION] L'utilisateur ${uid} est maintenant Créateur.`);
        return { success: true };
    } catch (error: any) {
        console.error(`[CONVERSION ERROR] Échec pour ${uid}:`, error);
        throw new HttpsError("internal", "Impossible de valider le statut Créateur");
    }
});

// 20. Modification d'un chapitre par l'IA (v8.6.3)
export const modifyBookChapter = onCall({
    secrets: ["GEMINI_API_KEY"],
    region: "us-central1",
    invoker: "public"
}, async (request) => {
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Non authentifié");
    }

    const { currentContent, instruction } = request.data;
    if (!currentContent || !instruction) throw new HttpsError("invalid-argument", "Données manquantes");

    const prompt = `${AI_RULES}
    Tu es le biographe de l'utilisateur. Tu dois modifier le chapitre suivant selon ses instructions.
    RÈGLE CRITIQUE : Conserve impérativement les balises de type [PHOTO:uuid] ou [AUDIO:uuid] à leur place ou déplace-les logiquement, mais ne les supprime JAMAIS.

    Chapitre actuel : ${currentContent}
    Instruction de l'auteur : ${instruction}

    Réponds UNIQUEMENT avec le nouveau texte du chapitre.`;

    const newContent = await generateWithGemini(prompt);
    return { newContent: newContent || currentContent };
});

// 21. Génération de l'introduction globale (v8.7.0)
export const generateGlobalIntro = onCall({
    secrets: ["GEMINI_API_KEY"],
    region: "us-central1",
    invoker: "public"
}, async (request) => {
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Non authentifié");
    }

    const { chapterTitles } = request.data;
    if (!chapterTitles || !Array.isArray(chapterTitles)) throw new HttpsError("invalid-argument", "Liste de chapitres manquante");

    const prompt = `${AI_RULES}
    Tu es le biographe de l'utilisateur. Tu as rédigé un livre avec les chapitres suivants : ${chapterTitles.join(", ")}.
    Rédige une introduction globale chaleureuse et poétique pour ce livre de vie.
    L'introduction doit donner envie de lire la suite et souligner l'importance de la transmission.
    Utilise la première personne du singulier ("Je").

    Réponds UNIQUEMENT avec le texte de l'introduction.`;

    const content = await generateWithGemini(prompt);
    return { content: content || "" };
});

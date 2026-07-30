import { onCall, HttpsError } from "firebase-functions/v2/https";
import { onSchedule } from "firebase-functions/v2/scheduler";
import axios from "axios";
import * as admin from "firebase-admin";
import { db } from "./admin";

/**
 * PHOEN-X Protocol Module - Silence surveillance and activation
 */

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

// 10. Surveillance du silence
export const checkCreatorSilence = onSchedule({
    schedule: "every 24 hours",
    secrets: ["SMSPARTNER_API_KEY"]
}, async (event) => {
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
    const depositaryDoc = await db
        .collection("users").doc(creatorId)
        .collection("depositaries").doc(depositaryId)
        .get();

    if (!depositaryDoc.exists ||
        depositaryDoc.data()?.depositaryUid !== request.auth.uid) {
        throw new HttpsError("permission-denied", "Accès refusé");
    }

    // Récupérer le délai de contestation personnalisé du Créateur
    const creatorDoc = await db
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

    const ref = await db
        .collection("activationProtocols").add({
            creatorId, depositaryId,
            depositaryUid: request.auth.uid, // AJOUT v9.4.4
            status: "pending_contest",
            confirmedAt: now,
            contestDeadline,
            contactAttemptNote,
            contactAttemptDetails,
            depositaryNote: depositaryNote || null
        });

    await db.collection("tasks").add({
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
        return db.collection("mail").add({
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

/**
 * PHOEN-X v9.4.9 - Vérification automatique du décès via deces.matchid.io (INSEE)
 */
export const checkDeathRecord = onCall(async (request) => {
    if (!request.auth) throw new HttpsError("unauthenticated", "Non authentifié");

    const { creatorId } = request.data;

    // 1. VÉRIFICATION D'AUTORISATION DÉPOSITAIRE (D'ABORD v9.4.8)
    const creatorDoc = await db.collection("users").doc(creatorId).get();
    if (!creatorDoc.exists) throw new HttpsError("not-found", "Créateur introuvable");

    const creatorData = creatorDoc.data()!;
    if (!(creatorData.depositaryUids || []).includes(request.auth.uid)) {
        throw new HttpsError("permission-denied", "Accès refusé.");
    }

    // 2. VÉRIFICATION DE L'EXISTENCE D'UN PROTOCOLE ACTIF
    const protocolsSnap = await db.collection("activationProtocols")
        .where("creatorId", "==", creatorId)
        .where("status", "==", "pending_contest")
        .orderBy("confirmedAt", "desc").limit(1).get();

    if (protocolsSnap.empty) return { ok: false, reason: "no_active_protocol" };
    const protocolRef = protocolsSnap.docs[0].ref;

    // 3. RÉCUPÉRATION IDENTITÉ
    const { lastName: nom, firstName: prenom, dateOfBirth: dob } = creatorData;
    if (!nom || !prenom || !dob) return { ok: false, reason: "missing_identity_fields" };

    const dobStr = dob.toDate().toISOString().split('T')[0];

    // 4. INTERROGATION API PUBLIQUE (Sans clé v9.4.9)
    try {
        const response = await axios.get("https://deces.matchid.io/deces/api/v1/search", {
            params: {
                firstName: prenom,
                lastName: nom,
                birthDate: dobStr
            }
        });

        const hits = response.data.hits?.hits || [];
        const found = hits.length > 0;

        await protocolRef.update({
            deathRecordCheck: {
                checkedAt: admin.firestore.FieldValue.serverTimestamp(),
                status: found ? "found" : "not_found",
                apiReference: hits[0]?._id || null
            }
        });

        return { ok: true, found };
    } catch (error: any) {
        console.error(`[DEATH_CHECK] API Error: ${error.message}`);
        return { ok: false, reason: "api_failure" };
    }
});

import { onCall, HttpsError } from "firebase-functions/v2/https";
import { GoogleGenAI } from "@google/genai";
import * as admin from "firebase-admin";
import { db } from "./admin";
import {
    loadBillingConfig,
    isEnforced,
    resolveEntitlement,
    activePromotions,
    computePageCharge,
    reserveCredits,
    settleCredits,
    releaseCredits,
    logAiUsage
} from "./billing";

/**
 * PHOEN-X Intelligence Layer - AI Module
 */

const API_KEY = process.env.GEMINI_API_KEY || "";
const ai = new GoogleGenAI({ apiKey: API_KEY });
const AI_MODEL = "gemini-3.5-flash";

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

// Helper pour simplifier les appels avec le nouveau SDK pérenne
async function generateWithGemini(prompt: string, caller: string = "unknown"): Promise<string> {
    try {
        const result = await ai.models.generateContent({
            model: AI_MODEL,
            contents: [prompt]
        });

        if (result.usageMetadata) {
            const { promptTokenCount, candidatesTokenCount, totalTokenCount } = result.usageMetadata;
            console.log(`[GEMINI USAGE - ${caller}] Prompt: ${promptTokenCount}, Candidates: ${candidatesTokenCount}, Total: ${totalTokenCount}`);
        }

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

    const text = await generateWithGemini(prompt, "analyzeEntry") || "{}";

    const analysis = JSON.parse(text.replace(/```json|```/g, "").trim());

    // Filtrage de sécurité (Allowlist)
    if (analysis && Array.isArray(analysis.suggestedCompartments)) {
        analysis.suggestedCompartments = analysis.suggestedCompartments.filter(
            (comp: string) => VALID_COMPARTMENTS.includes(comp)
        );
    } else if (analysis) {
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
    return await generateWithGemini(prompt, "generateBiographerQuestion") || "Quel souvenir te fait sourire ?";
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
    return await generateWithGemini(prompt, "generateEssencePortrait") || "";
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
    const text = await generateWithGemini(prompt, "detectThoughtEvolution") || '{"transitions":[]}';
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
    if (!summariesAtThatAge?.length) return "";
    const prompt = `${AI_RULES} Suggestions pour lettre à soi-même à ${targetAge} ans. Résumés: ${summariesAtThatAge.join(" | ")}`;
    return await generateWithGemini(prompt, "generateYoungSelfSuggestions") || "";
});

// 6. Aide à la réconciliation
export const generateReconciliationHelp = onCall({
    secrets: ["GEMINI_API_KEY"]
}, async (request) => {
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Non authentifié");
    }

    const { recipient, intent } = request.data;
    if (!recipient || !intent) {
        throw new HttpsError("invalid-argument", "Champs requis manquants");
    }

    const prompt = `${AI_RULES}
    Tu es un assistant de médiation et de réconciliation familiale bienveillant pour PHOEN-X.
    L'utilisateur souhaite adresser un message apaisant ou une démarche de réconciliation à "${recipient}".
    Intention de l'utilisateur : "${intent}".

    Propose une ébauche de message chaleureux, sincère et nuancé, rédigé à la première personne ("Je"), favorisant l'écoute et l'apaisement sans jugement.
    Réponds UNIQUEMENT avec le texte du message proposé.`;

    const text = await generateWithGemini(prompt, "generateReconciliationHelp");
    return text || "";
});

// 8. Génération du livre (v7.6 Multimédia)
export const generateBookChapters = onCall({
    secrets: ["GEMINI_API_KEY"]
}, async (request) => {
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Non authentifié");
    }

    const uid = request.auth.uid;
    const { scenes, ageMin, ageMax, soulTone, plan, authorProfile, generationId } = request.data;
    if (!scenes || scenes.length === 0) throw new HttpsError("invalid-argument", "Pas de souvenirs à traiter");

    // Idempotence : si la génération a déjà été effectuée avec succès pour cet ID, renvoyer le résultat
    if (generationId && typeof generationId === "string") {
        const genDoc = await db.collection("users").doc(uid).collection("bookGenerations").doc(generationId).get();
        if (genDoc.exists && genDoc.data()?.status === "done") {
            const savedData = genDoc.data()!;
            return savedData.result;
        }
    }

    const cfg = await loadBillingConfig();
    const enforced = isEnforced(uid, cfg);
    const userDoc = await db.collection("users").doc(uid).get();
    const userData = userDoc.data() || {};
    const entitlement = await resolveEntitlement(uid, userData, cfg);
    const promos = activePromotions(uid, entitlement.tier, userData.createdAt, Date.now(), cfg);

    const costMode = cfg.billing?.aiCosts?.generateBookChapters?.mode || "per_page";

    // Estimation et réservation initiale de crédits
    const estimatedPages = Math.ceil(scenes.length * (cfg.billing.estimatePagesPerScene || 0.6));
    const initialCharge = computePageCharge(estimatedPages, entitlement.tier, promos, cfg);

    let reservationId: string | null = null;
    let initialReserveCredits = 0;

    if (enforced && costMode !== "free") {
        initialReserveCredits = initialCharge.credits;
        if (initialReserveCredits > 0) {
            const reserveResult = await reserveCredits(uid, initialReserveCredits);
            if (!reserveResult.success) {
                throw new HttpsError("resource-exhausted", "insufficient-credits", {
                    needed: initialReserveCredits,
                    available: reserveResult.available
                });
            }
            reservationId = reserveResult.reservationId;
        }
    }

    try {
        const toneInstruction = soulTone ? `Le ton de ce récit doit être : ${soulTone}.` : "Le ton doit être celui d'un biographe bienveillant, respectueux et narratif.";

        let planInstruction = "";
        if (plan && plan.length > 0) {
            planInstruction = `Respecte IMPÉRATIVEMENT ce plan de chapitres validé : ${JSON.stringify(plan)}.
            Chaque chapitre doit traiter uniquement les scenes dont les IDs sont listés pour lui.`;
        }

        let authorProfileInstruction = "";
        if (authorProfile && typeof authorProfile === "object" && Object.keys(authorProfile).length > 0) {
            authorProfileInstruction = `Profil de l'auteur (Créateur du livre) : ${JSON.stringify(authorProfile)}. Utilise ces éléments personnels (métier, centres d'intérêt, contexte familial, éléments biographiques) pour nourrir la personnalité et le ton du narrateur. ATTENTION : ce profil décrit l'état ACTUEL de l'auteur au moment où il rédige son livre (métier actuel, situation familiale actuelle, etc.), pas des faits valables à tout âge. N'insère JAMAIS un élément de ce profil dans un chapitre se déroulant clairement à un autre âge de sa vie que celui d'aujourd'hui — sauf s'il s'agit d'un trait de fond plausible depuis longtemps (une passion ancienne, un trait de caractère), à utiliser alors avec prudence et sans le dater précisément.`;
        }

        const prompt = `${AI_RULES}
        Tu es le biographe attitré de l'utilisateur. Tu dois rédiger un Livre de Vie structuré en chapitres.
        ${toneInstruction}
        ${planInstruction}
        ${authorProfileInstruction}
        Données source (Scènes) : ${JSON.stringify(scenes)}

        Instructions de rédaction (v9.5 — Lot 3 régénération partielle) :
        1. Rédige un récit fluide, à la première personne du singulier ("Je"), en couvrant la période de ${ageMin} à ${ageMax} ans.
        2. RÈGLE DE NON-PARAPHRASE (CRITIQUE) : Ne reproduis jamais la structure de phrase ou le choix de mots exact des 'summary' fournis. Tu es un biographe littéraire, pas un traducteur. Réécris entièrement chaque idée avec ton propre style narratif, fluide et élégant. Une simple reformulation par synonymes est interdite.
        3. RÈGLE ANTI-INVENTION (ABSOLUE) : N'invente JAMAIS un fait, un nom, un lieu, une date, un score ou un détail circumstantial qui ne figure pas explicitement dans les données fournies ('summary', 'userComment', 'amendments', descriptions des médias). Interdiction stricte de compléter à partir de tes connaissances générales, même sur un sujet réel et reconnaissable (un match de sport, un événement d'actualité, un lieu célèbre) : si le Créateur ne t'a pas donné le score, l'heure, les buteurs ou les circonstances exactes, tu ne les inventes pas — même s'ils sont exacts dans la réalité, ce ne sont pas SES souvenirs à lui tant qu'il ne te les a pas donnés.
        3bis. RÈGLE DE PROPORTION (CRITIQUE) : Si la matière source d'une scène est maigre (résumé très court, pas de 'userComment', aucun détail concret dans les compléments), reste bref — une phrase suffit — plutôt que de broder un paragraphe entier. Une phrase courte et honnête vaut toujours mieux qu'un embellissement inventé.
        3ter. RÈGLE DE CLOISONNEMENT DES SOURCES (CRITIQUE) : Le récit principal ('summary', avec sa date d'événement éventuelle 'summaryEventDate') et chaque ajout attaché à un souvenir ('stories') ne relatent pas nécessairement le même moment. Un ajout peut décrire un épisode différent, parfois à des années d'écart. Ne présume jamais qu'il s'agit d'une seule et même scène : ne les fusionne en un moment continu que si le texte lui-même l'indique clairement. Dans le doute, traite-les comme des évocations distinctes, ou n'en retiens qu'une seule. L'absence de date ne signifie pas qu'il s'agit du même moment.
        3quater. RÈGLE DE HIÉRARCHIE DES SOURCES (CRITIQUE) : Le récit principal ('summary') est la source de référence d'un souvenir : ses détails concrets doivent toujours être repris et développés en priorité. Un ajout ('stories') vient enrichir ou prolonger ce récit, jamais le remplacer ni l'éclipser. N'accorde jamais plus de place et de détail à un ajout qu'au récit principal du même souvenir.
        4. Utilise les 'userComment' (commentaires personnels) pour enrichir la description des médias et des souvenirs : ils apportent le contexte émotionnel que le résumé n'a pas forcément capté.
        5. Intègre les 'amendments' pour montrer comment la pensée de l'auteur a évolué sur un même sujet au fil des années.
        6. Utilise les données de l'Arbre Généalogique ('characters' avec parentIds et biography) pour assurer la cohérence des liens familiaux et donner de l'épaisseur aux proches cités.
        7. Pour chaque photo fournie (avec id et description), insère la balise [PHOTO:id_exact] à l'endroit le plus opportun dans ton texte.
        8. Pour chaque enregistrement vocal (id et description), intègre son essence émotionnelle. Tu peux aussi insérer une balise [AUDIO:id_exact].
        8bis. RÈGLE DES DEVINETTES (v12.3) : Si une scène est de type 'GUESS_QUESTION', tu dois IMPÉRATIVEMENT envelopper le récit qui s'y rapporte avec les balises [GUESS:id_exact] et [/GUESS]. Ces balises permettent de masquer ce contenu aux lecteurs qui n'auraient pas encore résolu la devinette.
        9. Réponds UNIQUEMENT en JSON avec cette structure : {"chapters": [{"title": "Nom du chapitre", "content": "Texte avec balises [PHOTO:id] et [GUESS:id] incluses", "orderIndex": 0, "sceneIds": ["id_exact_1", "id_exact_2"]}]}. Le champ 'sceneIds' doit lister EXACTEMENT les IDs des scènes (fournies dans les données source) réellement utilisées pour rédiger CE chapitre précis — jamais d'ID inventé, jamais la liste complète par défaut.`;

        const rawText = await generateWithGemini(prompt, "generateBookChapters");
        if (!rawText || !rawText.trim()) {
            throw new HttpsError("internal", "L'IA a renvoyé une réponse vide lors de la génération du livre.");
        }
        const cleanText = rawText.replace(/```json|```/g, "").trim();
        let parsed: any;
        try {
            parsed = JSON.parse(cleanText);
        } catch (e: any) {
            throw new HttpsError("internal", "Format JSON invalide renvoyé par l'IA pour le livre.");
        }
        if (!parsed || !Array.isArray(parsed.chapters) || parsed.chapters.length === 0) {
            throw new HttpsError("internal", "Aucun chapitre valide généré par l'IA.");
        }

        // Calcul des mots réels produits (nettoyage des balises de médias et devinettes)
        let totalWords = 0;
        for (const chap of parsed.chapters) {
            if (chap.content && typeof chap.content === "string") {
                const cleanContent = chap.content.replace(/\[(PHOTO|AUDIO|GUESS):[^\]]+\]|\[\/GUESS\]/g, "");
                const words = cleanContent.trim().split(/\s+/).filter((w: string) => w.length > 0);
                totalWords += words.length;
            }
        }

        const wordsPerPage = cfg.billing.wordsPerPage || 250;
        const actualPages = Math.max(1, Math.ceil(totalWords / wordsPerPage));
        const actualCharge = computePageCharge(actualPages, entitlement.tier, promos, cfg);

        if (enforced && costMode !== "free") {
            await settleCredits(uid, reservationId, actualCharge.credits, "generateBookChapters");
        }

        await logAiUsage({
            uid,
            fn: "generateBookChapters",
            words: totalWords,
            pages: actualPages,
            scenes: scenes.length,
            credits: actualCharge.credits,
            enforced
        });

        const billingInfo = {
            pages: actualPages,
            freePages: actualCharge.freePages,
            credits: actualCharge.credits,
            enforced
        };

        const resultResponse = {
            ...parsed,
            billing: billingInfo
        };

        if (generationId && typeof generationId === "string") {
            try {
                await db.collection("users").doc(uid).collection("bookGenerations").doc(generationId).set({
                    status: "done",
                    result: resultResponse,
                    pagesCharged: actualCharge.credits,
                    createdAt: admin.firestore.FieldValue.serverTimestamp()
                });
            } catch (e) {
                console.error("[generateBookChapters] Erreur enregistrement bookGeneration:", e);
            }
        }

        return resultResponse;
    } catch (error: any) {
        if (reservationId) {
            await releaseCredits(uid, reservationId).catch(e => console.error("[generateBookChapters] Erreur releaseCredits:", e));
        }
        if (error instanceof HttpsError) throw error;
        throw new HttpsError("internal", error.message || "Erreur de génération du livre");
    }
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
    Utilise les 'userComment' et les 'amendments' pour mieux saisir l'importance relative de chaque scène.
    Scènes : ${JSON.stringify(scenes)}

    Instructions :
    1. Propose entre 3 et 8 chapitres.
    2. RÈGLE DE CLOISONNEMENT DES SOURCES (CRITIQUE) : Le récit principal ('summary', avec sa date d'événement éventuelle 'summaryEventDate') et chaque ajout attaché à un souvenir ('stories') ne relatent pas nécessairement le même moment. Un ajout peut décrire un épisode différent, parfois à des années d'écart. Ne présume jamais qu'il s'agit d'une seule et même scène : ne les fusionne en un moment continu que si le texte lui-même l'indique clairement. Dans le doute, traite-les comme des évocations distinctes, ou n'en retiens qu'une seule. L'absence de date ne signifie pas qu'il s'agit du même moment.
    3. Pour chaque chapitre, donne un titre poétique et la liste des IDs des scènes incluses.
    4. Réponds UNIQUEMENT en JSON avec cette structure : {"plan": [{"title": "Titre du chapitre", "sceneIds": ["id1", "id2"], "description": "Brève intention narrative"}]}`;

    const rawText = await generateWithGemini(prompt, "generateBookPlan");
    if (!rawText || !rawText.trim()) {
        throw new HttpsError("internal", "L'IA a renvoyé une réponse vide lors de la génération du plan.");
    }
    const cleanText = rawText.replace(/```json|```/g, "").trim();
    let parsed: any;
    try {
        parsed = JSON.parse(cleanText);
    } catch (e: any) {
        throw new HttpsError("internal", "Format JSON invalide renvoyé par l'IA pour le plan.");
    }
    if (!parsed || !Array.isArray(parsed.plan) || parsed.plan.length === 0) {
        throw new HttpsError("internal", "Aucun plan valide généré par l'IA.");
    }
    return parsed;
});

// 20. Modification d'un chapitre par l'IA (v8.6.3 / v14)
export const modifyBookChapter = onCall({
    secrets: ["GEMINI_API_KEY"]
}, async (request) => {
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Non authentifié");
    }

    const uid = request.auth.uid;
    const { currentContent, instruction, generationId } = request.data;
    if (!currentContent || !instruction) throw new HttpsError("invalid-argument", "Données manquantes");

    if (generationId && typeof generationId === "string") {
        const genDoc = await db.collection("users").doc(uid).collection("bookGenerations").doc(generationId).get();
        if (genDoc.exists && genDoc.data()?.status === "done") {
            return genDoc.data()!.result;
        }
    }

    const cfg = await loadBillingConfig();
    const enforced = isEnforced(uid, cfg);

    const costConfig = cfg.billing?.aiCosts?.modifyBookChapter || { mode: "free", flat: 0 };
    const costMode = costConfig.mode || "free";
    const flatCost = costConfig.flat || 0;

    const userDoc = await db.collection("users").doc(uid).get();
    const userData = userDoc.data() || {};
    const entitlement = await resolveEntitlement(uid, userData, cfg);
    const promos = activePromotions(uid, entitlement.tier, userData.createdAt, Date.now(), cfg);

    let reservationId: string | null = null;
    let initialReserveCredits = 0;

    if (enforced && costMode !== "free") {
        if (costMode === "flat" && flatCost > 0) {
            initialReserveCredits = flatCost;
        } else if (costMode === "per_page") {
            const initialCharge = computePageCharge(1, entitlement.tier, promos, cfg);
            initialReserveCredits = initialCharge.credits;
        }

        if (initialReserveCredits > 0) {
            const reserveResult = await reserveCredits(uid, initialReserveCredits);
            if (!reserveResult.success) {
                throw new HttpsError("resource-exhausted", "insufficient-credits", {
                    needed: initialReserveCredits,
                    available: reserveResult.available
                });
            }
            reservationId = reserveResult.reservationId;
        }
    }

    try {
        const prompt = `${AI_RULES}
        Tu es le biographe de l'utilisateur. Tu dois modifier le chapitre suivant selon ses instructions.
        RÈGLE CRITIQUE : Conserve impérativement les balises de type [PHOTO:uuid] ou [AUDIO:uuid] à leur place ou déplace-les logiquement, mais ne les supprime JAMAIS.

        Chapitre actuel : ${currentContent}
        Instruction de l'auteur : ${instruction}

        Réponds UNIQUEMENT avec le nouveau texte du chapitre.`;

        const newContent = await generateWithGemini(prompt, "modifyBookChapter");
        const resultText = newContent || currentContent;

        const cleanText = resultText.replace(/\[(PHOTO|AUDIO|GUESS):[^\]]+\]|\[\/GUESS\]/g, "");
        const words = cleanText.trim().split(/\s+/).filter((w: string) => w.length > 0).length;

        let actualCreditsNeeded = 0;
        let pagesCount = 1;
        let freePagesCount = 0;

        if (costMode === "flat") {
            actualCreditsNeeded = flatCost;
            pagesCount = 1;
        } else if (costMode === "per_page") {
            const wordsPerPage = cfg.billing.wordsPerPage || 250;
            pagesCount = Math.max(1, Math.ceil(words / wordsPerPage));
            const charge = computePageCharge(pagesCount, entitlement.tier, promos, cfg);
            actualCreditsNeeded = charge.credits;
            freePagesCount = charge.freePages;
        }

        if (enforced && costMode !== "free") {
            await settleCredits(uid, reservationId, actualCreditsNeeded, "modifyBookChapter");
        }

        await logAiUsage({
            uid,
            fn: "modifyBookChapter",
            words,
            pages: pagesCount,
            scenes: 1,
            credits: actualCreditsNeeded,
            enforced
        });

        const billingInfo = {
            pages: pagesCount,
            freePages: freePagesCount,
            credits: actualCreditsNeeded,
            enforced
        };

        const resultResponse = {
            newContent: resultText,
            billing: billingInfo
        };

        if (generationId && typeof generationId === "string") {
            try {
                await db.collection("users").doc(uid).collection("bookGenerations").doc(generationId).set({
                    status: "done",
                    result: resultResponse,
                    pagesCharged: actualCreditsNeeded,
                    createdAt: admin.firestore.FieldValue.serverTimestamp()
                });
            } catch (e) {
                console.error("[modifyBookChapter] Erreur enregistrement bookGeneration:", e);
            }
        }

        return resultResponse;
    } catch (error: any) {
        if (reservationId) {
            await releaseCredits(uid, reservationId).catch(e => console.error("[modifyBookChapter] Erreur releaseCredits:", e));
        }
        if (error instanceof HttpsError) throw error;
        throw new HttpsError("internal", error.message || "Erreur de modification du chapitre");
    }
});

// 21. Génération de l'introduction globale (v8.7.0)
export const generateGlobalIntro = onCall({
    secrets: ["GEMINI_API_KEY"]
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

    const content = await generateWithGemini(prompt, "generateGlobalIntro");
    return { content: content || "" };
});

// 22. Assistant IA (v9.4.25)
export const askAssistant = onCall({
    secrets: ["GEMINI_API_KEY"]
}, async (request) => {
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Non authentifié");
    }
    const { question, userName, language = "fr" } = request.data;
    if (!question) throw new HttpsError("invalid-argument", "Question manquante");

    // 1. Récupération STRICTE de la base de connaissance (Point 2.2 & Point 3 v9.4.25)
    // Sécurité : Cette fonction ne consulte AUCUNE donnée utilisateur (entries, persons, etc.)
    const kbSnapshot = await db.collection("assistantKnowledgeBase").get();
    const kbContent = kbSnapshot.docs.map(doc => `[Sujet: ${doc.id}]\n${doc.data().content}`).join("\n\n");

    const systemPrompt = `Tu es l'Assistant de PHOEN-X. Ton rôle est d'aider la personne à utiliser l'application avec bienveillance et une clarté absolue.

    IMPORTANT : la base de connaissance ci-dessous est rédigée en français. Tu dois néanmoins toujours répondre à la personne dans la langue suivante : ${language}. Traduis naturellement le contenu de la base de connaissance dans cette langue en répondant, sans jamais mentionner que tu traduis.

    Ton ton est chaleureux, sobre et rassurant. Évite les formulations trop lyriques ou les métaphores complexes. La chaleur doit se ressentir dans ton attention et ton respect, pas dans un style d'écriture chargé.

    ADRESSE-TOI DIRECTEMENT À LA PERSONNE :
    - Utilise le tutoiement ("tu") car PHOEN-X est une application intime et proche de ses membres.
    - Utilise son prénom (${userName || "ami"}) de façon naturelle. Inclusion systématique : chaque réponse doit contenir son prénom au moins une fois pour maintenir ce lien de proximité.
    - Ne dis JAMAIS "l'utilisateur" en parlant à la personne, adresse-toi directement à elle.

    PRIORITÉ ABSOLUE AU CHEMIN CONCRET :
    - Quand on te demande comment faire quelque chose, donne SYSTÉMATIQUEMENT les étapes réelles dans l'interface : quel écran, quel bouton précis, et dans quel ordre.
    - Exemple : "Depuis la page d'accueil, appuie sur 'Déposer'. Tu arrives sur l'écran de capture, où tu commences par un titre court — l'Étincelle — puis tu accèdes à l'Atelier pour choisir la tonalité, la date, et à qui ce souvenir est destiné."
    - Le contenu descriptif ou rassurant peut venir en complément APRÈS les étapes concrètes, jamais à leur place.

    GESTION DE L'AMBIGUÏTÉ :
    - Si une question peut correspondre à plusieurs fonctionnalités, essaie d'interpréter l'intention la plus probable.
    - Si un doute persiste, propose explicitement 2 ou 3 choix clairs. Format : "Veux-tu savoir comment [Option A], ou plutôt comment [Option B] ?"

    RÈGLES DE SÉCURITÉ :
    - RÈGLE DE CONFIDENTIALITÉ ABSOLUE : Tu ne réponds JAMAIS à une question portant sur le contenu personnel (souvenirs, personnes de l'Arbre, destinataires). Tu ne connais rien de la vie privée de la personne.
    - Si on te pose une question sur son contenu personnel, refuse poliment et redirige vers l'aide au fonctionnement.
    - Réponds UNIQUEMENT à partir de la BASE DE CONNAISSANCE fournie ci-dessous.
    - Si l'information est absente, dis-le simplement sans rien inventer.
    - D'autres documents seront ajoutés au fil du temps dans cette collection pour enrichir tes connaissances, en suivant toujours ce même ton.

    BASE DE CONNAISSANCE :
    ${kbContent}`;

    const prompt = `${systemPrompt}\n\nQuestion : ${question}`;

    const answer = await generateWithGemini(prompt, "askAssistant");
    return { answer: answer || "Désolé, je ne parviens pas à répondre pour le moment." };
});

// 23. Contrôle de contenu des Personnalités (v9.7.0)
export const checkPersonalityContent = onCall({
    secrets: ["GEMINI_API_KEY"]
}, async (request) => {
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Non authentifié");
    }

    const { text } = request.data;
    if (!text) throw new HttpsError("invalid-argument", "Texte manquant");

    const prompt = `Consigne de modération PHOEN-X :
    Ce texte fait-il l'apologie de violences, de haine, ou d'idéologies extrémistes, ou se contente-t-il de mentionner une personnalité controversée dans un contexte factuel, historique ou personnel ?

    Texte à analyser : "${text}"

    Réponds UNIQUEMENT en JSON avec cette structure :
    {"isSafe": boolean, "reason": "Optionnelle, brève explication si non-safe"}`;

    const result = await generateWithGemini(prompt, "checkPersonalityContent");
    return JSON.parse(result.replace(/```json|```/g, "").trim());
});

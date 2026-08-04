import { onCall, HttpsError } from "firebase-functions/v2/https";
import { GoogleGenAI } from "@google/genai";
import { db } from "./admin";

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
    if (!summariesAtThatAge?.length) return "";
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

// 20. Modification d'un chapitre par l'IA (v8.6.3)
export const modifyBookChapter = onCall({
    secrets: ["GEMINI_API_KEY"]
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

    const content = await generateWithGemini(prompt);
    return { content: content || "" };
});

// 22. Assistant IA (v9.4.25)
export const askAssistant = onCall({
    secrets: ["GEMINI_API_KEY"]
}, async (request) => {
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Non authentifié");
    }
    const { question, userName } = request.data;
    if (!question) throw new HttpsError("invalid-argument", "Question manquante");

    // 1. Récupération STRICTE de la base de connaissance (Point 2.2 & Point 3 v9.4.25)
    // Sécurité : Cette fonction ne consulte AUCUNE donnée utilisateur (entries, persons, etc.)
    const kbSnapshot = await db.collection("assistantKnowledgeBase").get();
    const kbContent = kbSnapshot.docs.map(doc => `[Sujet: ${doc.id}]\n${doc.data().content}`).join("\n\n");

    const systemPrompt = `Tu es l'Assistant de PHOEN-X. Ton rôle est d'aider la personne à utiliser l'application avec bienveillance et une clarté absolue.

    Ton ton est chaleureux, sobre et rassurant. Évite les formulations trop lyriques ou les métaphores complexes. La chaleur doit se ressentir dans ton attention et ton respect, pas dans un style d'écriture chargé.

    ADRESSE-TOI DIRECTEMENT À LA PERSONNE :
    - Utilise le tutoiement ("tu") car PHOEN-X est une application intime et proche de ses membres.
    - Utilise son prénom (${userName || "ami"}) de façon naturelle.
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

    const answer = await generateWithGemini(prompt);
    return { answer: answer || "Désolé, je ne parviens pas à répondre pour le moment." };
});

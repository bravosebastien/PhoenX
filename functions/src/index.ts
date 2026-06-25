import { onRequest, onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { VertexAI } from "@google-cloud/vertexai";

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
    model: "gemini-1.5-flash-001", // Version Cloud (Pro/Flash) pour fonctions complexes
});

const AI_RULES = `
Tu es l'IA de PHOEN-X, une plateforme de mémoire vivante.
Tu traites des contenus personnels et intimes.
- Ne génère JAMAIS de contenu à la première personne du présent (ex: "Je suis").
- Utilise TOUJOURS le conditionnel pour tes interprétations.
- Tu n'es jamais clinique ou psychologique. Tu es un accompagnateur chaleureux.
- Réponds UNIQUEMENT en JSON valide si demandé.
`;

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
    return JSON.parse(result.response.candidates[0].content.parts[0].text || "{}");
});

// 2. Génération de la Question du Biographe (Hebdomadaire)
export const generateBiographerQuestion = onCall(async (request) => {
    const { themes } = request.data; // Thèmes récents pour personnaliser

    const prompt = `${AI_RULES}
    Génère UNE question de biographe de maximum 15 mots.
    Elle doit être ouverte, jamais morbide, jamais clinique.
    Elle doit inciter à la transmission.
    Thèmes récents de l'utilisateur : ${themes || "vie en général"}.
    Réponds juste par la question entre guillemets.`;

    const result = await generativeModel.generateContent(prompt);
    return result.response.candidates[0].content.parts[0].text || "Quel est le souvenir qui te fait sourire aujourd'hui ?";
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
    return result.response.candidates[0].content.parts[0].text || "";
});

// 4. Détection d'Évolution (Dialogue Temporel)
export const detectThoughtEvolution = onCall(async (request) => {
    const { entriesByAge } = request.data;

    const prompt = `${AI_RULES}
    Identifie les transitions thématiques majeures par âge dans ces pensées.
    Réponds en JSON : {"transitions": [{"fromAge": number, "toAge": number, "description": "15 mots max, conditionnel"}]}
    Données : ${JSON.stringify(entriesByAge)}`;

    const result = await generativeModel.generateContent(prompt);
    return JSON.parse(result.response.candidates[0].content.parts[0].text || '{"transitions":[]}');
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
    return result.response.candidates[0].content.parts[0].text || "";
});

// 6. Synthèse Narrative d'un Portrait de Proche
export const generatePortraitNarrative = onCall(async (request) => {
    const { answers } = request.data;

    const prompt = `${AI_RULES}
    Rédige une synthèse narrative chaleureuse (3-5 phrases) du regard de l'utilisateur sur son proche.
    Utilise le conditionnel et la 1ère personne de l'utilisateur.
    Réponses aux questions : ${answers.join(" | ")}`;

    const result = await generativeModel.generateContent(prompt);
    return result.response.candidates[0].content.parts[0].text || "";
});

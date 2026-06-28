import { onCall, HttpsError } from "firebase-functions/v2/https";
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
    const responseText = result.response.candidates?.[0]?.content.parts[0]?.text || "{}";
    const cleaned = responseText.replace(/```json|```/g, "").trim();
    return JSON.parse(cleaned);
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

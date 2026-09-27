/**
 * Valeurs par défaut de la configuration de facturation, abonnements,
 * crédits de pages, crédits d'IA, fonctionnalités et promotions de PHOEN-X.
 *
 * Ces configurations sont initialisées automatiquement par le serveur dans Firestore (collection appConfig/*)
 * si un document est manquant. Elles peuvent ensuite être modifiées à tout moment via la console Firestore.
 */

// 1. Paramètres généraux de facturation et de comportement
export const defaultBillingConfig = {
    // Interrupteur général : si false et utilisateur non-testeur, aucun contrôle ni débit n'est appliqué
    enforcementEnabled: false,
    // Liste des UIDs bénéficiant des contrôles de facturation en avant-première (bêta-testeurs)
    testerUids: [] as string[],
    // Accepter les achats en environnement de test RevenueCat (Sandbox)
    acceptSandboxPurchases: true,
    // Palier d'abonnement par défaut lorsqu'aucun droit actif n'est trouvé
    defaultTier: "DECOUVERTE",
    // Ratio moyen du nombre de mots par page de livre
    wordsPerPage: 250,
    // Offre d'une page gratuite toutes les N pages générées (0 = désactivé)
    freePageEvery: 10,
    // Estimation indicative du nombre de pages générées par scène pour la réservation initiale
    estimatePagesPerScene: 0.6,
    // Mode de coût IA par fonction (free: gratuit, flat: forfaitaire, per_page: basé sur les pages réelles)
    aiCosts: {
        generateBookChapters: { mode: "per_page" },
        modifyBookChapter: { mode: "free", flat: 0 },
        generateGlobalIntro: { mode: "free", flat: 0 },
        generateBookPlan: { mode: "free", flat: 0 }
    },
    // Règle de report des pages mensuelles non utilisées d'un mois sur l'autre
    monthlyRollover: { enabled: true, capMonths: 3 },
    // Durée de validité en jours des crédits achetés (null = illimité)
    purchasedCreditsExpireDays: null as number | null,
    // Essai inversé automatique à la création du compte (Reverse Trial)
    reverseTrial: { enabled: false, tier: "ESSENCE", days: 30 }
};

// 2. Définition des 4 paliers d'abonnement (Paliers DECOUVERTE, ESSENCE, LIGNEE, PRESTIGE)
export const defaultTiersConfig = {
    DECOUVERTE: {
        rank: 0,
        name: { fr: "Découverte", en: "Discovery" },
        tagline: { fr: "Pour démarrer votre transmission et explorer PHOEN-X", en: "Start your legacy and explore PHOEN-X" },
        highlight: false,
        revenueCatEntitlement: null as string | null,
        storeProductIds: { monthly: null as string | null, annual: null as string | null },
        stripePriceIds: { monthly: null as string | null, annual: null as string | null },
        monthlyBookPages: 0,
        freePageEvery: null as number | null,
        features: {
            genealogy: false,
            portraits: false,
            literary: false,
            mappemonde: false,
            encounters: false,
            hundredQuestions: false,
            askQuestion: false,
            mirror: false,
            livingLink: false,
            essencePortrait: false,
            book: false,
            bookEnrichedSources: false,
            recipientAmbiance: false
        },
        limits: {
            recipients: 1,
            depositaries: 2,
            witnesses: 1,
            notificationContacts: 3,
            photos: 25,
            videos: 2,
            audios: 5,
            storageMb: 250,
            videoMaxSeconds: 90,
            youngSelfLetters: 1,
            vaultEnigmas: 1,
            ultimateSecrets: 0,
            rankings: 1,
            personalities: 3,
            reconciliations: 0,
            capsules: 0,
            bookMaxPages: 0
        }
    },
    ESSENCE: {
        rank: 1,
        name: { fr: "Essence", en: "Essence" },
        tagline: { fr: "L'essentiel pour transmettre vos souvenirs et générer votre Livre", en: "The essentials to pass on your memories and generate your Book" },
        highlight: false,
        revenueCatEntitlement: "essence",
        storeProductIds: { monthly: "phoenx_essence_monthly", annual: "phoenx_essence_annual" },
        stripePriceIds: { monthly: null as string | null, annual: null as string | null },
        monthlyBookPages: 80,
        freePageEvery: null as number | null,
        features: {
            genealogy: true,
            portraits: true,
            literary: true,
            mappemonde: true,
            encounters: true,
            hundredQuestions: true,
            askQuestion: true,
            mirror: true,
            livingLink: true,
            essencePortrait: true,
            book: true,
            bookEnrichedSources: true,
            recipientAmbiance: true
        },
        limits: {
            recipients: 6,
            depositaries: 3,
            witnesses: 5,
            notificationContacts: 10,
            photos: 750,
            videos: 40,
            audios: 150,
            storageMb: 5000,
            videoMaxSeconds: 90,
            youngSelfLetters: -1,
            vaultEnigmas: 10,
            ultimateSecrets: 1,
            rankings: 10,
            personalities: -1,
            reconciliations: 3,
            capsules: 5,
            bookMaxPages: 80
        }
    },
    LIGNEE: {
        rank: 2,
        name: { fr: "Lignée", en: "Lineage" },
        tagline: { fr: "L'expérience complète pour rassembler toute votre famille", en: "The complete experience to bring your entire family together" },
        highlight: true,
        revenueCatEntitlement: "lignee",
        storeProductIds: { monthly: "phoenx_lignee_monthly", annual: "phoenx_lignee_annual" },
        stripePriceIds: { monthly: null as string | null, annual: null as string | null },
        monthlyBookPages: 160,
        freePageEvery: null as number | null,
        features: {
            genealogy: true,
            portraits: true,
            literary: true,
            mappemonde: true,
            encounters: true,
            hundredQuestions: true,
            askQuestion: true,
            mirror: true,
            livingLink: true,
            essencePortrait: true,
            book: true,
            bookEnrichedSources: true,
            recipientAmbiance: true
        },
        limits: {
            recipients: 15,
            depositaries: 3,
            witnesses: 10,
            notificationContacts: 25,
            photos: 2500,
            videos: 150,
            audios: 500,
            storageMb: 20000,
            videoMaxSeconds: 90,
            youngSelfLetters: -1,
            vaultEnigmas: -1,
            ultimateSecrets: 3,
            rankings: -1,
            personalities: -1,
            reconciliations: -1,
            capsules: 20,
            bookMaxPages: 80
        }
    },
    PRESTIGE: {
        rank: 3,
        name: { fr: "Prestige", en: "Prestige" },
        tagline: { fr: "Sans limite de mémoire pour transmettre un patrimoine illimité", en: "Unlimited memory to pass on a limitless heritage" },
        highlight: false,
        revenueCatEntitlement: "prestige",
        storeProductIds: { monthly: "phoenx_prestige_monthly", annual: "phoenx_prestige_annual" },
        stripePriceIds: { monthly: null as string | null, annual: null as string | null },
        monthlyBookPages: 400,
        freePageEvery: null as number | null,
        features: {
            genealogy: true,
            portraits: true,
            literary: true,
            mappemonde: true,
            encounters: true,
            hundredQuestions: true,
            askQuestion: true,
            mirror: true,
            livingLink: true,
            essencePortrait: true,
            book: true,
            bookEnrichedSources: true,
            recipientAmbiance: true
        },
        limits: {
            recipients: -1,
            depositaries: 5,
            witnesses: -1,
            notificationContacts: -1,
            photos: 10000,
            videos: 400,
            audios: -1,
            storageMb: 60000,
            videoMaxSeconds: 90,
            youngSelfLetters: -1,
            vaultEnigmas: -1,
            ultimateSecrets: -1,
            rankings: -1,
            personalities: -1,
            reconciliations: -1,
            capsules: -1,
            bookMaxPages: 200
        }
    }
};

// 3. Catalogue des fonctionnalités et leurs descriptions chaleureuses
export const defaultFeaturesConfig = {
    genealogy: {
        name: { fr: "Arbre Généalogique", en: "Genealogy Tree" },
        description: { fr: "Tissez les liens de votre histoire familiale et situez chaque souvenir dans le temps.", en: "Weave your family story and locate every memory in time." },
        imageUrl: null as string | null
    },
    portraits: {
        name: { fr: "Portraits Proches", en: "Loved Ones Portraits" },
        description: { fr: "Dressez le portrait vivant des personnes qui comptent le plus dans votre vie.", en: "Paint vivid portraits of the people who matter most in your life." },
        imageUrl: null as string | null
    },
    literary: {
        name: { fr: "Bibliothèque Littérature", en: "Literary Library" },
        description: { fr: "Enrichissez vos écrits d'extraits célèbres, de citations et d'inspirations poétiques.", en: "Enrich your writings with famous quotes, excerpts, and poetic inspiration." },
        imageUrl: null as string | null
    },
    mappemonde: {
        name: { fr: "Mappemonde des Souvenirs", en: "Memory World Map" },
        description: { fr: "Ancrez vos histoires aux quatre coins du monde sur une carte interactive.", en: "Anchor your stories across the globe on an interactive map." },
        imageUrl: null as string | null
    },
    encounters: {
        name: { fr: "Rencontres Marquantes", en: "Meaningful Encounters" },
        description: { fr: "Racontez les chemins croisés, les amitiés décisives et les grands amours.", en: "Tell the stories of crossed paths, decisive friendships, and great loves." },
        imageUrl: null as string | null
    },
    hundredQuestions: {
        name: { fr: "100 Questions de Vie", en: "100 Life Questions" },
        description: { fr: "Laissez-vous guider par des questions inspirantes pour réveiller vos souvenirs.", en: "Let inspiring prompts guide you to awaken deeply buried memories." },
        imageUrl: null as string | null
    },
    askQuestion: {
        name: { fr: "Poser une Question", en: "Ask a Question" },
        description: { fr: "Permettez à vos proches de vous interroger sur des chapitres précis de votre vie.", en: "Allow your loved ones to ask you questions about specific chapters of your life." },
        imageUrl: null as string | null
    },
    mirror: {
        name: { fr: "Miroir à Deux", en: "Mirror for Two" },
        description: { fr: "Partagez un espace complice pour écrire vos souvenirs communs à quatre mains.", en: "Share a private space to co-write joint memories together." },
        imageUrl: null as string | null
    },
    livingLink: {
        name: { fr: "Transmission Active", en: "Living Link" },
        description: { fr: "Transmettez des messages vivants et guidés à vos proches de votre vivant.", en: "Share active, guided messages with your loved ones during your lifetime." },
        imageUrl: null as string | null
    },
    essencePortrait: {
        name: { fr: "Portrait d'Essence", en: "Essence Portrait" },
        description: { fr: "Capturez la quintessence de votre personnalité à travers vos récits.", en: "Capture the essence of your personality through your stories." },
        imageUrl: null as string | null
    },
    book: {
        name: { fr: "Génération du Livre", en: "Book Generation" },
        description: { fr: "Transformez vos souvenirs fragmentés en un véritable roman autobiographique chiffré.", en: "Transform your fragmented memories into an encrypted autobiographical novel." },
        imageUrl: null as string | null
    },
    bookEnrichedSources: {
        name: { fr: "Sources Enrichies pour le Livre", en: "Enriched Sources for Book" },
        description: { fr: "Intégrez vos enregistrements vocaux, photos et lettres dans la trame de votre livre.", en: "Integrate your voice notes, photos, and letters into your book narrative." },
        imageUrl: null as string | null
    },
    recipientAmbiance: {
        name: { fr: "Habillage du Livre Héritier", en: "Heir Book Styling" },
        description: { fr: "Offrez à vos lecteurs une expérience visuelle et poétique personnalisée.", en: "Offer your readers a personalized visual and poetic reading experience." },
        imageUrl: null as string | null
    }
};

// 4. Packs de recharge de crédits de pages
export const defaultCreditPacksConfig = {
    packs: [
        {
            id: "pages_50",
            pages: 50,
            storeProductId: "phoenx_pages_50",
            stripePriceId: null as string | null,
            name: { fr: "50 pages", en: "50 pages" },
            badge: null as { fr: string, en: string } | null
        },
        {
            id: "pages_150",
            pages: 150,
            storeProductId: "phoenx_pages_150",
            stripePriceId: null as string | null,
            name: { fr: "150 pages", en: "150 pages" },
            badge: { fr: "Le plus avantageux", en: "Best value" }
        }
    ]
};

// 5. Structure des promotions et campagnes
export const defaultPromotionsConfig = {
    campaigns: [] as Array<{
        id: string;
        active: boolean;
        priority: number;
        startsAt: string;
        endsAt: string;
        audience: {
            all?: boolean;
            tiers?: string[];
            newUsersWithinDays?: number;
            uids?: string[];
        };
        effect: {
            type: "pack_bonus_percent" | "page_discount_percent" | "free_page_every" | "gift_pages" | "trial_tier";
            value: number;
            tier?: string;
            days?: number;
        };
        bannerText: { fr: string; en: string };
    }>
};

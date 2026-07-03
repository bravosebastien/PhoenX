package com.example.phoenx.ui.screens.questions

data class Question(
    val id: String,
    val text: String,
    val category: String
)

object QuestionsData {
    val categories = listOf(
        "Toutes", "Enfance", "Famille", "Amour", "Amitié",
        "Travail", "Argent & Réussite", "Valeurs",
        "Foi & Spiritualité", "Corps & Santé", "Regrets",
        "Rêves", "Voyages & Lieux", "Créativité & Passions",
        "Secrets & Aveux", "Sagesse"
    )

    val allQuestions = listOf(
        // ENFANCE
        Question("e1", "Quel est ton plus vieux souvenir ?", "Enfance"),
        Question("e2", "Quelle odeur te rappelle ta maison d'enfance ?", "Enfance"),
        Question("e3", "Qui était ton héros quand tu étais petit ?", "Enfance"),
        Question("e4", "Quel était ton jouet préféré ?", "Enfance"),
        Question("e5", "À quoi ressemblait ta chambre d'enfant ?", "Enfance"),
        Question("e6", "Quelle était ta plus grande peur étant petit ?", "Enfance"),
        Question("e7", "Quel est le plus beau cadeau que tu aies reçu enfant ?", "Enfance"),
        Question("e8", "Quel genre d'élève étais-tu à l'école ?", "Enfance"),
        Question("e9", "Quel est le premier métier que tu as rêvé de faire ?", "Enfance"),
        Question("e10", "Quel goût a ton enfance ?", "Enfance"),

        // FAMILLE
        Question("f1", "Que t'a transmis ton père que tu portes encore ?", "Famille"),
        Question("f2", "Quel est le meilleur conseil que ta mère t'ait donné ?", "Famille"),
        Question("f3", "Quel est ton souvenir de famille le plus joyeux ?", "Famille"),
        Question("f4", "Y a-t-il une tradition familiale que tu chéris ?", "Famille"),
        Question("f5", "À qui ressembles-tu le plus dans ta famille ?", "Famille"),
        Question("f6", "Quelle a été la plus grande épreuve de ta famille ?", "Famille"),
        Question("f7", "Que voudrais-tu dire à tes ancêtres si tu les voyais ?", "Famille"),
        Question("f8", "Quel est le membre de ta famille qui t'a le plus inspiré ?", "Famille"),
        Question("f9", "Quelle histoire familiale racontes-tu toujours ?", "Famille"),
        Question("f10", "Qu'est-ce que 'famille' signifie pour toi aujourd'hui ?", "Famille"),

        // AMOUR
        Question("a1", "Te souviens-tu de ton premier amour ?", "Amour"),
        Question("a2", "Qu'est-ce qui t'a fait tomber amoureux la première fois ?", "Amour"),
        Question("a3", "Quelle est la plus belle preuve d'amour reçue ?", "Amour"),
        Question("a4", "Crois-tu au coup de foudre ?", "Amour"),
        Question("a5", "Quelle chanson te rappelle une personne aimée ?", "Amour"),
        Question("a6", "Qu'est-ce que tu as appris sur l'amour avec le temps ?", "Amour"),
        Question("a7", "Quel sacrifice as-tu fait par amour ?", "Amour"),
        Question("a8", "Quelle est la définition d'un couple réussi selon toi ?", "Amour"),
        Question("a9", "As-tu déjà eu le cœur brisé ? Comment as-tu guéri ?", "Amour"),
        Question("a10", "Quelle trace l'amour a-t-il laissé sur ta vie ?", "Amour"),

        // AMITIÉ
        Question("am1", "Qui a été ton ami(e) le plus fidèle et pourquoi ?", "Amitié"),
        Question("am2", "Y a-t-il une amitié que tu regrettes d'avoir perdue ?", "Amitié"),
        Question("am3", "Qu'est-ce qu'un vrai ami selon toi ?", "Amitié"),
        Question("am4", "Quelle amitié t'a le plus surpris(e) ?", "Amitié"),
        Question("am5", "As-tu déjà trahi un(e) ami(e) ? Comment tu vis ça ?", "Amitié"),
        Question("am6", "Qui aurait dû être dans ta vie plus longtemps ?", "Amitié"),
        Question("am7", "Qu'est-ce que tes amis voient en toi que tu ne vois pas ?", "Amitié"),
        Question("am8", "Y a-t-il quelqu'un à qui tu dois des excuses ?", "Amitié"),
        Question("am9", "Quelle amitié t'a changé(e) profondément ?", "Amitié"),
        Question("am10", "Si tu pouvais retrouver quelqu'un, qui ce serait ?", "Amitié"),

        // TRAVAIL
        Question("t1", "Quel a été ton tout premier travail ?", "Travail"),
        Question("t2", "Quelle est ta plus grande fierté professionnelle ?", "Travail"),
        Question("t3", "As-tu déjà eu envie de tout plaquer ?", "Travail"),
        Question("t4", "Quel patron ou collègue t'a le plus marqué ?", "Travail"),
        Question("t5", "Quelle compétence as-tu mis du temps à acquérir ?", "Travail"),
        Question("t6", "Quel échec pro t'a finalement fait grandir ?", "Travail"),
        Question("t7", "Travailler, c'est pour toi un devoir ou un plaisir ?", "Travail"),
        Question("t8", "Quelle ambiance de bureau te manque ?", "Travail"),
        Question("t9", "Quel rêve de carrière n'as-tu pas réalisé ?", "Travail"),
        Question("t10", "Quel conseil donnerais-tu à un débutant aujourd'hui ?", "Travail"),

        // ARGENT & RÉUSSITE
        Question("ar1", "Quelle est ta plus grande réussite financière ou pro ?", "Argent & Réussite"),
        Question("ar2", "As-tu déjà tout perdu ? Comment tu t'en es relevé(e) ?", "Argent & Réussite"),
        Question("ar3", "L'argent t'a-t-il rendu(e) plus heureux/heureuse ?", "Argent & Réussite"),
        Question("ar4", "Qu'est-ce que tu as sacrifié pour réussir ?", "Argent & Réussite"),
        Question("ar5", "Si tu pouvais tout recommencer professionnellement ?", "Argent & Réussite"),
        Question("ar6", "Qu'est-ce que le succès signifie vraiment pour toi ?", "Argent & Réussite"),
        Question("ar7", "As-tu déjà fait quelque chose dont tu n'es pas fier(e) pour de l'argent ?", "Argent & Réussite"),
        Question("ar8", "Quelle leçon financière tu aurais voulu apprendre plus tôt ?", "Argent & Réussite"),
        Question("ar9", "Qu'est-ce que tu laisses comme héritage professionnel ?", "Argent & Réussite"),
        Question("ar10", "Qu'est-ce que tu n'as jamais pu t'offrir et que tu voulais ?", "Argent & Réussite"),

        // VALEURS
        Question("v1", "Quelle est la valeur la plus importante pour toi ?", "Valeurs"),
        Question("v2", "Pour quelle cause pourrais-tu te battre ?", "Valeurs"),
        Question("v3", "Qu'est-ce qui te met le plus en colère ?", "Valeurs"),
        Question("v4", "De quoi es-tu le plus fier dans ton caractère ?", "Valeurs"),
        Question("v5", "Quel défaut as-tu appris à accepter chez toi ?", "Valeurs"),
        Question("v6", "Quelle est ta définition de l'intégrité ?", "Valeurs"),
        Question("v7", "Quel est l'acte de courage dont tu es le plus fier ?", "Valeurs"),
        Question("v8", "À quoi ne pourrais-tu jamais renoncer ?", "Valeurs"),
        Question("v9", "Que signifie 'être une bonne personne' ?", "Valeurs"),
        Question("v10", "Quelle est ta plus grande force intérieure ?", "Valeurs"),

        // FOI & SPIRITUALITÉ
        Question("fs1", "Crois-tu en quelque chose de plus grand que toi ?", "Foi & Spiritualité"),
        Question("fs2", "Qu'est-ce qui se passe après la mort selon toi ?", "Foi & Spiritualité"),
        Question("fs3", "As-tu eu un moment de doute profond dans ta foi ?", "Foi & Spiritualité"),
        Question("fs4", "Y a-t-il un moment où tu as senti une présence ?", "Foi & Spiritualité"),
        Question("fs5", "La mort d'un proche a-t-elle changé tes croyances ?", "Foi & Spiritualité"),
        Question("fs6", "Qu'est-ce qui donne du sens à ta vie ?", "Foi & Spiritualité"),
        Question("fs7", "As-tu prié ou médité ? Qu'est-ce que ça t'a apporté ?", "Foi & Spiritualité"),
        Question("fs8", "Qu'est-ce que tu espères trouver après ta mort ?", "Foi & Spiritualité"),
        Question("fs9", "Y a-t-il des rituels qui comptent pour toi ?", "Foi & Spiritualité"),
        Question("fs10", "Qu'est-ce que tu voudrais que tes proches retiennent de ta façon de voir la vie ?", "Foi & Spiritualité"),

        // CORPS & SANTÉ
        Question("cs1", "Quelle épreuve physique t'a le plus marqué(e) ?", "Corps & Santé"),
        Question("cs2", "As-tu vécu une maladie grave ? Comment tu l'as traversée ?", "Corps & Santé"),
        Question("cs3", "Quel rapport as-tu eu avec ton corps au fil des années ?", "Corps & Santé"),
        Question("cs4", "Y a-t-il quelque chose que tu aurais dû faire pour prendre soin de toi plus tôt ?", "Corps & Santé"),
        Question("cs5", "La douleur t'a-t-elle appris quelque chose ?", "Corps & Santé"),
        Question("cs6", "As-tu eu peur de mourir ? Quand ?", "Corps & Santé"),
        Question("cs7", "Qu'est-ce que ton corps a enduré que personne ne sait ?", "Corps & Santé"),
        Question("cs8", "Comment ta santé a-t-elle changé ta façon de vivre ?", "Corps & Santé"),
        Question("cs9", "Y a-t-il quelque chose que tu aurais voulu faire physiquement et que tu n'as pas pu ?", "Corps & Santé"),
        Question("cs10", "Qu'est-ce que tu voudrais dire à ton corps ?", "Corps & Santé"),

        // REGRETS
        Question("r1", "Quel est ton plus grand regret ?", "Regrets"),
        Question("r2", "Si tu pouvais revenir en arrière, que changerais-tu ?", "Regrets"),
        Question("r3", "Quelle opportunité as-tu laissé passer ?", "Regrets"),
        Question("r4", "Y a-t-il une parole que tu aurais aimé retirer ?", "Regrets"),
        Question("r5", "À qui n'as-tu pas dit 'je t'aime' assez souvent ?", "Regrets"),
        Question("r6", "Quelle décision a changé le cours de ta vie ?", "Regrets"),
        Question("r7", "De quoi n'es-tu pas fier ?", "Regrets"),
        Question("r8", "As-tu des remords envers quelqu'un ?", "Regrets"),
        Question("r9", "Que ferais-tu différemment si tu avais su ?", "Regrets"),
        Question("r10", "Comment vis-tu avec tes regrets aujourd'hui ?", "Regrets"),

        // RÊVES
        Question("re1", "Quel est ton plus grand rêve réalisé ?", "Rêves"),
        Question("re2", "Où rêverais-tu de vivre ?", "Rêves"),
        Question("re3", "Quel rêve d'enfant as-tu abandonné ?", "Rêves"),
        Question("re4", "Qu'est-ce qui te fait encore rêver aujourd'hui ?", "Rêves"),
        Question("re5", "Si tu n'avais aucune limite, que ferais-tu ?", "Rêves"),
        Question("re6", "Quel est le rêve le plus fou que tu aies fait ?", "Rêves"),
        Question("re7", "À quoi ressemblerait ta journée idéale ?", "Rêves"),
        Question("re8", "Quelle aventure aimerais-tu encore vivre ?", "Rêves"),
        Question("re9", "Qui aurais-tu aimé rencontrer ?", "Rêves"),
        Question("re10", "Quel message tes rêves t'ont-ils laissé ?", "Rêves"),

        // VOYAGES & LIEUX
        Question("vl1", "Quel voyage a changé quelque chose en toi ?", "Voyages & Lieux"),
        Question("vl2", "Y a-t-il un endroit où tu te sens vraiment toi-même ?", "Voyages & Lieux"),
        Question("vl3", "Quel lieu voudrais-tu avoir vu avant de mourir ?", "Voyages & Lieux"),
        Question("vl4", "Un voyage que tu regrettes de ne pas avoir fait ?", "Voyages & Lieux"),
        Question("vl5", "Quel endroit dans le monde te manque ?", "Voyages & Lieux"),
        Question("vl6", "Y a-t-il un lieu lié à un souvenir douloureux ?", "Voyages & Lieux"),
        Question("vl7", "Quel pays t'a le plus surpris(e) ?", "Voyages & Lieux"),
        Question("vl8", "Si tu pouvais vivre ailleurs, où ce serait ?", "Voyages & Lieux"),
        Question("vl9", "Quel voyage as-tu fait seul(e) et qu'est-ce que ça t'a appris ?", "Voyages & Lieux"),
        Question("vl10", "Y a-t-il un endroit de ton enfance que tu voudrais revoir ?", "Voyages & Lieux"),

        // CRÉATIVITÉ & PASSIONS
        Question("cp1", "Qu'est-ce que tu as créé dont tu es le plus fier(e) ?", "Créativité & Passions"),
        Question("cp2", "Y a-t-il un talent que tu n'as jamais développé ?", "Créativité & Passions"),
        Question("cp3", "Quelle passion as-tu abandonnée et pourquoi ?", "Créativité & Passions"),
        Question("cp4", "Qu'est-ce qui te met dans un état de flow total ?", "Créativité & Passions"),
        Question("cp5", "As-tu une œuvre — livre, musique, art — qui t'a changé(e)?", "Créativité & Passions"),
        Question("cp6", "Si tu avais été artiste, qu'est-ce que tu aurais créé ?", "Créativité & Passions"),
        Question("cp7", "Y a-t-il quelque chose que tu fais juste pour toi, sans que personne le sache ?", "Créativité & Passions"),
        Question("cp8", "Quelle passion voudrais-tu transmettre ?", "Créativité & Passions"),
        Question("cp9", "As-tu déjà créé quelque chose et tout arrêté ?", "Créativité & Passions"),
        Question("cp10", "Qu'est-ce que tu aurais voulu apprendre à faire ?", "Créativité & Passions"),

        // SECRETS & AVEUX
        Question("sa1", "Y a-t-il quelque chose que tu n'as dit à personne ?", "Secrets & Aveux"),
        Question("sa2", "As-tu gardé un secret toute ta vie ? Lequel ?", "Secrets & Aveux"),
        Question("sa3", "Y a-t-il quelque chose que tu as fait et dont tu n'as jamais parlé ?", "Secrets & Aveux"),
        Question("sa4", "Qu'est-ce que tu aurais voulu avouer à quelqu'un avant qu'il soit trop tard ?", "Secrets & Aveux"),
        Question("sa5", "Y a-t-il une vérité sur toi que tes proches ne connaissent pas ?", "Secrets & Aveux"),
        Question("sa6", "As-tu eu une vie secrète — même petite — que personne ne soupçonne ?", "Secrets & Aveux"),
        Question("sa7", "Qu'est-ce que tu as toujours voulu dire mais jamais osé ?", "Secrets & Aveux"),
        Question("sa8", "Y a-t-il quelque chose dont tu as honte et que tu portes seul(e) ?", "Secrets & Aveux"),
        Question("sa9", "Un aveu que tu ferais si tu savais que personne ne te jugerait ?", "Secrets & Aveux"),
        Question("sa10", "Qu'est-ce que tu emporterais comme secret dans ta tombe ?", "Secrets & Aveux"),

        // SAGESSE
        Question("s1", "Quelle est la leçon la plus dure que la vie t'ait apprise ?", "Sagesse"),
        Question("s2", "Comment ton regard sur le monde a-t-il changé avec l'âge ?", "Sagesse"),
        Question("s3", "Que dirais-tu au jeune homme/à la jeune femme que tu étais ?", "Sagesse"),
        Question("s4", "Qu'est-ce qui est essentiel pour être heureux, selon toi ?", "Sagesse"),
        Question("s5", "Quel est le secret d'une vie bien remplie ?", "Sagesse"),
        Question("s6", "Comment aimerais-tu que l'on se souvienne de toi ?", "Sagesse"),
        Question("s7", "Qu'est-ce que tu n'as plus peur de perdre aujourd'hui ?", "Sagesse"),
        Question("s8", "Si tu devais résumer ta vie en une phrase ?", "Sagesse"),
        Question("s9", "Quelle est ta définition de la paix ?", "Sagesse"),
        Question("s10", "Que laisses-tu derrière toi de plus précieux ?", "Sagesse")
    )
}

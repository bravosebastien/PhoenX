package com.example.phoenx.ui.screens.questions

import androidx.annotation.StringRes
import com.example.phoenx.R

data class Question(
    val id: String,
    val text: String,
    val category: String,
    @StringRes val textResId: Int = 0
)

object QuestionsData {
    val categories = listOf(
        "Toutes", "Enfance", "Famille", "Amour", "Amitié",
        "Travail", "Argent & Réussite", "Valeurs",
        "Foi & Spiritualité", "Corps & Santé", "Regrets",
        "Rêves", "Voyages & Lieux", "Créativité & Passions",
        "Secrets & Aveux", "Sagesse", "Mes Questions"
    )

    val allQuestions = listOf(
        // ENFANCE
        Question("e1", "Quel est ton plus vieux souvenir ?", "Enfance", R.string.question_text_e1),
        Question("e2", "Quelle odeur te rappelle ta maison d'enfance ?", "Enfance", R.string.question_text_e2),
        Question("e3", "Qui était ton héros quand tu étais petit ?", "Enfance", R.string.question_text_e3),
        Question("e4", "Quel était ton jouet préféré ?", "Enfance", R.string.question_text_e4),
        Question("e5", "À quoi ressemblait ta chambre d'enfant ?", "Enfance", R.string.question_text_e5),
        Question("e6", "Quelle était ta plus grande peur étant petit ?", "Enfance", R.string.question_text_e6),
        Question("e7", "Quel est le plus beau cadeau que tu aies reçu enfant ?", "Enfance", R.string.question_text_e7),
        Question("e8", "Quel genre d'élève étais-tu à l'école ?", "Enfance", R.string.question_text_e8),
        Question("e9", "Quel est le premier métier que tu as rêvé de faire ?", "Enfance", R.string.question_text_e9),
        Question("e10", "Quel goût a ton enfance ?", "Enfance", R.string.question_text_e10),

        // FAMILLE
        Question("f1", "Que t'a transmis ton père que tu portes encore ?", "Famille", R.string.question_text_f1),
        Question("f2", "Quel est le meilleur conseil que ta mère t'ait donné ?", "Famille", R.string.question_text_f2),
        Question("f3", "Quel est ton souvenir de famille le plus joyeux ?", "Famille", R.string.question_text_f3),
        Question("f4", "Y a-t-il une tradition familiale que tu chéris ?", "Famille", R.string.question_text_f4),
        Question("f5", "À qui ressembles-tu le plus dans ta famille ?", "Famille", R.string.question_text_f5),
        Question("f6", "Quelle a été la plus grande épreuve de ta famille ?", "Famille", R.string.question_text_f6),
        Question("f7", "Que voudrais-tu dire à tes ancêtres si tu les voyais ?", "Famille", R.string.question_text_f7),
        Question("f8", "Quel est le membre de ta famille qui t'a le plus inspiré ?", "Famille", R.string.question_text_f8),
        Question("f9", "Quelle histoire familiale racontes-tu toujours ?", "Famille", R.string.question_text_f9),
        Question("f10", "Qu'est-ce que 'famille' signifie pour toi aujourd'hui ?", "Famille", R.string.question_text_f10),

        // AMOUR
        Question("a1", "Te souviens-tu de ton premier amour ?", "Amour", R.string.question_text_a1),
        Question("a2", "Qu'est-ce qui t'a fait tomber amoureux la première fois ?", "Amour", R.string.question_text_a2),
        Question("a3", "Quelle est la plus belle preuve d'amour reçue ?", "Amour", R.string.question_text_a3),
        Question("a4", "Crois-tu au coup de foudre ?", "Amour", R.string.question_text_a4),
        Question("a5", "Quelle chanson te rappelle une personne aimée ?", "Amour", R.string.question_text_a5),
        Question("a6", "Qu'est-ce que tu as appris sur l'amour avec le temps ?", "Amour", R.string.question_text_a6),
        Question("a7", "Quel sacrifice as-tu fait par amour ?", "Amour", R.string.question_text_a7),
        Question("a8", "Quelle est la définition d'un couple réussi selon toi ?", "Amour", R.string.question_text_a8),
        Question("a9", "As-tu déjà eu le cœur brisé ? Comment as-tu guéri ?", "Amour", R.string.question_text_a9),
        Question("a10", "Quelle trace l'amour a-t-il laissé sur ta vie ?", "Amour", R.string.question_text_a10),

        // AMITIÉ
        Question("am1", "Qui a été ton ami(e) le plus fidèle et pourquoi ?", "Amitié", R.string.question_text_am1),
        Question("am2", "Y a-t-il une amitié que tu regrettes d'avoir perdue ?", "Amitié", R.string.question_text_am2),
        Question("am3", "Qu'est-ce qu'un vrai ami selon toi ?", "Amitié", R.string.question_text_am3),
        Question("am4", "Quelle amitié t'a le plus surpris(e) ?", "Amitié", R.string.question_text_am4),
        Question("am5", "As-tu déjà trahi un(e) ami(e) ? Comment tu vis ça ?", "Amitié", R.string.question_text_am5),
        Question("am6", "Qui aurait dû être dans ta vie plus longtemps ?", "Amitié", R.string.question_text_am6),
        Question("am7", "Qu'est-ce que tes amis voient en toi que tu ne vois pas ?", "Amitié", R.string.question_text_am7),
        Question("am8", "Y a-t-il quelqu'un à qui tu dois des excuses ?", "Amitié", R.string.question_text_am8),
        Question("am9", "Quelle amitié t'a changé(e) profondément ?", "Amitié", R.string.question_text_am9),
        Question("am10", "Si tu pouvais retrouver quelqu'un, qui ce serait ?", "Amitié", R.string.question_text_am10),

        // TRAVAIL
        Question("t1", "Quel a été ton tout premier travail ?", "Travail", R.string.question_text_t1),
        Question("t2", "Quelle est ta plus grande fierté professionnelle ?", "Travail", R.string.question_text_t2),
        Question("t3", "As-tu déjà eu envie de tout plaquer ?", "Travail", R.string.question_text_t3),
        Question("t4", "Quel patron ou collègue t'a le plus marqué ?", "Travail", R.string.question_text_t4),
        Question("t5", "Quelle compétence as-tu mis du temps à acquérir ?", "Travail", R.string.question_text_t5),
        Question("t6", "Quel échec pro t'a finalement fait grandir ?", "Travail", R.string.question_text_t6),
        Question("t7", "Travailler, c'est pour toi un devoir ou un plaisir ?", "Travail", R.string.question_text_t7),
        Question("t8", "Quelle ambiance de bureau te manque ?", "Travail", R.string.question_text_t8),
        Question("t9", "Quel rêve de carrière n'as-tu pas réalisé ?", "Travail", R.string.question_text_t9),
        Question("t10", "Quel conseil donnerais-tu à un débutant aujourd'hui ?", "Travail", R.string.question_text_t10),

        // ARGENT & RÉUSSITE
        Question("ar1", "Quelle est ta plus grande réussite financière ou pro ?", "Argent & Réussite", R.string.question_text_ar1),
        Question("ar2", "As-tu déjà tout perdu ? Comment tu t'en es relevé(e) ?", "Argent & Réussite", R.string.question_text_ar2),
        Question("ar3", "L'argent t'a-t-il rendu(e) plus heureux/heureuse ?", "Argent & Réussite", R.string.question_text_ar3),
        Question("ar4", "Qu'est-ce que tu as sacrifié pour réussir ?", "Argent & Réussite", R.string.question_text_ar4),
        Question("ar5", "Si tu pouvais tout recommencer professionnellement ?", "Argent & Réussite", R.string.question_text_ar5),
        Question("ar6", "Qu'est-ce que le succès signifie vraiment pour toi ?", "Argent & Réussite", R.string.question_text_ar6),
        Question("ar7", "As-tu déjà fait quelque chose dont tu n'es pas fier(e) pour de l'argent ?", "Argent & Réussite", R.string.question_text_ar7),
        Question("ar8", "Quelle leçon financière tu aurais voulu apprendre plus tôt ?", "Argent & Réussite", R.string.question_text_ar8),
        Question("ar9", "Qu'est-ce que tu laisses comme héritage professionnel ?", "Argent & Réussite", R.string.question_text_ar9),
        Question("ar10", "Qu'est-ce que tu n'as jamais pu t'offrir et que tu voulais ?", "Argent & Réussite", R.string.question_text_ar10),

        // VALEURS
        Question("v1", "Quelle est la valeur la plus importante pour toi ?", "Valeurs", R.string.question_text_v1),
        Question("v2", "Pour quelle cause pourrais-tu te battre ?", "Valeurs", R.string.question_text_v2),
        Question("v3", "Qu'est-ce qui te met le plus en colère ?", "Valeurs", R.string.question_text_v3),
        Question("v4", "De quoi es-tu le plus fier dans ton caractère ?", "Valeurs", R.string.question_text_v4),
        Question("v5", "Quel défaut as-tu appris à accepter chez toi ?", "Valeurs", R.string.question_text_v5),
        Question("v6", "Quelle est ta définition de l'intégrité ?", "Valeurs", R.string.question_text_v6),
        Question("v7", "Quel est l'acte de courage dont tu es le plus fier ?", "Valeurs", R.string.question_text_v7),
        Question("v8", "À quoi ne pourrais-tu jamais renoncer ?", "Valeurs", R.string.question_text_v8),
        Question("v9", "Que signifie 'être une bonne personne' ?", "Valeurs", R.string.question_text_v9),
        Question("v10", "Quelle est ta plus grande force intérieure ?", "Valeurs", R.string.question_text_v10),

        // FOI & SPIRITUALITÉ
        Question("fs1", "Crois-tu en quelque chose de plus grand que toi ?", "Foi & Spiritualité", R.string.question_text_fs1),
        Question("fs2", "Qu'est-ce qui se passe après la mort selon toi ?", "Foi & Spiritualité", R.string.question_text_fs2),
        Question("fs3", "As-tu eu un moment de doute profond dans ta foi ?", "Foi & Spiritualité", R.string.question_text_fs3),
        Question("fs4", "Y a-t-il un moment où tu as senti une présence ?", "Foi & Spiritualité", R.string.question_text_fs4),
        Question("fs5", "La mort d'un proche a-t-elle changé tes croyances ?", "Foi & Spiritualité", R.string.question_text_fs5),
        Question("fs6", "Qu'est-ce qui donne du sens à ta vie ?", "Foi & Spiritualité", R.string.question_text_fs6),
        Question("fs7", "As-tu prié ou médité ? Qu'est-ce que ça t'a apporté ?", "Foi & Spiritualité", R.string.question_text_fs7),
        Question("fs8", "Qu'est-ce que tu espères trouver après ta mort ?", "Foi & Spiritualité", R.string.question_text_fs8),
        Question("fs9", "Y a-t-il des rituels qui comptent pour toi ?", "Foi & Spiritualité", R.string.question_text_fs9),
        Question("fs10", "Qu'est-ce que tu voudrais que tes proches retiennent de ta façon de voir la vie ?", "Foi & Spiritualité", R.string.question_text_fs10),

        // CORPS & SANTÉ
        Question("cs1", "Quelle épreuve physique t'a le plus marqué(e) ?", "Corps & Santé", R.string.question_text_cs1),
        Question("cs2", "As-tu vécu une maladie grave ? Comment tu l'as traversée ?", "Corps & Santé", R.string.question_text_cs2),
        Question("cs3", "Quel rapport as-tu eu avec ton corps au fil des années ?", "Corps & Santé", R.string.question_text_cs3),
        Question("cs4", "Y a-t-il quelque chose que tu aurais dû faire pour prendre soin de toi plus tôt ?", "Corps & Santé", R.string.question_text_cs4),
        Question("cs5", "La douleur t'a-t-elle appris quelque chose ?", "Corps & Santé", R.string.question_text_cs5),
        Question("cs6", "As-tu eu peur de mourir ? Quand ?", "Corps & Santé", R.string.question_text_cs6),
        Question("cs7", "Qu'est-ce que ton corps a enduré que personne ne sait ?", "Corps & Santé", R.string.question_text_cs7),
        Question("cs8", "Comment ta santé a-t-elle changé ta façon de vivre ?", "Corps & Santé", R.string.question_text_cs8),
        Question("cs9", "Y a-t-il quelque chose que tu aurais voulu faire physiquement et que tu n'as pas pu ?", "Corps & Santé", R.string.question_text_cs9),
        Question("cs10", "Qu'est-ce que tu voudrais dire à ton corps ?", "Corps & Santé", R.string.question_text_cs10),

        // REGRETS
        Question("r1", "Quel est ton plus grand regret ?", "Regrets", R.string.question_text_r1),
        Question("r2", "Si tu pouvais revenir en arrière, que changerais-tu ?", "Regrets", R.string.question_text_r2),
        Question("r3", "Quelle opportunité as-tu laissé passer ?", "Regrets", R.string.question_text_r3),
        Question("r4", "Y a-t-il une parole que tu aurais aimé retirer ?", "Regrets", R.string.question_text_r4),
        Question("r5", "À qui n'as-tu pas dit 'je t'aime' assez souvent ?", "Regrets", R.string.question_text_r5),
        Question("r6", "Quelle décision a changé le cours de ta vie ?", "Regrets", R.string.question_text_r6),
        Question("r7", "De quoi n'es-tu pas fier ?", "Regrets", R.string.question_text_r7),
        Question("r8", "As-tu des remords envers quelqu'un ?", "Regrets", R.string.question_text_r8),
        Question("r9", "Que ferais-tu différemment si tu avais su ?", "Regrets", R.string.question_text_r9),
        Question("r10", "Comment vis-tu avec tes regrets aujourd'hui ?", "Regrets", R.string.question_text_r10),

        // RÊVES
        Question("re1", "Quel est ton plus grand rêve réalisé ?", "Rêves", R.string.question_text_re1),
        Question("re2", "Où rêverais-tu de vivre ?", "Rêves", R.string.question_text_re2),
        Question("re3", "Quel rêve d'enfant as-tu abandonné ?", "Rêves", R.string.question_text_re3),
        Question("re4", "Qu'est-ce qui te fait encore rêver aujourd'hui ?", "Rêves", R.string.question_text_re4),
        Question("re5", "Si tu n'avais aucune limite, que ferais-tu ?", "Rêves", R.string.question_text_re5),
        Question("re6", "Quel est le rêve le plus fou que tu aies fait ?", "Rêves", R.string.question_text_re6),
        Question("re7", "À quoi ressemblerait ta journée idéale ?", "Rêves", R.string.question_text_re7),
        Question("re8", "Quelle aventure aimerais-tu encore vivre ?", "Rêves", R.string.question_text_re8),
        Question("re9", "Qui aurais-tu aimé rencontrer ?", "Rêves", R.string.question_text_re9),
        Question("re10", "Quel message tes rêves t'ont-ils laissé ?", "Rêves", R.string.question_text_re10),

        // VOYAGES & LIEUX
        Question("vl1", "Quel voyage a changé quelque chose en toi ?", "Voyages & Lieux", R.string.question_text_vl1),
        Question("vl2", "Y a-t-il un endroit où tu te sens vraiment toi-même ?", "Voyages & Lieux", R.string.question_text_vl2),
        Question("vl3", "Quel lieu voudrais-tu avoir vu avant de mourir ?", "Voyages & Lieux", R.string.question_text_vl3),
        Question("vl4", "Un voyage que tu regrettes de ne pas avoir fait ?", "Voyages & Lieux", R.string.question_text_vl4),
        Question("vl5", "Quel endroit dans le monde te manque ?", "Voyages & Lieux", R.string.question_text_vl5),
        Question("vl6", "Y a-t-il un lieu lié à un souvenir douloureux ?", "Voyages & Lieux", R.string.question_text_vl6),
        Question("vl7", "Quel pays t'a le plus surpris(e) ?", "Voyages & Lieux", R.string.question_text_vl7),
        Question("vl8", "Si tu pouvais vivre ailleurs, où ce serait ?", "Voyages & Lieux", R.string.question_text_vl8),
        Question("vl9", "Quel voyage as-tu fait seul(e) et qu'est-ce que ça t'a appris ?", "Voyages & Lieux", R.string.question_text_vl9),
        Question("vl10", "Y a-t-il un endroit de ton enfance que tu voudrais revoir ?", "Voyages & Lieux", R.string.question_text_vl10),

        // CRÉATIVITÉ & PASSIONS
        Question("cp1", "Qu'est-ce que tu as créé dont tu es le plus fier(e) ?", "Créativité & Passions", R.string.question_text_cp1),
        Question("cp2", "Y a-t-il un talent que tu n'as jamais développé ?", "Créativité & Passions", R.string.question_text_cp2),
        Question("cp3", "Quelle passion as-tu abandonnée et pourquoi ?", "Créativité & Passions", R.string.question_text_cp3),
        Question("cp4", "Qu'est-ce qui te met dans un état de flow total ?", "Créativité & Passions", R.string.question_text_cp4),
        Question("cp5", "As-tu une œuvre — livre, musique, art — qui t'a changé(e)?", "Créativité & Passions", R.string.question_text_cp5),
        Question("cp6", "Si tu avais été artiste, qu'est-ce que tu aurais créé ?", "Créativité & Passions", R.string.question_text_cp6),
        Question("cp7", "Y a-t-il quelque chose que tu fais juste pour toi, sans que personne le sache ?", "Créativité & Passions", R.string.question_text_cp7),
        Question("cp8", "Quelle passion voudrais-tu transmettre ?", "Créativité & Passions", R.string.question_text_cp8),
        Question("cp9", "As-tu déjà créé quelque chose et tout arrêté ?", "Créativité & Passions", R.string.question_text_cp9),
        Question("cp10", "Qu'est-ce que tu aurais voulu apprendre à faire ?", "Créativité & Passions", R.string.question_text_cp10),

        // SECRETS & AVEUX
        Question("sa1", "Y a-t-il quelque chose que tu n'as dit à personne ?", "Secrets & Aveux", R.string.question_text_sa1),
        Question("sa2", "As-tu gardé un secret toute ta vie ? Lequel ?", "Secrets & Aveux", R.string.question_text_sa2),
        Question("sa3", "Y a-t-il quelque chose que tu as fait et dont tu n'as jamais parlé ?", "Secrets & Aveux", R.string.question_text_sa3),
        Question("sa4", "Qu'est-ce que tu aurais voulu avouer à quelqu'un avant qu'il soit trop tard ?", "Secrets & Aveux", R.string.question_text_sa4),
        Question("sa5", "Y a-t-il une vérité sur toi que tes proches ne connaissent pas ?", "Secrets & Aveux", R.string.question_text_sa5),
        Question("sa6", "As-tu eu une vie secrète — même petite — que personne ne soupçonne ?", "Secrets & Aveux", R.string.question_text_sa6),
        Question("sa7", "Qu'est-ce que tu as toujours voulu dire mais jamais osé ?", "Secrets & Aveux", R.string.question_text_sa7),
        Question("sa8", "Y a-t-il quelque chose dont tu as honte et que tu portes seul(e) ?", "Secrets & Aveux", R.string.question_text_sa8),
        Question("sa9", "Un aveu que tu ferais si tu savais que personne ne te jugerait ?", "Secrets & Aveux", R.string.question_text_sa9),
        Question("sa10", "Qu'est-ce que tu emporterais comme secret dans ta tombe ?", "Secrets & Aveux", R.string.question_text_sa10),

        // SAGESSE
        Question("s1", "Quelle est la leçon la plus dure que la vie t'ait apprise ?", "Sagesse", R.string.question_text_s1),
        Question("s2", "Comment ton regard sur le monde a-t-il changé avec l'âge ?", "Sagesse", R.string.question_text_s2),
        Question("s3", "Que dirais-tu au jeune homme/à la jeune femme que tu étais ?", "Sagesse", R.string.question_text_s3),
        Question("s4", "Qu'est-ce qui est essentiel pour être heureux, selon toi ?", "Sagesse", R.string.question_text_s4),
        Question("s5", "Quel est le secret d'une vie bien remplie ?", "Sagesse", R.string.question_text_s5),
        Question("s6", "Comment aimerais-tu que l'on se souvienne de toi ?", "Sagesse", R.string.question_text_s6),
        Question("s7", "Qu'est-ce que tu n'as plus peur de perdre aujourd'hui ?", "Sagesse", R.string.question_text_s7),
        Question("s8", "Si tu devais résumer ta vie en une phrase ?", "Sagesse", R.string.question_text_s8),
        Question("s9", "Quelle est ta définition de la paix ?", "Sagesse", R.string.question_text_s9),
        Question("s10", "Que laisses-tu derrière toi de plus précieux ?", "Sagesse", R.string.question_text_s10)
    )
}

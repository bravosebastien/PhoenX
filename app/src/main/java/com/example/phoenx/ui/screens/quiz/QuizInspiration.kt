package com.example.phoenx.ui.screens.quiz

data class QuizInspiration(
    val question: String,
    val exampleAnswers: List<String>
)

object QuizInspirationData {
    val questions = listOf(
        QuizInspiration(
            "Dans quelle ville suis-je né(e) ?",
            listOf("Paris", "Lyon", "Marseille", "Bordeaux")
        ),
        QuizInspiration(
            "Quel était mon plat préféré ?",
            listOf("La blanquette", "Le couscous", "La paella", "Le bœuf bourguignon")
        ),
        QuizInspiration(
            "Quelle était ma couleur préférée ?",
            listOf("Le bleu", "Le rouge", "Le vert", "Le noir")
        ),
        QuizInspiration(
            "Quel était mon film préféré de tous les temps ?",
            listOf("Le Parrain", "Titanic", "Star Wars", "Amélie Poulain")
        ),
        QuizInspiration(
            "Dans quel pays ai-je fait mon plus beau voyage ?",
            listOf("L'Italie", "Le Japon", "Le Maroc", "Les États-Unis")
        ),
        QuizInspiration(
            "Quel était mon sport favori ?",
            listOf("Le football", "La natation", "Le tennis", "La course à pied")
        ),
        QuizInspiration(
            "Quelle était ma chanson préférée ?",
            listOf("Une chanson française", "Un classique rock", "De la soul", "Du jazz")
        ),
        QuizInspiration(
            "Quel était mon métier de rêve enfant ?",
            listOf("Astronaute", "Pompier", "Médecin", "Pilote")
        ),
        QuizInspiration(
            "Quelle était ma saison préférée ?",
            listOf("Le printemps", "L'été", "L'automne", "L'hiver")
        ),
        QuizInspiration(
            "Quel était mon animal préféré ?",
            listOf("Le chien", "Le chat", "Le cheval", "Le lion")
        ),
        QuizInspiration(
            "À quelle heure me levais-je naturellement ?",
            listOf("Avant 7h", "Entre 7h et 8h", "Entre 8h et 9h", "Après 9h")
        ),
        QuizInspiration(
            "Quel était mon genre de livre préféré ?",
            listOf("Les romans historiques", "Les thrillers", "La science-fiction", "Les biographies")
        ),
        QuizInspiration(
            "Quelle était ma plus grande peur ?",
            listOf("Le vide", "Le noir", "La foule", "La solitude")
        ),
        QuizInspiration(
            "Quel était mon péché mignon ?",
            listOf("Le chocolat", "Le fromage", "Les chips", "La glace")
        ),
        QuizInspiration(
            "Dans quelle ville aurais-je voulu vivre ?",
            listOf("New York", "Tokyo", "Rome", "Buenos Aires")
        ),
        QuizInspiration(
            "Quelle était ma façon de me détendre ?",
            listOf("La lecture", "La musique", "La marche", "La cuisine")
        ),
        QuizInspiration(
            "Quel était mon sport de cœur à regarder ?",
            listOf("Le football", "Le tennis", "Le rugby", "Le cyclisme")
        ),
        QuizInspiration(
            "Quelle était ma boisson du matin ?",
            listOf("Le café noir", "Le café au lait", "Le thé", "Le jus d'orange")
        ),
        QuizInspiration(
            "Quel était mon moyen de transport préféré ?",
            listOf("La voiture", "Le train", "L'avion", "Le vélo")
        ),
        QuizInspiration(
            "Quelle valeur était la plus importante pour moi ?",
            listOf("La famille", "La liberté", "L'honnêteté", "La générosité")
        )
    )
}

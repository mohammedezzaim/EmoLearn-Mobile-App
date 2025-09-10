package com.example.gencont_app.quiz

import com.example.gencont_app.configDB.sqlite.data.Question as EntityQuestion

// Fonction d'extension définie au top-level
fun EntityQuestion.toQuizQuestion(): Question {
    return Question(
        id = this.id,
        ref = this.ref,
        libelle = this.libelle,
        status_question = this.status_question,
        quizId = this.quizId
    )
}

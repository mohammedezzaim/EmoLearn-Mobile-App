package com.example.gencont_app.quiz

data class Question(
    val id: Long,
    val ref: String?,
    val libelle: String?,
    val status_question: String?,
    val quizId: Long
)
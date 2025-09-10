package com.example.gencont_app.quiz

// Dans QuestionAdapter.kt
data class QuizResult(
    val score: Int,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val incorrectAnswers: Int
)
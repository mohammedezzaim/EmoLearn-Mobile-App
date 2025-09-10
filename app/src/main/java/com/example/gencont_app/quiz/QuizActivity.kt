package com.example.gencont_app.quiz

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.gencont_app.R
import com.example.gencont_app.configDB.sqlite.database.AppDatabase
import com.example.gencont_app.score.ScoreActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QuizActivity : AppCompatActivity() {
    private lateinit var listViewQuestions: ListView
    private lateinit var btnSubmit: Button
    private var sectionId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_quiz)

        listViewQuestions = findViewById(R.id.listViewQuestions)
        btnSubmit = findViewById(R.id.btnSubmit)

        sectionId = intent.getLongExtra("section_id", -1L)
        val isRetake = intent.getBooleanExtra("retake", false)

        if (sectionId != -1L) {
            val db = AppDatabase.getInstance(applicationContext)
            CoroutineScope(Dispatchers.Main).launch {
                val quiz = db.quizDao().getQuizBySection(sectionId)
                quiz?.let {
                    val questions = db.questionDao().getQuestionsWithReponses(it.id)
                    val adapter = QuestionAdapter(this@QuizActivity, questions)
                    listViewQuestions.adapter = adapter

                    // Si c'est une reprise, effacer les sélections précédentes
                    if (isRetake) {
                        adapter.clearSelections()
                    }
                }
            }
        }

        btnSubmit.setOnClickListener {
            val adapter = listViewQuestions.adapter as QuestionAdapter
            val result = adapter.calculateDetailedResult()

            // Récupérer les IDs des questions et des réponses sélectionnées
            val questionIds = adapter.getQuestionIds()
            val selectedAnswerIds = adapter.getSelectedAnswerIds()

            val intent = Intent(this, ScoreActivity::class.java).apply {
                putExtra("score", result.score)
                putExtra("totalQuestions", result.totalQuestions)
                putExtra("correctAnswers", result.correctAnswers)
                putExtra("incorrectAnswers", result.incorrectAnswers)
                putExtra("section_id", sectionId)
                putExtra("questionIds", questionIds)
                putExtra("selectedAnswerIds", selectedAnswerIds)
            }
            startActivity(intent)

            // Ne pas finir l'activité pour permettre de revenir facilement
        }
    }
}
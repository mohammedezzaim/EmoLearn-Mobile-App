package com.example.gencont_app.score





import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.gencont_app.R
import com.example.gencont_app.configDB.sqlite.data.Question
import com.example.gencont_app.configDB.sqlite.data.Reponse
import com.example.gencont_app.configDB.sqlite.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SummaryActivity : AppCompatActivity() {

    private lateinit var listViewSummary: ListView
    private lateinit var btnBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_summary)

        val score = intent.getIntExtra("score", 0)
        val totalQuestions = intent.getIntExtra("totalQuestions", 0)
        val correctAnswers = intent.getIntExtra("correctAnswers", 0)
        val incorrectAnswers = intent.getIntExtra("incorrectAnswers", 0)
        val questionIds = intent.getLongArrayExtra("questionIds") ?: longArrayOf()
        val selectedAnswerIds = intent.getLongArrayExtra("selectedAnswerIds") ?: longArrayOf()

        // Initialiser les vues
        val txtSummaryTitle = findViewById<TextView>(R.id.txtSummaryTitle)
        listViewSummary = findViewById(R.id.listViewSummary)
        btnBack = findViewById(R.id.btnBack)

        txtSummaryTitle.text = "Quiz Summary: $correctAnswers correct out of $totalQuestions"

        // Charger les questions et réponses depuis la base de données
        loadQuestionSummary(questionIds, selectedAnswerIds)

        btnBack.setOnClickListener {
            finish() // Retourner à l'écran précédent
        }
    }

    private fun loadQuestionSummary(questionIds: LongArray, selectedAnswerIds: LongArray) {
        val db = AppDatabase.getInstance(this)

        CoroutineScope(Dispatchers.IO).launch {
            val summaryItems = mutableListOf<QuestionSummaryItem>()

            for (i in questionIds.indices) {
                val questionId = questionIds[i]
                val selectedAnswerId = if (i < selectedAnswerIds.size) selectedAnswerIds[i] else -1L

                val question = db.questionDao().getQuestionById(questionId)
                val responses = db.reponseDao().getReponsesByQuestion(questionId)

                val selectedResponse = responses.find { it.id == selectedAnswerId }
                val correctResponse = responses.find { it.status == "correct" }

                question?.let {
                    summaryItems.add(
                        QuestionSummaryItem(
                            question = it,
                            responses = responses,
                            selectedResponse = selectedResponse,
                            correctResponse = correctResponse,
                            isCorrect = selectedResponse?.status == "correct"
                        )
                    )
                }
            }

            withContext(Dispatchers.Main) {
                val adapter = QuestionSummaryAdapter(this@SummaryActivity, summaryItems)
                listViewSummary.adapter = adapter
            }
        }
    }

    data class QuestionSummaryItem(
        val question: Question,
        val responses: List<Reponse>,
        val selectedResponse: Reponse?,
        val correctResponse: Reponse?,
        val isCorrect: Boolean
    )

    inner class QuestionSummaryAdapter(
        private val context: AppCompatActivity,
        private val items: List<QuestionSummaryItem>
    ) : BaseAdapter() {

        private val inflater: LayoutInflater = LayoutInflater.from(context)

        override fun getCount(): Int = items.size

        override fun getItem(position: Int): QuestionSummaryItem = items[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: inflater.inflate(R.layout.item_quiz_summary, parent, false)
            val item = getItem(position)

            val txtQuestionNumber = view.findViewById<TextView>(R.id.txtQuestionNumber)
            val txtQuestionText = view.findViewById<TextView>(R.id.txtQuestionText)
            val txtSelectedAnswer = view.findViewById<TextView>(R.id.txtSelectedAnswer)
            val txtCorrectAnswer = view.findViewById<TextView>(R.id.txtCorrectAnswer)
            val txtStatus = view.findViewById<TextView>(R.id.txtStatus)

            txtQuestionNumber.text = "Question ${position + 1}"
            txtQuestionText.text = item.question.libelle

            txtSelectedAnswer.text = "Your answer: ${item.selectedResponse?.lib ?: "None"}"
            txtCorrectAnswer.text = "Correct answer: ${item.correctResponse?.lib ?: "None"}"

            if (item.isCorrect) {
                txtStatus.text = "Correct"
                txtStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
            } else {
                txtStatus.text = "Incorrect"
                txtStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
            }

            return view
        }
    }
}
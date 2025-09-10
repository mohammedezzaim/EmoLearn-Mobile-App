package com.example.gencont_app.quiz

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.example.gencont_app.R
import com.example.gencont_app.configDB.sqlite.data.Question
import com.example.gencont_app.configDB.sqlite.data.Reponse
import com.example.gencont_app.configDB.sqlite.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QuestionAdapter(
    private val context: Context,
    private val questions: List<Question>
) : BaseAdapter() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private val selectedAnswers = mutableMapOf<Int, Int>() // Position -> Index sélectionné
    private val questionResponses = mutableMapOf<Int, List<Reponse>>() // Position -> Liste de réponses

    override fun getCount(): Int = questions.size

    override fun getItem(position: Int): Question = questions[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.item_question, parent, false)
        val question = getItem(position)

        val questionText = view.findViewById<TextView>(R.id.question_text)
        val optionsGroup = view.findViewById<RadioGroup>(R.id.options_group)

        questionText.text = question.libelle
        optionsGroup.removeAllViews()

        if (question.id != -1L) {
            val db = AppDatabase.getInstance(context.applicationContext)
            CoroutineScope(Dispatchers.Main).launch {
                val reponses = db.reponseDao().getReponsesByQuestion(question.id)
                reponses?.let {
                    questionResponses[position] = it
                    for ((index, option) in it.withIndex()) {
                        val radioButton = RadioButton(context).apply {
                            text = option.lib
                            id = index
                        }
                        optionsGroup.addView(radioButton)
                    }
                }
            }
        }

        selectedAnswers[position]?.let { checkedId ->
            optionsGroup.check(checkedId)
        }

        optionsGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedAnswers[position] = checkedId
        }

        return view
    }

    fun getSelectedAnswers(): Map<Int, Int> = selectedAnswers.toMap()

    fun clearSelections() {
        selectedAnswers.clear()
        notifyDataSetChanged()
    }

    // Nouvelle méthode pour récupérer les IDs des questions
    fun getQuestionIds(): LongArray {
        return questions.map { it.id }.toLongArray()
    }

    // Nouvelle méthode pour récupérer les IDs des réponses sélectionnées
    fun getSelectedAnswerIds(): LongArray {
        val answerIds = mutableListOf<Long>()

        for (position in 0 until questions.size) {
            val selectedIndex = selectedAnswers[position] ?: -1
            if (selectedIndex >= 0) {
                val responses = questionResponses[position]
                if (responses != null && selectedIndex < responses.size) {
                    answerIds.add(responses[selectedIndex].id)
                } else {
                    answerIds.add(-1L)
                }
            } else {
                answerIds.add(-1L)
            }
        }

        return answerIds.toLongArray()
    }

    fun calculateScore(): Int {
        var score = 0
        selectedAnswers.forEach { (position, selectedIndex) ->
            questionResponses[position]?.let { responses ->
                if (selectedIndex in responses.indices) {
                    val selectedResponse = responses[selectedIndex]
                    if (selectedResponse.status == "correct") {
                        score++
                    }
                }
            }
        }
        return score
    }

    fun getTotalQuestions(): Int = questions.size

    fun calculateDetailedResult(): QuizResult {
        var correct = 0
        selectedAnswers.forEach { (position, selectedIndex) ->
            questionResponses[position]?.let { responses ->
                if (selectedIndex in responses.indices && responses[selectedIndex].status == "correct") {
                    correct++
                }
            }
        }
        val total = questions.size
        return QuizResult(
            score = (correct * 100) / total,
            totalQuestions = total,
            correctAnswers = correct,
            incorrectAnswers = total - correct
        )
    }
}
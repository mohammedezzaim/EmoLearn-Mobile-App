package com.example.gencont_app.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.example.gencont_app.R
import com.example.gencont_app.configDB.sqlite.data.*
import com.example.gencont_app.cours.ChapiterActivity
import com.example.gencont_app.quiz.QuizActivity

class LessonAdapter(
    private val context: Context,
    lessons: List<Cours>,                  // Liste originale
    private val onStartLessonClick: (Int) -> Unit,
    private val onQuizClick: (Int) -> Unit
) : BaseAdapter() {

    // On inverse la liste pour afficher le dernier en premier
    private val sortedLessons = lessons.sortedByDescending { it.id }

    // L'id du cours le plus récent (premier de sortedLessons)
    private val lastLessonId: Long? = sortedLessons.firstOrNull()?.id

    override fun getCount(): Int = sortedLessons.size
    override fun getItem(position: Int): Any = sortedLessons[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Inflate ou recycle la vue
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_lesson, parent, false)

        // Récupération du ConstraintLayout racine (pour changer son background)
        val rootLayout = view.findViewById<ConstraintLayout>(R.id.clLessonItem)

        val lesson = sortedLessons[position]

        // Texte
        view.findViewById<TextView>(R.id.tvLessonTitle).text = lesson.titre
        view.findViewById<TextView>(R.id.tvLessonDesc).text  = lesson.description

        // 🎨 Mise en évidence du cours le plus récent
        if (lesson.id == lastLessonId) {
            rootLayout.setBackgroundColor(
                ContextCompat.getColor(context, R.color.tag_color)
            )
        } else {
            rootLayout.setBackgroundColor(
                ContextCompat.getColor(context, android.R.color.white)
            )
        }

        // Bouton "Commencer"
        view.findViewById<Button>(R.id.btnStartLesson).setOnClickListener {
            onStartLessonClick(lesson.id.toInt())
            val intent = Intent(context, ChapiterActivity::class.java)

            // Ajouter les extras dans l'Intent
            intent.putExtra("cours_id", lesson.id)
            intent.putExtra("cours_titre", lesson.titre ?: "TItre de Cour")  // Utiliser une valeur par défaut pour titre si null

            // Lancer l'activité
            context.startActivity(intent)
        }




        return view
    }
}

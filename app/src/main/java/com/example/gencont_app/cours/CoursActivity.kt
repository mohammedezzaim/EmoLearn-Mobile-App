package com.example.gencont_app.cours


//import LessonAdapter
import android.content.Intent
//import LessonAdapter
import android.os.Bundle
import android.widget.ListView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.gencont_app.R
import com.example.gencont_app.adapter.LessonAdapter
//import com.example.gencont_app.adapter.LessonAdapter
import com.example.gencont_app.configDB.sqlite.database.*
import com.example.gencont_app.login.UserSessionManager
import kotlinx.coroutines.launch

class CoursActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_cours)

        val db = AppDatabase.getInstance(applicationContext)
        val coursDao = db.coursDao()

        val listView = findViewById<ListView>(R.id.lessonsListView)

        lifecycleScope.launch {
            val userId = UserSessionManager.getUserId(this@CoursActivity)
            if (userId != -1L) {
                val coursList = coursDao.getCoursByUtilisateur(userId)

                // Conversion de List<Cours> -> List<Lesson>
                val lessons = coursList

                listView.adapter = LessonAdapter(
                    context = this@CoursActivity,
                    lessons = lessons,
                    onStartLessonClick = { position ->
                        Toast.makeText(
                            this@CoursActivity,
                            "Lesson ${position + 1} clicked",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onQuizClick = { position ->
                        Toast.makeText(
                            this@CoursActivity,
                            "Quiz ${position + 1} clicked",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }
}
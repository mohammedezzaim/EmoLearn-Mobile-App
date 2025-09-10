package com.example.gencont_app.cours

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.gencont_app.R
import com.example.gencont_app.configDB.sqlite.database.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.content.Intent
import android.content.Context
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.example.gencont_app.quiz.QuizActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

class CourSectionActivity : AppCompatActivity() {
    private lateinit var youtubeWebView: WebView
    private lateinit var fabPlayVideo: FloatingActionButton

    private lateinit var titleTextView: MaterialTextView
    private lateinit var descTextView: MaterialTextView
    private lateinit var tvChapitreContent: MaterialTextView
    private lateinit var example: MaterialTextView
    private lateinit var btnQuiz: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_cour_section)

        youtubeWebView = findViewById(R.id.youtubeWebView)
        fabPlayVideo = findViewById(R.id.fabPlayVideo)

        // Configure WebView
        val webSettings: WebSettings = youtubeWebView.settings
        webSettings.javaScriptEnabled = true
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        webSettings.mediaPlaybackRequiresUserGesture = false

        youtubeWebView.webChromeClient = WebChromeClient()
        youtubeWebView.webViewClient = WebViewClient()

        titleTextView = findViewById(R.id.tvChapitreTitle)
        descTextView = findViewById(R.id.tvChapitreContent)
        tvChapitreContent = findViewById(R.id.tvChapitreContent)
        example = findViewById(R.id.example)
        btnQuiz = findViewById(R.id.btnStartQuiz)

        val sectionId = intent.getLongExtra("section_id", -1L)

        if (sectionId != -1L) {
            val db = AppDatabase.getInstance(applicationContext)
            CoroutineScope(Dispatchers.Main).launch {
                val section = db.sectionDao().getById(sectionId)
                section?.let {
                    titleTextView.text = it.titre
                    descTextView.text = "Ordre : ${it.numeroOrder}"
                    tvChapitreContent.text = it.contenu
                    example.text = it.exemple ?: "Date inconnue"

                    val videoUrl =  it.urlVideo
                    // Gestion du bouton play vidéo
                    fabPlayVideo.setOnClickListener {
                        if (videoUrl != null) {
                            val embedUrl = getYoutubeEmbedUrl(videoUrl)
                            if (embedUrl != null) {
                                youtubeWebView.loadUrl(embedUrl)
                                fabPlayVideo.visibility = View.GONE
                            } else {
                                Toast.makeText(this@CourSectionActivity, "URL vidéo invalide", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this@CourSectionActivity, "URL vidéo manquante", Toast.LENGTH_SHORT).show()
                        }
                    }


                    val id = it.id
                    btnQuiz.setOnClickListener {


                        Toast.makeText(this@CourSectionActivity, "Lesson ${id} clicked", Toast.LENGTH_SHORT).show()


                        val intent = Intent(this@CourSectionActivity, QuizActivity::class.java)
                        intent.putExtra("section_id", id)
                        startActivity(intent)
                    }
                }
            }
        }
    }


    fun getYoutubeEmbedUrl(url: String): String? {
        // Exemple simple : https://www.youtube.com/watch?v=0Q6JpqDBrrg
        val regex = "v=([\\w-]+)".toRegex()
        val matchResult = regex.find(url)
        val videoId = matchResult?.groups?.get(1)?.value
        return if (videoId != null) {
            "https://www.youtube.com/embed/$videoId"
        } else {
            null
        }
    }



}
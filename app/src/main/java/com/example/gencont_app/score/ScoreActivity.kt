package com.example.gencont_app.score

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.graphics.Bitmap
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.button.MaterialButton
import java.io.File
import java.io.FileOutputStream
import androidx.activity.enableEdgeToEdge
import com.example.gencont_app.R
import com.example.gencont_app.quiz.QuizActivity
import com.google.android.material.progressindicator.CircularProgressIndicator

class ScoreActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_score)

        val score = intent.getIntExtra("score", 0)
        val totalQuestions = intent.getIntExtra("totalQuestions", 0)
        val correctAnswers = intent.getIntExtra("correctAnswers", 0)
        val incorrectAnswers = intent.getIntExtra("incorrectAnswers", 0)
        val sectionId = intent.getLongExtra("section_id", -1L)

        // Sauvegarder les données de quiz pour pouvoir les passer à l'activité de résumé
        val questionIds = intent.getLongArrayExtra("questionIds") ?: longArrayOf()
        val selectedAnswerIds = intent.getLongArrayExtra("selectedAnswerIds") ?: longArrayOf()

        // Initialiser les vues
        val scoreText = findViewById<TextView>(R.id.scoreText)
        val scoreText2 = findViewById<TextView>(R.id.scoreText2)
        val barProgress = findViewById<CircularProgressIndicator>(R.id.progress)
        val correctNumber = findViewById<TextView>(R.id.correctNumber)
        val incorrectNumber = findViewById<TextView>(R.id.incorrectNumber)
        val btnViewSummary = findViewById<Button>(R.id.btnViewSummary)
        val btnRetakeQuiz = findViewById<Button>(R.id.btnRetakeQuiz)

        // Afficher les résultats
        scoreText.text = "${score}%"
        scoreText2.text = "${score}%"
        barProgress.setProgress(score, true)
        correctNumber.text = correctAnswers.toString()
        incorrectNumber.text = incorrectAnswers.toString()

        // Gestion des clics sur les boutons
        btnViewSummary.setOnClickListener {
            // Ouvrir l'activité de résumé de quiz
            val intent = Intent(this, SummaryActivity::class.java).apply {
                putExtra("score", score)
                putExtra("totalQuestions", totalQuestions)
                putExtra("correctAnswers", correctAnswers)
                putExtra("incorrectAnswers", incorrectAnswers)
                putExtra("questionIds", questionIds)
                putExtra("selectedAnswerIds", selectedAnswerIds)
            }
            startActivity(intent)
        }

        btnRetakeQuiz.setOnClickListener {
            // Retourner à l'activité QuizActivity avec le même sectionId
            if (sectionId != -1L) {
                val intent = Intent(this, QuizActivity::class.java).apply {
                    putExtra("section_id", sectionId)
                    // Flag pour indiquer que c'est une reprise de quiz
                    putExtra("retake", true)
                }
                startActivity(intent)
            }
            finish() // Fermer l'activité actuelle
        }

        setupShareButton()
    }

    private fun setupShareButton() {
        val shareButton = findViewById<MaterialButton>(R.id.shareButton)

        shareButton.setOnClickListener {
            shareResult()
        }
    }

    private fun shareResult() {
        // Get your content to share
        val contentToShare = "Check out my amazing result from AppName!"

        // You can also include a URL or additional text
        val url = "https://your-app-link.com"
        val shareText = "$contentToShare $url"

        // Take screenshot and share it along with the text
        val screenshotUri = takeScreenshot()

        // Create the share intent with both text and image
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)

            // Add the screenshot if available
            if (screenshotUri != null) {
                putExtra(Intent.EXTRA_STREAM, screenshotUri)
                type = "image/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "text/plain"
            }
        }

        // Show the share sheet
        try {
            startActivity(Intent.createChooser(shareIntent, "Share via"))
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to share content", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Takes a screenshot of the current activity and returns the URI of the saved image
     * @return Uri? The content URI of the saved screenshot, or null if failed
     */
    private fun takeScreenshot(): android.net.Uri? {
        try {
            // Get root view of the activity
            val rootView = window.decorView.rootView

            // Make it drawable
            rootView.isDrawingCacheEnabled = true
            val bitmap = Bitmap.createBitmap(rootView.drawingCache)
            rootView.isDrawingCacheEnabled = false

            return saveScreenshotToCache(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to capture screenshot", Toast.LENGTH_SHORT).show()
            return null
        }
    }

    /**
     * Saves the bitmap to the app's cache directory and returns its content URI
     */
    private fun saveScreenshotToCache(bitmap: Bitmap): android.net.Uri? {
        return try {
            // Create file in cache directory
            val fileName = "screenshot_${System.currentTimeMillis()}.jpg"
            val cachePath = File(cacheDir, "screenshots")
            cachePath.mkdirs()

            val stream = FileOutputStream("$cachePath/$fileName")
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.close()

            // Get the file
            val newFile = File(cachePath, fileName)

            // Create content URI using FileProvider
            FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                newFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
package com.example.gencont_app

import CoursePersister
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.gencont_app.aiintegration.service_video.VideoSuggestionManager
import com.example.gencont_app.aiintegration.service_video.model.VideoQueryRequest
import com.example.gencont_app.api.ChatApiClient
import com.example.gencont_app.configDB.firebase.repository.UtilisateurFirebaseRepository
import com.example.gencont_app.configDB.firebase.syncData.SyncData
import com.example.gencont_app.configDB.sqlite.data.Utilisateur
import com.example.gencont_app.configDB.sqlite.database.AppDatabase
import com.example.gencont_app.formulaire.FormulaireActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import com.example.gencont_app.login.LoginActivity
import com.example.gencont_app.register.RegisterActivity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Call
import okhttp3.Callback
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var utilisateurFirebaseRepository: UtilisateurFirebaseRepository

    override  fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // ensure the filename is activity_main.xml

        val db = AppDatabase.getInstance(applicationContext)
        val utilisateurDao = db.utilisateurDao()

//        this is a test
        runBlocking {
            launch {
                // Insert a Utilisateur
                val newUtilisateur = Utilisateur(
                    nom = "John",
                    prénom = "Doe",
                    email = "john.doe@example.com",
                    motDePasse = "password123"
                )
//                val utilisateurId = utilisateurDao.insert(newUtilisateur)
//                Log.d("DB_INIT", "Utilisateur inserted with ID: $utilisateurId")
            }
        }

// Par exemple dans onCreate ou après un clic de bouton :


        // to go directly to form (incomment this)
        /*  val intent = Intent(this, FormulaireActivity::class.java)
       startActivity(intent)*/

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        btnLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
//            ChatApiClient.sendMessage("Explique-moi comment utiliser TabLayout + ViewPager2 en Kotlin")

//            ChatApiClient.generateCourseJson(
//                titre       = "traitement d'image",
//                niveau      = "intermidiaire",
//                description = "comment en traite l image ",
//                emotion     = "happy"
//            ) { jsonCourse ->
//                lifecycleScope.launch(Dispatchers.IO) {
//                    val repo = CoursePersister(AppDatabase.getInstance(applicationContext))
//                    repo.saveCourse(jsonCourse, 2)
//                }
//            }


        }

        btnRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // how to add user local and firebase
        val firestore = FirebaseFirestore.getInstance()

        utilisateurFirebaseRepository = UtilisateurFirebaseRepository(utilisateurDao, firestore)

        // Insert a Utilisateur
        val nouvelUtilisateur = Utilisateur(
            nom = "Mohammed",
            prénom = "Ezzaim",
            email = "m@gmail.com",
            motDePasse = "password123"
        )

        runBlocking {
            launch {


                lifecycleScope.launch {
                    val id = utilisateurFirebaseRepository.insert(nouvelUtilisateur)
                    Log.d("InsertUtilisateur", "Utilisateur inséré avec l'ID : $id")
                }
            }
        }

        // Synchronisation des données
//        SyncData.syncFromFirebaseToLocal(this)
//        SyncData.syncFromLocalToFirebase(this)
    }
}

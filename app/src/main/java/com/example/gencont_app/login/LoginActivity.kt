package com.example.gencont_app.login

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import com.example.gencont_app.R
import com.example.gencont_app.configDB.firebase.syncData.SyncData
import com.example.gencont_app.configDB.sqlite.database.*
import com.example.gencont_app.register.RegisterActivity
import com.example.gencont_app.home.HomeActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class LoginActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: AppCompatButton
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvRegister: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etUsername       = findViewById(R.id.etUsername)
        etPassword       = findViewById(R.id.etPassword)
        btnLogin         = findViewById(R.id.btnLogin)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        tvRegister       = findViewById(R.id.tvRegister)

        btnLogin.setOnClickListener {
            val email    = etUsername.text.toString().trim()
            val password = etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                showToast("Veuillez remplir tous les champs")
                return@setOnClickListener
            }

            // start  service firebase
            // Synchronisation des données
            SyncData.syncFromFirebaseToLocal(this)

            // si le cas que firebase supprimer ou localmenet en les donner
            // SyncData.syncFromLocalToFirebase(this)

            // end  service firebase

//            startActivity(
//
//                Intent(this@LoginActivity, HomeActivity::class.java)
//            )
            loginUser(email, password) { success ->
                runOnUiThread {
                    if (success) {
                        showToast("Connexion réussie")
                        startActivity(
                            Intent(this@LoginActivity, HomeActivity::class.java)
                        )
                        finish()
                    } else {
                        showToast("Email ou mot de passe incorrect")
                    }
                }
            }
        }

        tvForgotPassword.setOnClickListener {
            // TODO: naviguer vers ForgotPasswordActivity ou afficher un dialogue
        }

        tvRegister.setOnClickListener {
            startActivity(
                Intent(this@LoginActivity, RegisterActivity::class.java)
            )
        }
    }

    /**
     * Vérifie les identifiants en base et renvoie success=true ou false via onResult
     */
    private fun loginUser(
        email: String,
        password: String,
        onResult: (isSuccess: Boolean) -> Unit
    ) {
        val db = AppDatabase.getInstance(this)

//        lifecycleScope.launch {
//            val utilisateur = withContext(Dispatchers.IO) {
//                db.utilisateurDao().getUtilisateurByEmail(email)
//            }
//
//            if (utilisateur == null) {
//                onResult(false)
//                return@launch
//            }
//
//            val motDePasse = utilisateur.motDePasse
//            if (motDePasse.isNullOrEmpty() || !motDePasse.contains(":")) {
//                onResult(false)
//                return@launch
//            }
//
//            val parts = motDePasse.split(":")
//            if (parts.size != 2) {
//                onResult(false)
//                return@launch
//            }
//
//            val salt       = parts[0]
//            val storedHash = parts[1]
//            val inputHash  = hashPassword(password, salt)
//
//            onResult(storedHash == inputHash)
//        }

        lifecycleScope.launch {
            val utilisateur = withContext(Dispatchers.IO) {
                db.utilisateurDao().getUtilisateurByEmail(email)
            }

            if (utilisateur == null) {
                onResult(false)
                return@launch
            }

            val motDePasse = utilisateur.motDePasse
            if (motDePasse.isNullOrEmpty() || !motDePasse.contains(":")) {
                onResult(false)
                return@launch
            }

            val parts = motDePasse.split(":")
            if (parts.size != 2) {
                onResult(false)
                return@launch
            }

            val salt = parts[0]
            val storedHash = parts[1]
            val inputHash = hashPassword(password, salt)

            if (storedHash == inputHash) {
                // Sauvegarde de l'ID utilisateur
                UserSessionManager.saveUserId(this@LoginActivity, utilisateur.id)
                onResult(true)
            } else {
                onResult(false)
            }
        }


    }

    /**
     * Reproduit le même algorithme PBKDF2 que pour l'enregistrement
     */
    private fun hashPassword(password: String, salt: String): String {
        val saltBytes = android.util.Base64.decode(salt, android.util.Base64.NO_WRAP)

        val spec = PBEKeySpec(
            password.toCharArray(),
            saltBytes,
            120_000,
            256
        )
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash    = factory.generateSecret(spec).encoded

        return android.util.Base64.encodeToString(hash, android.util.Base64.NO_WRAP)
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}
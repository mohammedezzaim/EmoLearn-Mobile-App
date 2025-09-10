package com.example.gencont_app.register

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.gencont_app.R
import com.example.gencont_app.configDB.firebase.repository.QuestionFirebaseRepository
import com.example.gencont_app.configDB.firebase.repository.UtilisateurFirebaseRepository
import com.example.gencont_app.configDB.sqlite.data.Utilisateur
import com.example.gencont_app.configDB.sqlite.database.AppDatabase

import com.example.gencont_app.login.LoginActivity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class RegisterActivity : AppCompatActivity() {

    private lateinit var editTextNom: EditText
    private lateinit var editTextPrenom: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonRegister: Button

    // Constantes pour le hashage du mot de passe
    companion object {
        private const val SALT_BYTES = 32
        private const val PBKDF2_ITERATIONS = 120000
        private const val KEY_LENGTH = 256
    }

    val firestore = FirebaseFirestore.getInstance()
    private lateinit var utilisateurFirebaseRepository: UtilisateurFirebaseRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize views
        editTextNom = findViewById(R.id.idNom)
        editTextPrenom = findViewById(R.id.idPrenom)
        editTextEmail = findViewById(R.id.idEmail)
        editTextPassword = findViewById(R.id.idPassword)
        buttonRegister = findViewById(R.id.btnSignUp)

        // Setup click listener
        buttonRegister.setOnClickListener {
            // Récupération des données du formulaire
            val nom = editTextNom.text.toString().trim()
            val prenom = editTextPrenom.text.toString().trim()
            val email = editTextEmail.text.toString().trim()
            val password = editTextPassword.text.toString()

            // Tentative d'inscription
            registerUser(nom, prenom, email, password)
        }
    }

    private fun registerUser(nom: String, prenom: String, email: String, password: String) {
        // Validation des entrées
        if (!validateInputs(nom, prenom, email, password)) {
            return
        }

        // Traitement de l'inscription dans un coroutine
        lifecycleScope.launch {
            try {
                // Récupération de la base de données
                val db = AppDatabase.getInstance(this@RegisterActivity)

                // Vérifier si l'email existe déjà
                val existingUser = withContext(Dispatchers.IO) {
                    db.utilisateurDao().getUtilisateurByEmail(email)
                }

                if (existingUser != null) {
                    showToast("Cet email est déjà utilisé")
                    return@launch
                }

                // Génération du salt et hashage du mot de passe
                val salt = generateSalt()
                val hashedPassword = hashPassword(password, salt)

                // Création de l'utilisateur avec le mot de passe hashé (salt inclus dans le hash)
                val utilisateur = Utilisateur(
                    nom = nom,
                    prénom = prenom,
                    email = email,
                    motDePasse = "$salt:$hashedPassword" // Format: "salt:hash" pour stockage
                )

                // Sauvegarde de l'utilisateur dans la base de données
                withContext(Dispatchers.IO) {
                    UtilisateurFirebaseRepository(db.utilisateurDao(),firestore).insert(utilisateur)
                }

                // Notifier l'utilisateur et terminer l'activité
                showToast("Inscription réussie")
                val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                startActivity(intent)

            } catch (e: Exception) {
                showToast("Erreur lors de l'inscription: ${e.message}")
            }
        }
    }

    /**
     * Valide les entrées du formulaire
     */
    private fun validateInputs(nom: String, prenom: String, email: String, password: String): Boolean {
        // Validation du nom
        if (nom.isEmpty()) {
            showToast("Le nom est requis")
            return false
        }

        // Validation du prénom
        if (prenom.isEmpty()) {
            showToast("Le prénom est requis")
            return false
        }

        // Validation de l'email
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast("Adresse email invalide")
            return false
        }

        // Validation du mot de passe
        if (password.length < 8) {
            showToast("Le mot de passe doit contenir au moins 8 caractères")
            return false
        }

        if (!isStrongPassword(password)) {
            showToast("Le mot de passe doit contenir au moins une majuscule, une minuscule, un chiffre et un caractère spécial")
            return false
        }

        return true
    }

    /**
     * Vérifie si le mot de passe est fort
     */
    private fun isStrongPassword(password: String): Boolean {
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }

        return hasUppercase && hasLowercase && hasDigit && hasSpecial
    }

    /**
     * Génère un salt aléatoire pour le hashage
     */
    private fun generateSalt(): String {
        val salt = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    /**
     * Hashe le mot de passe avec PBKDF2
     */
    private fun hashPassword(password: String, salt: String): String {
        val saltBytes = Base64.decode(salt, Base64.NO_WRAP)

        val spec = PBEKeySpec(
            password.toCharArray(),
            saltBytes,
            PBKDF2_ITERATIONS,
            KEY_LENGTH
        )

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded

        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    /**
     * Affiche un message Toast
     */
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
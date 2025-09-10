package com.example.gencont_app.configDB.firebase.repository

import android.util.Log

import com.example.gencont_app.configDB.sqlite.dao.CoursDao
import com.example.gencont_app.configDB.sqlite.data.Cours
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CoursFirebaseRepository(
     val dao: CoursDao,
    private val firestore: FirebaseFirestore
) {

    private val collection = firestore.collection("cours")

    suspend fun syncFromFirebaseToLocal(): Boolean {
        val snapshot = collection.get().await()
        var hasChanges = false

        for (doc in snapshot.documents) {
            val cours = doc.toObject(Cours::class.java)
            cours?.let {
                try {
                    val localCours = dao.getCoursById(it.id)

                    if (localCours == null) {
                        // Si le cours n'existe pas dans la base locale, on l'insère
                        dao.insert(it)
                        Log.d("SYNC", "Cours inséré depuis Firestore: ${it.titre}")
                        hasChanges = true
                    } else if (!localCours.equalsIgnoreFields(it)) {
                        // Si le cours existe mais a des différences, on le met à jour
                        dao.update(it)
                        Log.d("SYNC", "Cours mis à jour depuis Firestore: ${it.titre}")
                        hasChanges = true
                    } else {

                    }

                } catch (e: Exception) {
                    Log.e("SYNC_ERROR", "Erreur d'insertion locale: ${e.message}")
                }
            }
        }

        return hasChanges
    }

    suspend fun syncFromLocalToFirebase() {
        try {
            val coursList = dao.getAllCours()
            for (cours in coursList) {
                val documentRef = collection.document(cours.id.toString())
                val documentSnapshot = documentRef.get().await()

                if (!documentSnapshot.exists()) {
                    documentRef.set(cours).await()
                    Log.d("SYNC_FIREBASE", "Cours envoyé: ${cours.titre}")
                } else {
                    Log.d("SYNC_FIREBASE", "Le cours avec l'ID ${cours.id} existe déjà.")
                }
            }
        } catch (e: Exception) {
            Log.e("SYNC_FIREBASE_ERR", "Erreur d'envoi vers Firestore: ${e.message}")
        }
    }



    suspend fun insert(cours: Cours): Long {
        val id = dao.insert(cours)
        cours.id = id
        collection.document(id.toString()).set(cours).await()
        return id
    }

    suspend fun updateCoursById(id: Long, updatedCours: Cours) {
        val existingCours = dao.getCoursById(id)
        if (existingCours != null) {
            dao.update(updatedCours.copy(id = id))
            collection.document(id.toString()).set(updatedCours.copy(id = id)).await()
        }
    }

    suspend fun deleteCoursById(id: Long) {
        val existingCours = dao.getCoursById(id)
        if (existingCours != null) {
            dao.delete(existingCours)
            collection.document(id.toString()).delete().await()
        }
    }

    fun listenForRemoteUpdates(onChange: (Cours) -> Unit) {
        collection.addSnapshotListener { snapshots, _ ->
            snapshots?.documentChanges?.forEach { change ->
                val cours = change.document.toObject(Cours::class.java)
                when (change.type) {
                    com.google.firebase.firestore.DocumentChange.Type.ADDED,
                    com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                        onChange(cours)
                        // Synchronisation locale automatique
                        CoroutineScope(Dispatchers.IO).launch {
                            val local = dao.getCoursById(cours.id)
                            if (local == null) dao.insert(cours)
                            else dao.update(cours)
                        }
                    }

                    com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                        CoroutineScope(Dispatchers.IO).launch {
                            dao.delete(cours)
                        }
                    }
                }
            }
        }
    }

    suspend fun getCoursByIdFromFirebase(id: Long): Cours? {
        val doc = collection.document(id.toString()).get().await()
        return if (doc.exists()) doc.toObject(Cours::class.java) else null
    }
}

package com.example.gencont_app.configDB.firebase.repository

import android.util.Log
import com.example.gencont_app.configDB.sqlite.dao.QuizDao
import com.example.gencont_app.configDB.sqlite.data.Quiz
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class QuizFirebaseRepository(
    val dao: QuizDao,
    private val firestore: FirebaseFirestore
) {
    private val collection = firestore.collection("Quiz")

    suspend fun syncFromFirebaseToLocal(): Boolean {
        val snapshot = collection.get().await()
        var hasChanges = false

        for (doc in snapshot.documents) {
            val item = doc.toObject(Quiz::class.java)
            item?.let {
                val local = dao.getQuizById(it.id)
                if (local == null) {
                    dao.insert(it)
                    hasChanges = true
                } else  {
                    if (!local.equalsIgnoreFields(it)) {
                        dao.update(it)
                        hasChanges = true
                    }
                }
            }
        }

        return hasChanges
    }


    suspend fun syncFromLocalToFirebase() {
        try {
            val list = dao.getAllQuizzes()
            for (item in list) {
                val documentRef = collection.document(item.id.toString())
                val documentSnapshot = documentRef.get().await()

                if (!documentSnapshot.exists()) {
                    documentRef.set(item).await()
                    Log.d("SYNC_FIREBASE", "Quiz avec l'ID ${item.id} inséré.")
                } else {
                    Log.d("SYNC_FIREBASE", "Le document avec l'ID ${item.id} existe déjà.")
                }
            }
        } catch (e: Exception) {
            Log.e("SYNC_FIREBASE_ERR", "Erreur de synchronisation Firestore: ${e.message}")
        }
    }

    suspend fun insert(item: Quiz) : Long{
        val id = dao.insert(item)
        item.id = id
        collection.document(id.toString()).set(item).await()
        return id
    }

    suspend fun updateById(id: Long, item: Quiz) {
        if (dao.getQuizById(id) != null) {
            dao.update(item.copy(id = id))
            collection.document(id.toString()).set(item.copy(id = id)).await()
        }
    }

    suspend fun deleteById(id: Long) {
        dao.getQuizById(id)?.let {
            dao.delete(it)
            collection.document(id.toString()).delete().await()
        }
    }

    fun listenForRemoteUpdates(onChange: (Quiz) -> Unit) {
        collection.addSnapshotListener { snapshots, _ ->
            snapshots?.documentChanges?.forEach { change ->
                val quiz = change.document.toObject(Quiz::class.java)
                CoroutineScope(Dispatchers.IO).launch {
                    val local = dao.getQuizById(quiz.id)
                    when (change.type) {
                        com.google.firebase.firestore.DocumentChange.Type.ADDED,
                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                            if (local == null) dao.insert(quiz) else dao.update(quiz)
                            onChange(quiz)
                        }
                        com.google.firebase.firestore.DocumentChange.Type.REMOVED -> dao.delete(quiz)
                    }
                }
            }
        }
    }
}

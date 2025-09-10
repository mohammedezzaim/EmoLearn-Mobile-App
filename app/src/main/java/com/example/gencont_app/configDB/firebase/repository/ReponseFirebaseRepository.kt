package com.example.gencont_app.configDB.firebase.repository

import android.util.Log
import com.example.gencont_app.configDB.sqlite.dao.ReponseDao
import com.example.gencont_app.configDB.sqlite.data.Reponse
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class ReponseFirebaseRepository(
    val dao: ReponseDao,
    private val firestore: FirebaseFirestore
) {
    private val collection = firestore.collection("reponses")

    suspend fun syncFromFirebaseToLocal(): Boolean {
        val snapshot = collection.get().await()
        var hasChanges = false

        for (doc in snapshot.documents) {
            val item = doc.toObject(Reponse::class.java)
            item?.let {
                val local = dao.getReponseById(it.id)

                if (local == null) {
                    dao.insert(it)
                    hasChanges = true
                } else {
                    dao.update(it)
                    hasChanges = true
                }
            }
        }

        return hasChanges
    }


    suspend fun syncFromLocalToFirebase() {
        try {
            val list = dao.getAllReponses()
            for (item in list) {
                val documentRef = collection.document(item.id.toString())
                val documentSnapshot = documentRef.get().await()

                if (!documentSnapshot.exists()) {
                    documentRef.set(item).await()
                    Log.d("SYNC_FIREBASE", "Réponse avec l'ID ${item.id} insérée.")
                } else {
                    Log.d("SYNC_FIREBASE", "Le document avec l'ID ${item.id} existe déjà.")
                }
            }
        } catch (e: Exception) {
            Log.e("SYNC_FIREBASE_ERR", "Erreur de synchronisation Firestore: ${e.message}")
        }
    }

    suspend fun insert(item: Reponse) : Long{
        val id = dao.insert(item)
        collection.document(id.toString()).set(item).await()
        return id
    }

    suspend fun updateById(id: Long, item: Reponse) {
        if (dao.getReponseById(id) != null) {
            dao.update(item.copy(id = id))
            collection.document(id.toString()).set(item.copy(id = id)).await()
        }
    }

    suspend fun deleteById(id: Long) {
        dao.getReponseById(id)?.let {
            dao.delete(it)
            collection.document(id.toString()).delete().await()
        }
    }

    fun listenForRemoteUpdates(onChange: (Reponse) -> Unit) {
        collection.addSnapshotListener { snapshots, _ ->
            snapshots?.documentChanges?.forEach { change ->
                val reponse = change.document.toObject(Reponse::class.java)
                CoroutineScope(Dispatchers.IO).launch {
                    val local = dao.getReponseById(reponse.id)
                    when (change.type) {
                        com.google.firebase.firestore.DocumentChange.Type.ADDED,
                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                            if (local == null) dao.insert(reponse) else dao.update(reponse)
                            onChange(reponse)
                        }
                        com.google.firebase.firestore.DocumentChange.Type.REMOVED -> dao.delete(reponse)
                    }
                }
            }
        }
    }
}

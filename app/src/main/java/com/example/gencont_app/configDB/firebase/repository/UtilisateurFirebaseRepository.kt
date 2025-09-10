package com.example.gencont_app.configDB.firebase.repository

import com.example.gencont_app.configDB.sqlite.dao.UtilisateurDao
import com.example.gencont_app.configDB.sqlite.data.Utilisateur
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class UtilisateurFirebaseRepository(
    val dao: UtilisateurDao,
    private val firestore: FirebaseFirestore
) {
    private val collection = firestore.collection("utilisateurs")

    suspend fun syncFromFirebaseToLocal(): Boolean {
        val snapshot = collection.get().await()
        var hasChanges = false

        for (doc in snapshot.documents) {
            val item = doc.toObject(Utilisateur::class.java)
            item?.let {
                val local = dao.getUtilisateurById(it.id)

                if (local == null) {
                    dao.insert(it)
                    hasChanges = true
                } else if (!local.equalsIgnoreFields(it)) {
                    dao.update(it)
                    hasChanges = true
                }
            }
        }

        return hasChanges
    }


    suspend fun syncFromLocalToFirebase() {
        val list = dao.getAllUtilisateurs()
        for (item in list) {
            val itemRef = collection.document(item.id.toString())
            val documentSnapshot = itemRef.get().await()

            if (!documentSnapshot.exists()) {
                itemRef.set(item).await()
            } else {
                println("Le document avec l'ID ${item.id} existe déjà.")
            }
        }
    }


    suspend fun insert(item: Utilisateur): Long {
        val id = dao.insert(item)
        item.id = id
        collection.document(id.toString()).set(item).await()
        return id
    }

    suspend fun updateById(id: Long, item: Utilisateur) {
        if (dao.getUtilisateurById(id) != null) {
            dao.update(item.copy(id = id))
            collection.document(id.toString()).set(item.copy(id = id)).await()
        }
    }

    suspend fun deleteById(id: Long) {
        dao.getUtilisateurById(id)?.let {
            dao.delete(it)
            collection.document(id.toString()).delete().await()
        }
    }

    fun listenForRemoteUpdates(onChange: (Utilisateur) -> Unit) {
        collection.addSnapshotListener { snapshots, _ ->
            snapshots?.documentChanges?.forEach { change ->
                val user = change.document.toObject(Utilisateur::class.java)
                CoroutineScope(Dispatchers.IO).launch {
                    val local = dao.getUtilisateurById(user.id)
                    when (change.type) {
                        com.google.firebase.firestore.DocumentChange.Type.ADDED,
                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                            if (local == null) dao.insert(user) else dao.update(user)
                            onChange(user)
                        }
                        com.google.firebase.firestore.DocumentChange.Type.REMOVED -> dao.delete(user)
                    }
                }
            }
        }
    }
}

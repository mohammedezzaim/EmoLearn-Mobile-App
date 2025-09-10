package com.example.gencont_app.configDB.firebase.repository

import android.util.Log
import com.example.gencont_app.configDB.sqlite.dao.SectionDao
import com.example.gencont_app.configDB.sqlite.data.Section
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SectionFirebaseRepository(
    private val dao: SectionDao,
    private val firestore: FirebaseFirestore
) {

    private val collection = firestore.collection("sections")

    suspend fun syncFromFirebaseToLocal(): Boolean {
        val snapshot = collection.get().await()
        var hasChanges = false

        for (doc in snapshot.documents) {
            val section = doc.toObject(Section::class.java)
            section?.let {
                try {
                    val local = dao.getSectionById(it.id)

                    // Vérification si la section existe déjà dans la base locale
                    if (local == null) {
                        // Si la section n'existe pas, on l'insère
                        dao.insert(it)
                        hasChanges = true
                    } else if (!local.equalsIgnoreFields(it)) {

                        dao.update(it)
                        hasChanges = true
                    } else {

                    }

                } catch (e: Exception) {
                    Log.e("SYNC_ERROR", "Section error: ${e.message}")
                }
            }
        }

        return hasChanges
    }


    suspend fun syncFromLocalToFirebase() {
        try {
            val list = dao.getAllSections()
            for (item in list) {
                val documentRef = collection.document(item.id.toString())
                val documentSnapshot = documentRef.get().await()

                if (!documentSnapshot.exists()) {
                    documentRef.set(item).await()
                    Log.d("SYNC_FIREBASE", "Document avec l'ID ${item.id} inséré.")
                } else {
                    Log.d("SYNC_FIREBASE", "Le document avec l'ID ${item.id} existe déjà.")
                }
            }
        } catch (e: Exception) {
            Log.e("SYNC_FIREBASE_ERR", "Erreur de synchronisation Firestore: ${e.message}")
        }
    }


    suspend fun insert(section: Section): Long {
        val id = dao.insert(section)
        section.id = id
        collection.document(id.toString()).set(section).await()
        return id
    }

    suspend fun updateById(id: Long, updated: Section) {
        val existing = dao.getSectionById(id)
        if (existing != null) {
            dao.update(updated.copy(id = id))
            collection.document(id.toString()).set(updated.copy(id = id)).await()
        }
    }

    suspend fun deleteById(id: Long) {
        val existing = dao.getSectionById(id)
        if (existing != null) {
            dao.delete(existing)
            collection.document(id.toString()).delete().await()
        }
    }

    fun listenForRemoteUpdates(onChange: (Section) -> Unit) {
        collection.addSnapshotListener { snapshots, _ ->
            snapshots?.documentChanges?.forEach { change ->
                val item = change.document.toObject(Section::class.java)
                CoroutineScope(Dispatchers.IO).launch {
                    val local = dao.getSectionById(item.id)
                    when (change.type) {
                        com.google.firebase.firestore.DocumentChange.Type.ADDED,
                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                            if (local == null) dao.insert(item) else dao.update(item)
                            onChange(item)
                        }

                        com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                            dao.delete(item)
                        }
                    }
                }
            }
        }
    }

    suspend fun getById(id: Long): Section? {
        val doc = collection.document(id.toString()).get().await()
        return if (doc.exists()) doc.toObject(Section::class.java) else null
    }
}

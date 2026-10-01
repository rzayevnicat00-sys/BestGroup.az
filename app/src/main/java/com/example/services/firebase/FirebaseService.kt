package com.example.services.firebase

import android.content.Context
import com.example.R
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class FirebaseService(private val context: Context) {

    val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    val firestore: FirebaseFirestore by lazy {
        try {
            val databaseId = context.getString(R.string.firestore_database_id)
            if (databaseId.isNotBlank()) {
                val app = FirebaseApp.getInstance()
                FirebaseFirestore.getInstance(app, databaseId)
            } else {
                FirebaseFirestore.getInstance()
            }
        } catch (e: Exception) {
            FirebaseFirestore.getInstance()
        }
    }

    val storage: FirebaseStorage by lazy {
        try {
            FirebaseStorage.getInstance()
        } catch (e: Exception) {
            FirebaseStorage.getInstance()
        }
    }
}

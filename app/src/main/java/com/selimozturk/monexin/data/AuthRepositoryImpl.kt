package com.selimozturk.monexin.data

import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.selimozturk.monexin.utils.Resource
import com.selimozturk.monexin.utils.await
import com.selimozturk.monexin.utils.convertToLongTime
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firebaseFirestore: FirebaseFirestore
) : AuthRepository {

    override suspend fun login(email: String, password: String): Resource<FirebaseUser> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("User is null")
            Resource.Success(user)
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Failure(e)
        }
    }

    override suspend fun register(
        name: String,
        email: String,
        password: String
    ): Resource<FirebaseUser> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("User is null")
            user.updateProfile(
                UserProfileChangeRequest.Builder().setDisplayName(name).build()
            ).await()
            val userMap = mutableMapOf<String, String>().apply {
                put("accountNumber", user.uid)
                put("joinedDate", user.metadata?.creationTimestamp?.toString()?.convertToLongTime() ?: "")
                put("name", name)
                put("email", email)
                put("device", "${Build.MANUFACTURER} ${Build.MODEL}")
            }
            firebaseFirestore.collection("users").document(user.uid).set(userMap).await()
            Resource.Success(user)
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Failure(e)
        }
    }

    override fun signOut() {
        firebaseAuth.signOut()
    }

}
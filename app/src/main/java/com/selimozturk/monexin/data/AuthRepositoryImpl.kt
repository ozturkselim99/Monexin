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
            Resource.Success(result.user!!)
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
            result.user?.updateProfile(
                UserProfileChangeRequest.Builder().setDisplayName(name).build()
            )?.await()
            val userMap = HashMap<String, String>()
            userMap["accountNumber"] = result.user?.uid!!
            userMap["joinedDate"] =
                result.user?.metadata?.creationTimestamp.toString().convertToLongTime()
            userMap["name"] = name
            userMap["email"] = email
            userMap["device"] = Build.MANUFACTURER + " " + Build.MODEL
            firebaseFirestore.collection("users").document(result.user?.uid!!).set(userMap).await()
            Resource.Success(result.user!!)
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Failure(e)
        }
    }

    override fun signOut() {
        firebaseAuth.signOut()
    }

}
package com.selimozturk.monexin.data

import com.google.firebase.auth.FirebaseUser
import com.selimozturk.monexin.utils.Resource

interface AuthRepository {
    suspend fun login(email: String, password: String): Resource<FirebaseUser>
    suspend fun register(name: String, email: String, password: String): Resource<FirebaseUser>
    fun signOut()
}
package com.selimozturk.monexin.ui.auth.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.selimozturk.monexin.data.AuthRepository
import com.selimozturk.monexin.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _loginState = MutableLiveData<Resource<FirebaseUser>?>(null)
    val loginState: LiveData<Resource<FirebaseUser>?> = _loginState

    private val _registerState = MutableLiveData<Resource<FirebaseUser>?>(null)
    val registerState: LiveData<Resource<FirebaseUser>?> = _registerState

    fun login(email: String, password: String) = viewModelScope.launch {
        _loginState.value = Resource.Loading
        _loginState.value = repository.login(email, password)
    }

    fun register(name: String, email: String, password: String) = viewModelScope.launch {
        _registerState.value = Resource.Loading
        _registerState.value = repository.register(name, email, password)
    }

    fun signOut() {
        repository.signOut()
        _loginState.value = null
        _registerState.value = null
    }

}
package com.selimozturk.monexin.ui.fragments.profile.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selimozturk.monexin.data.FirebaseRepository
import com.selimozturk.monexin.model.UserInfo
import com.selimozturk.monexin.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: FirebaseRepository
):ViewModel() {

    private val _profileState = MutableLiveData<Resource<UserInfo>?>(null)
    val profileState: LiveData<Resource<UserInfo>?> = _profileState

    fun getProfileInfo() = viewModelScope.launch {
        _profileState.value= Resource.Loading
        _profileState.value=repository.getProfileInfo()
    }

    fun clearAllTransaction()=viewModelScope.launch {
        repository.clearAllTransaction()
    }
}
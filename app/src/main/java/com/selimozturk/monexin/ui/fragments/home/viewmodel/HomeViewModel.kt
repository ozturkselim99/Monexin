package com.selimozturk.monexin.ui.fragments.home.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selimozturk.monexin.data.FirebaseRepository
import com.selimozturk.monexin.model.HomeInfo
import com.selimozturk.monexin.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: FirebaseRepository
): ViewModel() {

    private val _homeInfoState = MutableLiveData<Resource<HomeInfo>>(null)
    val homeInfoState : LiveData<Resource<HomeInfo>> = _homeInfoState

    fun getHomeInfo(minDate:String?,maxDate:String?)=viewModelScope.launch {
        _homeInfoState.value= Resource.Loading
        _homeInfoState.value=repository.getHomeInfo(minDate,maxDate)
    }
}
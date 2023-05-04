package com.selimozturk.monexin.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
) : ViewModel() {

    private val _isExpensesReselected = MutableLiveData(Unit)
    val isExpensesReselected: LiveData<Unit> = _isExpensesReselected

    private val _isIncomesReselected = MutableLiveData(Unit)
    val isIncomesReselected: LiveData<Unit> = _isIncomesReselected

    private val _isProfileReselected = MutableLiveData(Unit)
    val isProfileReselected: LiveData<Unit> = _isProfileReselected

    fun expensesReselected() = viewModelScope.launch {
        _isExpensesReselected.value = Unit
    }

    fun incomesReselected() = viewModelScope.launch {
        _isIncomesReselected.value = Unit
    }

    fun profileReselected() = viewModelScope.launch {
        _isProfileReselected.value = Unit
    }

}
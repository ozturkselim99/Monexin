package com.selimozturk.monexin.ui.fragments.incomes.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selimozturk.monexin.data.FirebaseRepository
import com.selimozturk.monexin.model.IncomesInfo
import com.selimozturk.monexin.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IncomesViewModel @Inject constructor(
    private val repository: FirebaseRepository
): ViewModel() {

    private val _incomesState = MutableLiveData<Resource<IncomesInfo>>(null)
    val incomesState : LiveData<Resource<IncomesInfo>> = _incomesState

    fun getIncomes()=viewModelScope.launch {
        _incomesState.value= Resource.Loading
        _incomesState.value=repository.getIncomes()
    }
}
package com.selimozturk.monexin.ui.fragments.expenses.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selimozturk.monexin.data.FirebaseRepository
import com.selimozturk.monexin.model.ExpensesInfo
import com.selimozturk.monexin.model.FilterModel
import com.selimozturk.monexin.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val repository: FirebaseRepository
): ViewModel() {

    private val _expensesState = MutableLiveData<Resource<ExpensesInfo>>(null)
    val expensesState : LiveData<Resource<ExpensesInfo>> = _expensesState

    fun getExpenses(filterModel: FilterModel?) = viewModelScope.launch {
        _expensesState.value = Resource.Loading
        _expensesState.value = repository.getExpenses(filterModel)
    }

}
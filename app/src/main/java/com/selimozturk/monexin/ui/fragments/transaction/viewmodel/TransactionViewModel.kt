package com.selimozturk.monexin.ui.fragments.transaction.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selimozturk.monexin.data.FirebaseRepository
import com.selimozturk.monexin.model.Transactions
import com.selimozturk.monexin.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val repository: FirebaseRepository
) : ViewModel() {

    private val _transactionAddState = MutableLiveData<Resource<String>?>(null)
    val transactionAddState: LiveData<Resource<String>?> = _transactionAddState

    private val _transactionUpdateState = MutableLiveData<Resource<String>?>(null)
    val transactionUpdateState: LiveData<Resource<String>?> = _transactionUpdateState

    private val _transactionImageState = MutableLiveData<Resource<Uri>>(null)
    val transactionImageState: LiveData<Resource<Uri>?> = _transactionImageState

    fun addTransaction(transactions: Transactions, photoUri: Uri?) = viewModelScope.launch {
        _transactionAddState.value = Resource.Loading
        _transactionAddState.value = repository.addTransaction(transactions, photoUri)
    }

    fun updateTransaction(transaction: Transactions, photoUri: Uri?) = viewModelScope.launch {
        _transactionUpdateState.value = Resource.Loading
        _transactionUpdateState.value=repository.updateTransaction(transaction, photoUri)
    }

    fun downloadTransactionImage(url: String?) = viewModelScope.launch {
        _transactionImageState.value = Resource.Loading
        _transactionImageState.value = repository.downloadTransactionImage(url)
    }
}
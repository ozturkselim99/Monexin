package com.selimozturk.monexin.ui.fragments.transaction_detail.viewmodel

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
class TransactionDetailViewModel @Inject constructor(
    private val repository: FirebaseRepository
): ViewModel() {

    private val _transactionImageState = MutableLiveData<Resource<Uri>>(null)
    val transactionImageState: LiveData<Resource<Uri>?> = _transactionImageState

    fun deleteTransaction(transaction: Transactions) = viewModelScope.launch {
        repository.deleteTransaction(transaction)
    }

    fun downloadTransactionImage(url:String?)=viewModelScope.launch {
        _transactionImageState.value=Resource.Loading
        _transactionImageState.value=repository.downloadTransactionImage(url)
    }

}
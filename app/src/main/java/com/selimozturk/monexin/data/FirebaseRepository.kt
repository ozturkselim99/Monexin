package com.selimozturk.monexin.data

import android.net.Uri
import com.selimozturk.monexin.model.*
import com.selimozturk.monexin.utils.Resource

interface FirebaseRepository {

    suspend fun getProfileInfo(): Resource<UserInfo>
    suspend fun addTransaction(transaction: Transactions, photoUri: Uri?): Resource<String>
    suspend fun getHomeInfo(minDate: String?, maxDate: String?): Resource<HomeInfo>
    suspend fun getExpenses(filterModel: FilterModel?): Resource<ExpensesInfo>
    suspend fun getIncomes(filterModel: FilterModel?): Resource<IncomesInfo>
    suspend fun clearAllTransaction()
    suspend fun deleteTransaction(transaction:Transactions)
    suspend fun updateTransaction(transaction: Transactions, photoUri: Uri?): Resource<String>
    suspend fun downloadTransactionImage(url: String?): Resource<Uri>

}
package com.selimozturk.monexin.data

import android.net.Uri
import com.selimozturk.monexin.model.*
import com.selimozturk.monexin.utils.Resource

interface FirebaseRepository {
    suspend fun getProfileInfo(): Resource<UserInfo>
    suspend fun addTransaction(transaction:Transactions,photoUri: Uri?):Resource<String>
    suspend fun getHomeInfo(minDate:String?,maxDate:String?):Resource<HomeInfo>
    suspend fun getExpenses():Resource<ExpensesInfo>
    suspend fun getIncomes():Resource<IncomesInfo>
    suspend fun clearAllTransaction()
    suspend fun deleteTransaction(collectionPath:String,id:String)
    suspend fun updateTransaction(transaction:Transactions,photoUri: Uri?):Resource<String>
    suspend fun downloadTransactionImage(url:String?):Resource<Uri>
}
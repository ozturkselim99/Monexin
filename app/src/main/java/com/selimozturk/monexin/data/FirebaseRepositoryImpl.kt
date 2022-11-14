package com.selimozturk.monexin.data

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.selimozturk.monexin.model.*
import com.selimozturk.monexin.utils.Resource
import com.selimozturk.monexin.utils.await
import javax.inject.Inject

class FirebaseRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firebaseFirestore: FirebaseFirestore,
    private val firebaseStorage: FirebaseStorage,
) : FirebaseRepository {
    override suspend fun getProfileInfo(): Resource<UserInfo> {
        return try {
            val profileInfo =
                firebaseFirestore.collection("users").document(firebaseAuth.currentUser?.uid!!)
                    .get().await()
            Resource.Success(
                UserInfo(
                    profileInfo.data?.get("accountNumber").toString(),
                    profileInfo.data?.get("name").toString(),
                    profileInfo.data?.get("email").toString(),
                    profileInfo.data?.get("device").toString(),
                    profileInfo.data?.get("joinedDate").toString()
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Failure(e)
        }
    }

    override suspend fun addTransaction(
        transaction: Transactions,
        photoUri: Uri?
    ): Resource<String> {
        return try {
            val userPath =
                firebaseFirestore.collection("users").document(firebaseAuth.currentUser?.uid!!)
            val expensesPath = userPath.collection("expenses")
            val incomesPath = userPath.collection("incomes")
            photoUri?.let {
                firebaseStorage.reference.child(transaction.photoPath).putFile(it).await()
            }
            val result: DocumentReference
            if (transaction.type == "Expense") {
                result = expensesPath.add(transaction).await()
                expensesPath.document(result.id).update("id", result.id).await()
            } else {
                result = incomesPath.add(transaction).await()
                incomesPath.document(result.id).update("id", result.id).await()
            }
            Resource.Success(result.id)
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Failure(e)
        }
    }

    override suspend fun updateTransaction(
        transaction: Transactions,
        photoUri: Uri?
    ): Resource<String> {
        return try {
            val userPath =
                firebaseFirestore.collection("users").document(firebaseAuth.currentUser?.uid!!)
            val expensesPath = userPath.collection("expenses")
            val incomesPath = userPath.collection("incomes")
            if (transaction.type == "Expense") {
                expensesPath.document(transaction.id).update(
                    "title",
                    transaction.title,
                    "description",
                    transaction.description,
                    "amount",
                    transaction.amount,
                    "type",
                    transaction.type,
                    "photoPath",
                    transaction.photoPath
                ).await()
            } else {
                incomesPath.document(transaction.id).update(
                    "title",
                    transaction.title,
                    "description",
                    transaction.description,
                    "amount",
                    transaction.amount,
                    "type",
                    transaction.type,
                    "photoPath",
                    transaction.photoPath
                ).await()
            }
            var result = "Success"
            photoUri?.let {
                result = firebaseStorage.reference.child(transaction.photoPath).putFile(it)
                    .await().uploadSessionUri.toString()
            }
            Resource.Success(result)

        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Failure(e)
        }
    }

    override suspend fun getHomeInfo(minDate: String?, maxDate: String?): Resource<HomeInfo> {
        return try {
            var activeExpense: Double=0.0
            var activeIncome: Double=0.0
            val expenses =
                firebaseFirestore.collection("users").document(firebaseAuth.currentUser?.uid!!)
                    .collection("expenses").get().await()
            val incomes =
                firebaseFirestore.collection("users").document(firebaseAuth.currentUser?.uid!!)
                    .collection("incomes").get().await()
            val list =
                expenses.toObjects(Transactions::class.java) + incomes.toObjects(Transactions::class.java)
            activeExpense = expenses.toObjects(Transactions::class.java)
                .filter { transactions -> transactions.createdAt >= minDate.toString() && transactions.createdAt <= maxDate.toString() }
                .sumOf { it.amount }
            activeIncome = incomes.toObjects(Transactions::class.java)
                .filter { transactions -> transactions.createdAt >= minDate.toString() && transactions.createdAt <= maxDate.toString() }
                .sumOf { it.amount }
            val recentlyAdded = list.sortedByDescending { it.createdAt }.take(5)
            Resource.Success(
                HomeInfo(
                    firebaseAuth.currentUser?.displayName!!,
                    activeExpense.toString(),
                    activeIncome.toString(),
                    recentlyAdded
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Failure(e)
        }
    }

    override suspend fun getExpenses(): Resource<ExpensesInfo> {
        return try {
            var activeExpense = 0.0
            val expenses =
                firebaseFirestore.collection("users").document(firebaseAuth.currentUser?.uid!!)
                    .collection("expenses").get().await()
            for (document in expenses.documents) {
                activeExpense += document.data?.getValue("amount").toString().toDouble()
            }
            val expensesList =
                expenses.toObjects(Transactions::class.java).sortedByDescending { it.createdAt }
            Resource.Success(ExpensesInfo(activeExpense.toString(), expensesList))
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Failure(e)
        }
    }

    override suspend fun getIncomes(): Resource<IncomesInfo> {
        return try {
            var activeIncome = 0.0
            val incomes =
                firebaseFirestore.collection("users").document(firebaseAuth.currentUser?.uid!!)
                    .collection("incomes").get().await()
            for (document in incomes.documents) {
                activeIncome += document.data?.getValue("amount").toString().toDouble()
            }
            val incomesList =
                incomes.toObjects(Transactions::class.java).sortedByDescending { it.createdAt }
            Resource.Success(IncomesInfo(activeIncome.toString(), incomesList))
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Failure(e)
        }
    }

    override suspend fun clearAllTransaction() {
        try {
            val expenses =
                firebaseFirestore.collection("users").document(firebaseAuth.currentUser?.uid!!)
                    .collection("expenses").get().await()
            for (it in expenses.documents) {
                it.reference.delete()
            }
            val incomes =
                firebaseFirestore.collection("users").document(firebaseAuth.currentUser?.uid!!)
                    .collection("incomes").get().await()
            for (it in incomes.documents) {
                it.reference.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun deleteTransaction(collectionPath: String, id: String) {
        try {
            firebaseFirestore.collection("users").document(firebaseAuth.currentUser?.uid!!)
                .collection(collectionPath).document(id).delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun downloadTransactionImage(url: String?): Resource<Uri> {
        return try {
            val result = firebaseStorage.reference.child(url!!).downloadUrl.await()
            Resource.Success(result)
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Failure(e)
        }
    }
}
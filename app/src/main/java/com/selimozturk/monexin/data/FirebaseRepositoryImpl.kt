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
            val activeExpense: Double
            val activeIncome: Double
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

    override suspend fun getExpenses(filterModel: FilterModel?): Resource<ExpensesInfo> {
        return try {
            var activeExpense: Double
            val expensesList: List<Transactions>
            val expenses =
                firebaseFirestore.collection("users").document(firebaseAuth.currentUser?.uid!!)
                    .collection("expenses").get().await()
            activeExpense = expenses.toObjects(Transactions::class.java).sumOf { it.amount }
            if (filterModel != null) {
                when (filterModel.bestMatchResult) {
                    "0" -> {
                        expensesList = expenses.toObjects(Transactions::class.java)
                            .sortedByDescending { it.createdAt }
                            .filter { transactions -> transactions.createdAt >= filterModel.minDate && transactions.createdAt <= filterModel.maxDate && transactions.amount >= filterModel.minAmount.toDouble() && transactions.amount <= filterModel.maxAmount.toDouble() }
                        activeExpense = expenses.toObjects(Transactions::class.java)
                            .filter { transactions -> transactions.createdAt >= filterModel.minDate && transactions.createdAt <= filterModel.maxDate && transactions.amount >= filterModel.minAmount.toDouble() && transactions.amount <= filterModel.maxAmount.toDouble() }
                            .sumOf { it.amount }
                    }
                    "1" -> {
                        expensesList = expenses.toObjects(Transactions::class.java)
                            .sortedBy { it.createdAt }
                            .filter { transactions -> transactions.createdAt >= filterModel.minDate && transactions.createdAt <= filterModel.maxDate && transactions.amount >= filterModel.minAmount.toDouble() && transactions.amount <= filterModel.maxAmount.toDouble() }
                        activeExpense = expenses.toObjects(Transactions::class.java)
                            .filter { transactions -> transactions.createdAt >= filterModel.minDate && transactions.createdAt <= filterModel.maxDate && transactions.amount >= filterModel.minAmount.toDouble() && transactions.amount <= filterModel.maxAmount.toDouble() }
                            .sumOf { it.amount }
                    }
                    "2" -> {
                        expensesList = expenses.toObjects(Transactions::class.java)
                            .sortedByDescending { it.amount }
                            .filter { transactions -> transactions.createdAt >= filterModel.minDate && transactions.createdAt <= filterModel.maxDate && transactions.amount >= filterModel.minAmount.toDouble() && transactions.amount <= filterModel.maxAmount.toDouble() }
                        activeExpense = expenses.toObjects(Transactions::class.java)
                            .filter { transactions -> transactions.createdAt >= filterModel.minDate && transactions.createdAt <= filterModel.maxDate && transactions.amount >= filterModel.minAmount.toDouble() && transactions.amount <= filterModel.maxAmount.toDouble() }
                            .sumOf { it.amount }
                    }
                    "3" -> {
                        expensesList = expenses.toObjects(Transactions::class.java)
                            .sortedBy { it.amount }
                            .filter { transactions -> transactions.createdAt >= filterModel.minDate && transactions.createdAt <= filterModel.maxDate && transactions.amount >= filterModel.minAmount.toDouble() && transactions.amount <= filterModel.maxAmount.toDouble() }
                        activeExpense = expenses.toObjects(Transactions::class.java)
                            .filter { transactions -> transactions.createdAt >= filterModel.minDate && transactions.createdAt <= filterModel.maxDate && transactions.amount >= filterModel.minAmount.toDouble() && transactions.amount <= filterModel.maxAmount.toDouble() }
                            .sumOf { it.amount }
                    }
                    else -> throw IllegalStateException("Best Match Result")
                }
            } else {
                expensesList =
                    expenses.toObjects(Transactions::class.java).sortedByDescending { it.createdAt }
            }

            Resource.Success(ExpensesInfo(activeExpense.toString(), expensesList))
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Failure(e)
        }
    }

    override suspend fun getIncomes(filterModel: FilterModel?): Resource<IncomesInfo> {
        return try {
            var activeIncome: Double
            val incomesList: List<Transactions>
            val incomes =
                firebaseFirestore.collection("users").document(firebaseAuth.currentUser?.uid!!)
                    .collection("incomes").get().await()
            activeIncome = incomes.toObjects(Transactions::class.java).sumOf { it.amount }
            if (filterModel != null) {
                when (filterModel.bestMatchResult) {
                    "0" -> {
                        incomesList = incomes.toObjects(Transactions::class.java)
                            .sortedByDescending { it.createdAt }
                            .filter { transactions -> transactions.createdAt >= filterModel.minDate && transactions.createdAt <= filterModel.maxDate && transactions.amount >= filterModel.minAmount.toDouble() && transactions.amount <= filterModel.maxAmount.toDouble() }
                        activeIncome = incomes.toObjects(Transactions::class.java)
                            .filter { transactions -> transactions.createdAt >= filterModel.minDate && transactions.createdAt <= filterModel.maxDate && transactions.amount >= filterModel.minAmount.toDouble() && transactions.amount <= filterModel.maxAmount.toDouble() }
                            .sumOf { it.amount }
                    }
                    "1" -> {
                        incomesList = incomes.toObjects(Transactions::class.java)
                            .sortedBy { it.createdAt }
                            .filter { transactions -> transactions.createdAt >= filterModel.minDate && transactions.createdAt <= filterModel.maxDate && transactions.amount >= filterModel.minAmount.toDouble() && transactions.amount <= filterModel.maxAmount.toDouble() }
                        activeIncome = incomes.toObjects(Transactions::class.java)
                            .filter { transactions -> transactions.createdAt >= filterModel.minDate && transactions.createdAt <= filterModel.maxDate && transactions.amount >= filterModel.minAmount.toDouble() && transactions.amount <= filterModel.maxAmount.toDouble() }
                            .sumOf { it.amount }
                    }
                    "2" -> {
                        incomesList = incomes.toObjects(Transactions::class.java)
                            .sortedByDescending { it.amount }
                            .filter { transactions -> transactions.createdAt >= filterModel.minDate && transactions.createdAt <= filterModel.maxDate && transactions.amount >= filterModel.minAmount.toDouble() && transactions.amount <= filterModel.maxAmount.toDouble() }
                        activeIncome = incomes.toObjects(Transactions::class.java)
                            .filter { transactions -> transactions.createdAt >= filterModel.minDate && transactions.createdAt <= filterModel.maxDate && transactions.amount >= filterModel.minAmount.toDouble() && transactions.amount <= filterModel.maxAmount.toDouble() }
                            .sumOf { it.amount }
                    }
                    "3" -> {
                        incomesList = incomes.toObjects(Transactions::class.java)
                            .sortedBy { it.amount }
                            .filter { transactions -> transactions.createdAt >= filterModel.minDate && transactions.createdAt <= filterModel.maxDate && transactions.amount >= filterModel.minAmount.toDouble() && transactions.amount <= filterModel.maxAmount.toDouble() }
                        activeIncome = incomes.toObjects(Transactions::class.java)
                            .filter { transactions -> transactions.createdAt >= filterModel.minDate && transactions.createdAt <= filterModel.maxDate && transactions.amount >= filterModel.minAmount.toDouble() && transactions.amount <= filterModel.maxAmount.toDouble() }
                            .sumOf { it.amount }
                    }
                    else -> throw IllegalStateException("Best Match Result")
                }
            } else {
                incomesList =
                    incomes.toObjects(Transactions::class.java).sortedByDescending { it.createdAt }
            }

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
            expenses.forEach{document->document.reference.delete()}
            val incomes =
                firebaseFirestore.collection("users").document(firebaseAuth.currentUser?.uid!!)
                    .collection("incomes").get().await()
            incomes.forEach{document->document.reference.delete()}
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun deleteTransaction(transaction: Transactions) {
        try {
            if(transaction.photoPath!="") {
                firebaseStorage.reference.child(transaction.photoPath).delete()
            }
            if (transaction.type == "Expense") {
                firebaseFirestore.collection("users").document(firebaseAuth.currentUser?.uid!!)
                    .collection("expenses").document(transaction.id).delete().await()
            } else {
                firebaseFirestore.collection("users").document(firebaseAuth.currentUser?.uid!!)
                    .collection("incomes").document(transaction.id).delete().await()
            }
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
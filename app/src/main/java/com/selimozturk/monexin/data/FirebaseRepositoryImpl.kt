package com.selimozturk.monexin.data

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.selimozturk.monexin.model.*
import com.selimozturk.monexin.utils.BestMatchResult
import com.selimozturk.monexin.utils.Resource
import com.selimozturk.monexin.utils.await
import java.io.IOException
import javax.inject.Inject

class FirebaseRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firebaseFirestore: FirebaseFirestore,
    private val firebaseStorage: FirebaseStorage,
) : FirebaseRepository {

    override suspend fun getProfileInfo(): Resource<UserInfo> {
        return try {
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null) {
                Resource.Failure(Exception("User is not authenticated"))
            } else {
                val profileInfo =
                    firebaseFirestore.collection("users").document(currentUser.uid).get().await()
                val accountNumber = profileInfo.data?.get("accountNumber").toString()
                val name = profileInfo.data?.get("name").toString()
                val email = profileInfo.data?.get("email").toString()
                val device = profileInfo.data?.get("device").toString()
                val joinedDate = profileInfo.data?.get("joinedDate").toString()

                Resource.Success(UserInfo(accountNumber, name, email, device, joinedDate))
            }
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
            val expensesPath = userPath.collection("expense")
            val incomesPath = userPath.collection("income")

            photoUri?.let {
                firebaseStorage.reference.child(transaction.photoPath).putFile(it).await()
            }

            val id = when (transaction.type) {
                "expense" -> expensesPath.add(transaction).await().id
                else -> incomesPath.add(transaction).await().id
            }

            userPath.collection(transaction.type).document(id).update("id", id).await()
            Resource.Success(id)
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
            val (collectionPath, documentId) = when (transaction.type) {
                "expense" -> Pair(userPath.collection("expense"), transaction.id)
                "income" -> Pair(userPath.collection("income"), transaction.id)
                else -> throw IllegalArgumentException("Invalid transaction type: ${transaction.type}")
            }
            updateTransactionInCollection(collectionPath, documentId, transaction)
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
            val userId = firebaseAuth.currentUser?.uid!!
            val expensesPath =
                firebaseFirestore.collection("users").document(userId).collection("expense")
            val incomesPath =
                firebaseFirestore.collection("users").document(userId).collection("income")

            val activeExpense =
                expensesPath.whereGreaterThanOrEqualTo("createdAt", minDate.toString())
                    .whereLessThanOrEqualTo("createdAt", maxDate.toString())
                    .get().await()
                    .sumOf { it.getDouble("amount") ?: 0.0 }

            val activeIncome =
                incomesPath.whereGreaterThanOrEqualTo("createdAt", minDate.toString())
                    .whereLessThanOrEqualTo("createdAt", maxDate.toString())
                    .get().await()
                    .sumOf { it.getDouble("amount") ?: 0.0 }

            val list = (expensesPath.get().await().toObjects(Transactions::class.java) +
                    incomesPath.get().await().toObjects(Transactions::class.java))
            val recentlyAdded = list.sortedByDescending { it.createdAt }
                .take(5)

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
            val expenses =
                firebaseFirestore.collection("users").document(firebaseAuth.currentUser?.uid!!)
                    .collection("expense").get().await()

            val filteredExpenses = expenses.toObjects(Transactions::class.java)
                .filter { transactions ->
                    filterModel?.let {
                        transactions.createdAt >= it.minDate &&
                                transactions.createdAt <= it.maxDate &&
                                transactions.amount >= it.minAmount.toDouble() &&
                                transactions.amount <= it.maxAmount.toDouble()
                    } ?: true
                }

            val expensesList = when (filterModel?.bestMatchResult) {
                BestMatchResult.ASCENDING_BY_DATE -> filteredExpenses.sortedBy { it.createdAt }
                BestMatchResult.DECREASING_BY_AMOUNT -> filteredExpenses.sortedByDescending { it.amount }
                BestMatchResult.INCREASING_BY_AMOUNT -> filteredExpenses.sortedBy { it.amount }
                else -> filteredExpenses.sortedByDescending { it.createdAt }
            }

            val activeExpense = filteredExpenses.sumOf { it.amount }

            Resource.Success(ExpensesInfo(activeExpense.toString(), expensesList))
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Failure(e)
        }
    }

    override suspend fun getIncomes(filterModel: FilterModel?): Resource<IncomesInfo> {
        return try {
            val incomes =
                firebaseFirestore.collection("users").document(firebaseAuth.currentUser?.uid!!)
                    .collection("income").get().await()

            val filteredIncomes = incomes.toObjects(Transactions::class.java)
                .filter { transactions ->
                    filterModel?.let {
                        transactions.createdAt >= it.minDate &&
                                transactions.createdAt <= it.maxDate &&
                                transactions.amount >= it.minAmount.toDouble() &&
                                transactions.amount <= it.maxAmount.toDouble()
                    } ?: true
                }

            val incomesList = when (filterModel?.bestMatchResult) {
                BestMatchResult.ASCENDING_BY_DATE -> filteredIncomes.sortedBy { it.createdAt }
                BestMatchResult.DECREASING_BY_AMOUNT -> filteredIncomes.sortedByDescending { it.amount }
                BestMatchResult.INCREASING_BY_AMOUNT -> filteredIncomes.sortedBy { it.amount }
                else -> filteredIncomes.sortedByDescending { it.createdAt }
            }

            val activeIncome = filteredIncomes.sumOf { it.amount }

            Resource.Success(IncomesInfo(activeIncome.toString(), incomesList))
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Failure(e)
        }
    }

    override suspend fun clearAllTransaction() {
        try {
            val expensesRef =
                firebaseFirestore.collection("users").document(firebaseAuth.currentUser!!.uid)
                    .collection("expense")
            expensesRef.get().await().forEach { document -> document.reference.delete() }
            val incomesRef =
                firebaseFirestore.collection("users").document(firebaseAuth.currentUser!!.uid)
                    .collection("income")
            incomesRef.get().await().forEach { document -> document.reference.delete() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun deleteTransaction(transaction: Transactions) {
        try {
            if (transaction.photoPath.isNotEmpty()) {
                firebaseStorage.reference.child(transaction.photoPath).delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val collectionName = if (transaction.type == "expense") "expense" else "income"
            firebaseFirestore.collection("users")
                .document(firebaseAuth.currentUser?.uid!!)
                .collection(collectionName)
                .document(transaction.id)
                .delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun downloadTransactionImage(url: String?): Resource<Uri> {
        return try {
            val result = firebaseStorage.reference.child(url!!).downloadUrl.await()
            Resource.Success(result)
        } catch (e: Exception) {
            val errorMsg = "Failed to download image with URL: $url. ${e.message}"
            Resource.Failure(IOException(errorMsg, e))
        }
    }

    private suspend fun updateTransactionInCollection(
        collection: CollectionReference,
        documentId: String,
        transaction: Transactions
    ) {
        collection.document(documentId).update(
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

}
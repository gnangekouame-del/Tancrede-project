package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ChurchTransaction
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

class TransactionRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing church treasury.")
    }

    private fun transactionsRef(userId: String) =
        db.collection("users").document(userId).collection("transactions")

    fun observeTransactions(): Flow<List<ChurchTransaction>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/transactions"
        emitAll(
            transactionsRef(uid)
                .orderBy("date", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ChurchTransaction::class.java)
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun getTransactionById(transactionId: String): Result<ChurchTransaction> {
        val uid = requireUserId()
        val docRef = transactionsRef(uid).document(transactionId)
        return try {
            val snapshot = docRef.get().await()
            val item = snapshot.toObject(ChurchTransaction::class.java)
            if (item != null) {
                Result.success(item)
            } else {
                Result.failure(NoSuchElementException("Transaction not found"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun addTransaction(transaction: ChurchTransaction): Result<String> {
        val uid = requireUserId()
        val ref = transactionsRef(uid)
        return try {
            val doc = ref.document()
            val payload = hashMapOf<String, Any>(
                "userId" to uid,
                "type" to transaction.type,
                "amount" to transaction.amount,
                "category" to transaction.category,
                "categoryLabel" to transaction.categoryLabel,
                "paymentMethod" to transaction.paymentMethod,
                "serviceType" to transaction.serviceType,
                "date" to transaction.date,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (transaction.donorOrRecipient.isNotBlank()) {
                payload["donorOrRecipient"] = transaction.donorOrRecipient
            }
            if (transaction.note.isNotBlank()) {
                payload["note"] = transaction.note
            }
            if (transaction.referenceNumber.isNotBlank()) {
                payload["referenceNumber"] = transaction.referenceNumber
            }

            doc.set(payload).await()
            Result.success(doc.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun updateTransaction(transaction: ChurchTransaction): Result<Unit> {
        val uid = requireUserId()
        require(transaction.id.isNotBlank()) { "Transaction ID must not be blank" }
        requireNotNull(transaction.createdAt) { "Transaction createdAt must not be null for update" }
        val docRef = transactionsRef(uid).document(transaction.id)

        return try {
            val payload = hashMapOf<String, Any>(
                "userId" to uid,
                "type" to transaction.type,
                "amount" to transaction.amount,
                "category" to transaction.category,
                "categoryLabel" to transaction.categoryLabel,
                "paymentMethod" to transaction.paymentMethod,
                "serviceType" to transaction.serviceType,
                "date" to transaction.date,
                "createdAt" to transaction.createdAt,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (transaction.donorOrRecipient.isNotBlank()) {
                payload["donorOrRecipient"] = transaction.donorOrRecipient
            }
            if (transaction.note.isNotBlank()) {
                payload["note"] = transaction.note
            }
            if (transaction.referenceNumber.isNotBlank()) {
                payload["referenceNumber"] = transaction.referenceNumber
            }

            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteTransaction(transactionId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = transactionsRef(uid).document(transactionId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}

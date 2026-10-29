package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.ChurchTransaction
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TransactionRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun addTransaction_validPayload_createsDocumentAndReturnsId() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = TransactionRepository(firestore)

    val item = ChurchTransaction(
      type = "RECETTE",
      amount = 75000.0,
      category = "OFFRANDE",
      categoryLabel = "Offrande du dimanche",
      paymentMethod = "ESPECES",
      serviceType = "CULTE_DIMANCHE",
      donorOrRecipient = "Assemblée",
      note = "Culte d'action de grâce",
      referenceNumber = "REF-101",
      date = "2026-10-25"
    )

    val createResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.addTransaction(item) }
    assertTrue(createResult.isSuccess)
    val txId = createResult.getOrThrow()
    assertTrue(txId.isNotEmpty())

    val fetchResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.getTransactionById(txId) }
    assertTrue(fetchResult.isSuccess)
    val fetched = fetchResult.getOrThrow()
    assertEquals(75000.0, fetched.amount, 0.001)
    assertEquals(uid, fetched.userId)
  }

  @Test
  fun observeTransactions_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repository = TransactionRepository(firestore)

    val item = ChurchTransaction(
      type = "RECETTE",
      amount = 25000.0,
      category = "DIME",
      categoryLabel = "Dîme",
      paymentMethod = "MOBILE_MONEY",
      serviceType = "CULTE_DIMANCHE",
      date = "2026-10-25"
    )
    val txId = withTimeout(DEFAULT_TIMEOUT_MS) { repository.addTransaction(item).getOrThrow() }

    val emitted = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeTransactions().first { list -> list.any { it.id == txId } }
    }
    assertTrue(emitted.any { it.id == txId })
  }

  @Test
  fun getTransactionById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = TransactionRepository(firestore)
    val item = ChurchTransaction(
      type = "DEPENSE",
      amount = 12000.0,
      category = "FACTURE",
      categoryLabel = "Facture Électricité",
      paymentMethod = "ESPECES",
      serviceType = "AUTRE",
      date = "2026-10-25"
    )
    val txId = withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.addTransaction(item).getOrThrow() }

    signInTestUser(BOB_EMAIL)
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("users").document(aliceUid).collection("transactions").document(txId).get().await()
      }
      fail("Expected PERMISSION_DENIED on cross-user read")
    } catch (e: Exception) {
      val fsEx = e.unwrapFirestoreException()
      val code = if (e is FirebaseFirestoreException) e.code else fsEx?.code
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, code)
    }
  }

  @Test
  fun observeTransactions_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repository = TransactionRepository(firestore)

    try {
      withTimeout(FLOW_TIMEOUT_MS) { repository.observeTransactions().first() }
      fail("Expected unauthenticated query to fail")
    } catch (e: Exception) {
      val fsEx = e.unwrapFirestoreException()
      val isExpected = e is IllegalStateException || fsEx?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
      assertTrue(isExpected)
    }
  }

  private companion object {
    const val ALICE_EMAIL = "alice_treasurer@test.com"
    const val BOB_EMAIL = "bob_treasurer@test.com"
    const val DEFAULT_TIMEOUT_MS = 6000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}

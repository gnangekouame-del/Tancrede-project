package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class ChurchTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = "RECETTE", // "RECETTE" or "DEPENSE"
    val amount: Double = 0.0,
    val category: String = "OFFRANDE",
    val categoryLabel: String = "Offrande ordinaire",
    val paymentMethod: String = "ESPECES", // "ESPECES", "MOBILE_MONEY", "BANQUE", "CHEQUE", "AUTRE"
    val serviceType: String = "CULTE_DIMANCHE", // "CULTE_DIMANCHE", "CULTE_SEMAINE", "VEILLEE", "EVENEMENT_SPECIAL", "AUTRE"
    val donorOrRecipient: String = "",
    val note: String = "",
    val referenceNumber: String = "",
    val date: String = "", // "YYYY-MM-DD"
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ChurchTransaction
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.Navy800
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CategoryItem(val key: String, val label: String)

val INCOME_CATEGORIES = listOf(
    CategoryItem("DIME", "Dîme"),
    CategoryItem("OFFRANDE", "Offrande ordinaire"),
    CategoryItem("QUETE_SPECIALE", "Quête spéciale (Travaux/Jeunes)"),
    CategoryItem("ACTION_GRACE", "Action de grâce"),
    CategoryItem("DON", "Don & Libéralité"),
    CategoryItem("VENTE_CHARITE", "Vente de charité / Bazar"),
    CategoryItem("AUTRE", "Autre recette")
)

val EXPENSE_CATEGORIES = listOf(
    CategoryItem("TRAVAUX", "Travaux & Entretien"),
    CategoryItem("FACTURE", "Factures (Électricité/Eau/Sono)"),
    CategoryItem("SOCIAL", "Social & Aide aux nécessiteux"),
    CategoryItem("MISSION", "Missions & Évangélisation"),
    CategoryItem("HONORAIRES", "Pasteur & Prédicateurs invités"),
    CategoryItem("CULTE", "Fournitures Culte & Sainte Cène"),
    CategoryItem("AUTRE", "Autre dépense")
)

val PAYMENT_METHODS = listOf(
    CategoryItem("ESPECES", "Espèces (Caisse)"),
    CategoryItem("MOBILE_MONEY", "Mobile Money (Wave/Orange/MTN)"),
    CategoryItem("BANQUE", "Banque / Virement"),
    CategoryItem("CHEQUE", "Chèque")
)

val SERVICES = listOf(
    CategoryItem("CULTE_DIMANCHE", "Culte du Dimanche"),
    CategoryItem("CULTE_SEMAINE", "Culte de Semaine"),
    CategoryItem("VEILLEE", "Veillée de Prière"),
    CategoryItem("EVENEMENT_SPECIAL", "Séminaire / Fête"),
    CategoryItem("AUTRE", "Hors culte")
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionDialog(
    initialTransaction: ChurchTransaction? = null,
    defaultType: String = "RECETTE",
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (ChurchTransaction) -> Unit
) {
    var type by remember { mutableStateOf(initialTransaction?.type ?: defaultType) }
    var amountText by remember {
        mutableStateOf(if (initialTransaction != null && initialTransaction.amount > 0) initialTransaction.amount.toLong().toString() else "")
    }
    var selectedCategory by remember {
        mutableStateOf(initialTransaction?.category ?: if (type == "RECETTE") "OFFRANDE" else "FACTURE")
    }
    var selectedCategoryLabel by remember {
        val initialList = if (type == "RECETTE") INCOME_CATEGORIES else EXPENSE_CATEGORIES
        val matched = initialList.find { it.key == selectedCategory }?.label
        mutableStateOf(initialTransaction?.categoryLabel ?: (matched ?: "Offrande ordinaire"))
    }
    var selectedPaymentMethod by remember {
        mutableStateOf(initialTransaction?.paymentMethod ?: "ESPECES")
    }
    var selectedService by remember {
        mutableStateOf(initialTransaction?.serviceType ?: "CULTE_DIMANCHE")
    }
    var donorOrRecipient by remember { mutableStateOf(initialTransaction?.donorOrRecipient ?: "") }
    var note by remember { mutableStateOf(initialTransaction?.note ?: "") }
    var referenceNumber by remember { mutableStateOf(initialTransaction?.referenceNumber ?: "") }
    var dateText by remember {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        mutableStateOf(initialTransaction?.date?.ifBlank { today } ?: today)
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialTransaction == null) "Nouvelle Opération" else "Modifier l'Opération",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Type Toggle (Entrée vs Sortie)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (type == "RECETTE") IncomeGreen else Color.Transparent)
                            .clickable {
                                type = "RECETTE"
                                selectedCategory = "OFFRANDE"
                                selectedCategoryLabel = "Offrande ordinaire"
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = if (type == "RECETTE") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Entrée (Recette)",
                                fontWeight = FontWeight.SemiBold,
                                color = if (type == "RECETTE") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (type == "DEPENSE") ExpenseRed else Color.Transparent)
                            .clickable {
                                type = "DEPENSE"
                                selectedCategory = "FACTURE"
                                selectedCategoryLabel = "Factures (Électricité/Eau/Sono)"
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = null,
                                tint = if (type == "DEPENSE") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sortie (Dépense)",
                                fontWeight = FontWeight.SemiBold,
                                color = if (type == "DEPENSE") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Montant Input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            amountText = input
                            errorMessage = null
                        }
                    },
                    label = { Text("Montant ($currencySymbol)") },
                    placeholder = { Text("Ex: 50000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_amount_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Category Selector
                Text(
                    text = "Catégorie paroissiale",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                val categories = if (type == "RECETTE") INCOME_CATEGORIES else EXPENSE_CATEGORIES
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat.key
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategory = cat.key
                                selectedCategoryLabel = cat.label
                            },
                            label = { Text(cat.label, style = MaterialTheme.typography.bodySmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (type == "RECETTE") IncomeGreen.copy(alpha = 0.2f) else ExpenseRed.copy(alpha = 0.2f),
                                selectedLabelColor = if (type == "RECETTE") IncomeGreen else ExpenseRed
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Caisse / Mode de paiement
                Text(
                    text = "Caisse / Moyen de paiement",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PAYMENT_METHODS.forEach { pm ->
                        val isSelected = selectedPaymentMethod == pm.key
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedPaymentMethod = pm.key },
                            label = { Text(pm.label, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Culte / Cadre de l'opération
                Text(
                    text = "Culte ou Événement",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SERVICES.forEach { s ->
                        val isSelected = selectedService == s.key
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedService = s.key },
                            label = { Text(s.label, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Date
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Date (AAAA-MM-JJ)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Donateur ou Bénéficiaire
                OutlinedTextField(
                    value = donorOrRecipient,
                    onValueChange = { donorOrRecipient = it },
                    label = { Text(if (type == "RECETTE") "Donateur / Fidèle (Optionnel)" else "Bénéficiaire / Fournisseur (Optionnel)") },
                    placeholder = { Text(if (type == "RECETTE") "Ex: Frère Marc / Anonyme" else "Ex: Compagnie Électricité / Prestataire") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Reference / N° de pièce
                OutlinedTextField(
                    value = referenceNumber,
                    onValueChange = { referenceNumber = it },
                    label = { Text("N° de Reçu / Référence (Optionnel)") },
                    placeholder = { Text("Ex: REC-2026-042 ou Ref Wave") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Note
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Remarque / Description détaillée (Optionnel)") },
                    placeholder = { Text("Détails complémentaires sur la dépense ou l'offrande") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Save button
                Button(
                    onClick = {
                        val amountVal = amountText.toDoubleOrNull()
                        if (amountVal == null || amountVal <= 0) {
                            errorMessage = "Veuillez saisir un montant valide supérieur à 0."
                            return@Button
                        }
                        if (dateText.isBlank()) {
                            errorMessage = "Veuillez renseigner la date."
                            return@Button
                        }

                        val transaction = (initialTransaction ?: ChurchTransaction()).copy(
                            type = type,
                            amount = amountVal,
                            category = selectedCategory,
                            categoryLabel = selectedCategoryLabel,
                            paymentMethod = selectedPaymentMethod,
                            serviceType = selectedService,
                            donorOrRecipient = donorOrRecipient.trim(),
                            note = note.trim(),
                            referenceNumber = referenceNumber.trim(),
                            date = dateText.trim()
                        )
                        onSave(transaction)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_transaction_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (type == "RECETTE") IncomeGreen else Navy800
                    )
                ) {
                    Text(
                        text = if (initialTransaction == null) "Enregistrer dans la Caisse" else "Enregistrer les Modifications",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

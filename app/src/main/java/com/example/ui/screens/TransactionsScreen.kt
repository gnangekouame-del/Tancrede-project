package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ChurchTransaction
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.Navy800
import com.example.ui.viewmodel.PaymentFilter
import com.example.ui.viewmodel.PeriodFilter
import com.example.ui.viewmodel.TreasurySummary
import com.example.ui.viewmodel.TypeFilter
import com.example.ui.viewmodel.UiState

@Composable
fun TransactionsScreen(
    state: UiState<List<ChurchTransaction>>,
    filteredTransactions: List<ChurchTransaction>,
    filteredSummary: TreasurySummary,
    periodFilter: PeriodFilter,
    typeFilter: TypeFilter,
    paymentFilter: PaymentFilter,
    searchQuery: String,
    formatMoney: (Double) -> String,
    onSetPeriodFilter: (PeriodFilter) -> Unit,
    onSetTypeFilter: (TypeFilter) -> Unit,
    onSetPaymentFilter: (PaymentFilter) -> Unit,
    onSetSearchQuery: (String) -> Unit,
    onEditTransaction: (ChurchTransaction) -> Unit,
    onDeleteTransaction: (String) -> Unit
) {
    var selectedTransactionForDetails by remember { mutableStateOf<ChurchTransaction?>(null) }
    var transactionToDelete by remember { mutableStateOf<ChurchTransaction?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Title
        Text(
            text = "Journal de Caisse",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSetSearchQuery,
            placeholder = { Text("Rechercher catégorie, donateur, n° reçu...") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSetSearchQuery("") }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Effacer")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_transactions_input"),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Period filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PeriodFilter.values().forEach { p ->
                FilterChip(
                    selected = periodFilter == p,
                    onClick = { onSetPeriodFilter(p) },
                    label = { Text(p.label, style = MaterialTheme.typography.bodySmall) }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Type & Caisse Filters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TypeFilter.values().forEach { t ->
                val isSelected = typeFilter == t
                FilterChip(
                    selected = isSelected,
                    onClick = { onSetTypeFilter(t) },
                    label = { Text(t.label, style = MaterialTheme.typography.bodySmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = when (t) {
                            TypeFilter.RECETTE -> IncomeGreen.copy(alpha = 0.2f)
                            TypeFilter.DEPENSE -> ExpenseRed.copy(alpha = 0.2f)
                            else -> MaterialTheme.colorScheme.primaryContainer
                        },
                        selectedLabelColor = when (t) {
                            TypeFilter.RECETTE -> IncomeGreen
                            TypeFilter.DEPENSE -> ExpenseRed
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                )
            }

            PaymentFilter.values().filter { it != PaymentFilter.ALL }.forEach { pm ->
                FilterChip(
                    selected = paymentFilter == pm,
                    onClick = {
                        onSetPaymentFilter(if (paymentFilter == pm) PaymentFilter.ALL else pm)
                    },
                    label = { Text(pm.label, style = MaterialTheme.typography.bodySmall) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filtered Results Summary Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredTransactions.size} opération(s) trouvée(s)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Solde net : ${formatMoney(filteredSummary.totalBalance)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (filteredSummary.totalBalance >= 0) IncomeGreen else ExpenseRed
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // List
        when (state) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Navy800)
                }
            }
            is UiState.Error -> {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
            is UiState.Success -> {
                if (filteredTransactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Aucune écriture ne correspond aux filtres",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredTransactions, key = { it.id }) { tx ->
                            TransactionRowItem(
                                transaction = tx,
                                formatMoney = formatMoney,
                                onClick = { selectedTransactionForDetails = tx }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }

    // Transaction Details Dialog
    selectedTransactionForDetails?.let { tx ->
        val isIncome = tx.type == "RECETTE"
        Dialog(onDismissRequest = { selectedTransactionForDetails = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isIncome) "Détail Recette" else "Détail Dépense",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isIncome) IncomeGreen else ExpenseRed
                        )
                        IconButton(onClick = { selectedTransactionForDetails = null }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Fermer")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = (if (isIncome) "+ " else "- ") + formatMoney(tx.amount),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isIncome) IncomeGreen else ExpenseRed
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    DetailItem(label = "Catégorie", value = tx.categoryLabel)
                    DetailItem(label = "Date", value = tx.date)
                    DetailItem(
                        label = "Caisse / Paiement",
                        value = when (tx.paymentMethod) {
                            "MOBILE_MONEY" -> "Mobile Money"
                            "BANQUE" -> "Banque / Virement"
                            "CHEQUE" -> "Chèque"
                            else -> "Espèces (Caisse physique)"
                        }
                    )
                    DetailItem(
                        label = "Cadre / Culte",
                        value = when (tx.serviceType) {
                            "CULTE_DIMANCHE" -> "Culte du Dimanche"
                            "CULTE_SEMAINE" -> "Culte de Semaine"
                            "VEILLEE" -> "Veillée de Prière"
                            "EVENEMENT_SPECIAL" -> "Événement spécial"
                            else -> "Hors culte"
                        }
                    )
                    if (tx.donorOrRecipient.isNotBlank()) {
                        DetailItem(
                            label = if (isIncome) "Donateur / Fidèle" else "Bénéficiaire",
                            value = tx.donorOrRecipient
                        )
                    }
                    if (tx.referenceNumber.isNotBlank()) {
                        DetailItem(label = "N° de Référence / Reçu", value = tx.referenceNumber)
                    }
                    if (tx.note.isNotBlank()) {
                        DetailItem(label = "Remarques", value = tx.note)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val item = tx
                                selectedTransactionForDetails = null
                                transactionToDelete = item
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = ExpenseRed
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Supprimer")
                        }

                        Button(
                            onClick = {
                                val item = tx
                                selectedTransactionForDetails = null
                                onEditTransaction(item)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Modifier")
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation alert dialog
    transactionToDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Supprimer cette opération ?") },
            text = {
                Text("Voulez-vous vraiment supprimer l'écriture \"${tx.categoryLabel}\" d'un montant de ${formatMoney(tx.amount)} ? Cette action est irréversible.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTransaction(tx.id)
                        transactionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Supprimer définitivement")
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.6f)
        )
    }
}

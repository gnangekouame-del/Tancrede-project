package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.ChurchTransaction
import com.example.ui.components.TransactionDialog
import com.example.ui.theme.GoldLight
import com.example.ui.theme.Navy800
import com.example.ui.viewmodel.ChurchTreasuryViewModel
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.launch

enum class MainTab {
    DASHBOARD,
    TRANSACTIONS,
    REPORTS,
    SETTINGS
}

@Composable
fun MainScreen(
    viewModel: ChurchTreasuryViewModel,
    onSignOut: () -> Unit
) {
    var currentTab by remember { mutableStateOf(MainTab.DASHBOARD) }
    var isTransactionDialogOpen by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<ChurchTransaction?>(null) }
    var transactionDefaultType by remember { mutableStateOf("RECETTE") }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val churchName by viewModel.churchName.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val rawState by viewModel.rawTransactionsState.collectAsState()
    val filteredTransactions by viewModel.filteredTransactions.collectAsState()
    val globalSummary by viewModel.globalSummary.collectAsState()
    val filteredSummary by viewModel.filteredSummary.collectAsState()
    val periodFilter by viewModel.periodFilter.collectAsState()
    val typeFilter by viewModel.typeFilter.collectAsState()
    val paymentFilter by viewModel.paymentFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    // Handle back button to return to dashboard
    BackHandler(enabled = currentTab != MainTab.DASHBOARD) {
        currentTab = MainTab.DASHBOARD
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_bottom_nav"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == MainTab.DASHBOARD,
                    onClick = { currentTab = MainTab.DASHBOARD },
                    icon = { Icon(imageVector = Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text("Synthèse") },
                    modifier = Modifier.testTag("nav_dashboard")
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.TRANSACTIONS,
                    onClick = { currentTab = MainTab.TRANSACTIONS },
                    icon = { Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null) },
                    label = { Text("Journal") },
                    modifier = Modifier.testTag("nav_transactions")
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.REPORTS,
                    onClick = { currentTab = MainTab.REPORTS },
                    icon = { Icon(imageVector = Icons.Default.Description, contentDescription = null) },
                    label = { Text("Rapports") },
                    modifier = Modifier.testTag("nav_reports")
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.SETTINGS,
                    onClick = { currentTab = MainTab.SETTINGS },
                    icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Paramètres") },
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        },
        floatingActionButton = {
            if (currentTab == MainTab.DASHBOARD || currentTab == MainTab.TRANSACTIONS) {
                FloatingActionButton(
                    onClick = {
                        transactionToEdit = null
                        transactionDefaultType = "RECETTE"
                        isTransactionDialogOpen = true
                    },
                    containerColor = Navy800,
                    contentColor = GoldLight,
                    modifier = Modifier.testTag("fab_add_transaction")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Nouvelle opération")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentTab) {
                MainTab.DASHBOARD -> {
                    DashboardScreen(
                        churchName = churchName,
                        summary = globalSummary,
                        state = rawState,
                        formatMoney = { viewModel.formatMoney(it) },
                        onOpenAddTransaction = { defaultType ->
                            transactionToEdit = null
                            transactionDefaultType = defaultType
                            isTransactionDialogOpen = true
                        },
                        onViewAllTransactions = { currentTab = MainTab.TRANSACTIONS },
                        onSelectTransaction = { tx ->
                            transactionToEdit = tx
                            isTransactionDialogOpen = true
                        }
                    )
                }
                MainTab.TRANSACTIONS -> {
                    TransactionsScreen(
                        state = rawState,
                        filteredTransactions = filteredTransactions,
                        filteredSummary = filteredSummary,
                        periodFilter = periodFilter,
                        typeFilter = typeFilter,
                        paymentFilter = paymentFilter,
                        searchQuery = searchQuery,
                        formatMoney = { viewModel.formatMoney(it) },
                        onSetPeriodFilter = { viewModel.setPeriodFilter(it) },
                        onSetTypeFilter = { viewModel.setTypeFilter(it) },
                        onSetPaymentFilter = { viewModel.setPaymentFilter(it) },
                        onSetSearchQuery = { viewModel.setSearchQuery(it) },
                        onEditTransaction = { tx ->
                            transactionToEdit = tx
                            isTransactionDialogOpen = true
                        },
                        onDeleteTransaction = { id ->
                            viewModel.deleteTransaction(id) { success, msg ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (success) "Opération supprimée de la caisse" else (msg ?: "Erreur")
                                    )
                                }
                            }
                        }
                    )
                }
                MainTab.REPORTS -> {
                    val allList = if (rawState is UiState.Success) (rawState as UiState.Success<List<ChurchTransaction>>).data else emptyList()
                    ReportsScreen(
                        churchName = churchName,
                        allTransactions = allList,
                        formatMoney = { viewModel.formatMoney(it) },
                        onGenerateReport = { date -> viewModel.generateServiceReport(date) }
                    )
                }
                MainTab.SETTINGS -> {
                    SettingsScreen(
                        currentChurchName = churchName,
                        currentCurrency = currencySymbol,
                        onSaveChurchName = { newName ->
                            viewModel.setChurchName(newName)
                            scope.launch {
                                snackbarHostState.showSnackbar("Nom de la paroisse mis à jour")
                            }
                        },
                        onSelectCurrency = { newCurr ->
                            viewModel.setCurrencySymbol(newCurr)
                            scope.launch {
                                snackbarHostState.showSnackbar("Devise changée en $newCurr")
                            }
                        },
                        onSignOut = onSignOut
                    )
                }
            }
        }
    }

    if (isTransactionDialogOpen) {
        TransactionDialog(
            initialTransaction = transactionToEdit,
            defaultType = transactionDefaultType,
            currencySymbol = currencySymbol,
            onDismiss = { isTransactionDialogOpen = false },
            onSave = { tx ->
                isTransactionDialogOpen = false
                if (tx.id.isBlank()) {
                    viewModel.addTransaction(tx) { success, err ->
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (success) "Écriture enregistrée en caisse !" else (err ?: "Erreur")
                            )
                        }
                    }
                } else {
                    viewModel.updateTransaction(tx) { success, err ->
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (success) "Écriture mise à jour !" else (err ?: "Erreur")
                            )
                        }
                    }
                }
            }
        )
    }
}

package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChurchTransaction
import com.example.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed interface UiState<out T> {
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class PeriodFilter(val label: String) {
    ALL("Tout"),
    TODAY("Aujourd'hui"),
    THIS_WEEK("Cette semaine"),
    THIS_MONTH("Ce mois"),
    THIS_YEAR("Cette année")
}

enum class TypeFilter(val label: String) {
    ALL("Tous"),
    RECETTE("Entrées"),
    DEPENSE("Sorties")
}

enum class PaymentFilter(val label: String) {
    ALL("Toutes caisses"),
    ESPECES("Espèces"),
    MOBILE_MONEY("Mobile Money"),
    BANQUE("Banque"),
    CHEQUE("Chèque")
}

data class TreasurySummary(
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val cashBalance: Double = 0.0,
    val mobileMoneyBalance: Double = 0.0,
    val bankBalance: Double = 0.0,
    val tithesTotal: Double = 0.0,
    val offeringsTotal: Double = 0.0,
    val specialCollectionsTotal: Double = 0.0,
    val socialTotal: Double = 0.0,
    val worksTotal: Double = 0.0,
    val billsTotal: Double = 0.0,
    val transactionsCount: Int = 0
)

class ChurchTreasuryViewModel(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _periodFilter = MutableStateFlow(PeriodFilter.ALL)
    val periodFilter: StateFlow<PeriodFilter> = _periodFilter.asStateFlow()

    private val _typeFilter = MutableStateFlow(TypeFilter.ALL)
    val typeFilter: StateFlow<TypeFilter> = _typeFilter.asStateFlow()

    private val _paymentFilter = MutableStateFlow(PaymentFilter.ALL)
    val paymentFilter: StateFlow<PaymentFilter> = _paymentFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _currencySymbol = MutableStateFlow("FCFA")
    val currencySymbol: StateFlow<String> = _currencySymbol.asStateFlow()

    private val _churchName = MutableStateFlow("Paroisse / Église Locale")
    val churchName: StateFlow<String> = _churchName.asStateFlow()

    val rawTransactionsState: StateFlow<UiState<List<ChurchTransaction>>> = repository.observeTransactions()
        .map<List<ChurchTransaction>, UiState<List<ChurchTransaction>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(it.localizedMessage ?: "Erreur de chargement des transactions")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val filteredTransactions: StateFlow<List<ChurchTransaction>> = combine(
        rawTransactionsState,
        _periodFilter,
        _typeFilter,
        _paymentFilter,
        _searchQuery
    ) { state, period, type, payment, query ->
        if (state !is UiState.Success) return@combine emptyList()
        val all = state.data
        all.filter { tx ->
            // Period filter
            val matchesPeriod = matchesPeriodFilter(tx.date, period)
            // Type filter
            val matchesType = when (type) {
                TypeFilter.ALL -> true
                TypeFilter.RECETTE -> tx.type == "RECETTE"
                TypeFilter.DEPENSE -> tx.type == "DEPENSE"
            }
            // Payment filter
            val matchesPayment = when (payment) {
                PaymentFilter.ALL -> true
                PaymentFilter.ESPECES -> tx.paymentMethod == "ESPECES"
                PaymentFilter.MOBILE_MONEY -> tx.paymentMethod == "MOBILE_MONEY"
                PaymentFilter.BANQUE -> tx.paymentMethod == "BANQUE"
                PaymentFilter.CHEQUE -> tx.paymentMethod == "CHEQUE"
            }
            // Query filter
            val matchesQuery = query.isBlank() ||
                    tx.categoryLabel.contains(query, ignoreCase = true) ||
                    tx.note.contains(query, ignoreCase = true) ||
                    tx.donorOrRecipient.contains(query, ignoreCase = true) ||
                    tx.referenceNumber.contains(query, ignoreCase = true)

            matchesPeriod && matchesType && matchesPayment && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    val globalSummary: StateFlow<TreasurySummary> = rawTransactionsState.map { state ->
        if (state !is UiState.Success) return@map TreasurySummary()
        calculateSummary(state.data)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = TreasurySummary()
    )

    val filteredSummary: StateFlow<TreasurySummary> = filteredTransactions.map { list ->
        calculateSummary(list)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = TreasurySummary()
    )

    fun setPeriodFilter(filter: PeriodFilter) { _periodFilter.value = filter }
    fun setTypeFilter(filter: TypeFilter) { _typeFilter.value = filter }
    fun setPaymentFilter(filter: PaymentFilter) { _paymentFilter.value = filter }
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setCurrencySymbol(symbol: String) { _currencySymbol.value = symbol }
    fun setChurchName(name: String) { _churchName.value = name }

    fun addTransaction(tx: ChurchTransaction, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.addTransaction(tx)
            if (result.isSuccess) {
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.localizedMessage ?: "Échec de l'ajout")
            }
        }
    }

    fun updateTransaction(tx: ChurchTransaction, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.updateTransaction(tx)
            if (result.isSuccess) {
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.localizedMessage ?: "Échec de modification")
            }
        }
    }

    fun deleteTransaction(id: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.deleteTransaction(id)
            if (result.isSuccess) {
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.localizedMessage ?: "Échec de suppression")
            }
        }
    }

    private fun calculateSummary(list: List<ChurchTransaction>): TreasurySummary {
        var totalIncome = 0.0
        var totalExpense = 0.0
        var cashIncome = 0.0
        var cashExpense = 0.0
        var mmIncome = 0.0
        var mmExpense = 0.0
        var bankIncome = 0.0
        var bankExpense = 0.0

        var tithes = 0.0
        var offerings = 0.0
        var specialCollections = 0.0
        var social = 0.0
        var works = 0.0
        var bills = 0.0

        for (tx in list) {
            if (tx.type == "RECETTE") {
                totalIncome += tx.amount
                when (tx.paymentMethod) {
                    "ESPECES" -> cashIncome += tx.amount
                    "MOBILE_MONEY" -> mmIncome += tx.amount
                    "BANQUE" -> bankIncome += tx.amount
                }
                when (tx.category) {
                    "DIME" -> tithes += tx.amount
                    "OFFRANDE" -> offerings += tx.amount
                    "QUETE_SPECIALE" -> specialCollections += tx.amount
                }
            } else {
                totalExpense += tx.amount
                when (tx.paymentMethod) {
                    "ESPECES" -> cashExpense += tx.amount
                    "MOBILE_MONEY" -> mmExpense += tx.amount
                    "BANQUE" -> bankExpense += tx.amount
                }
                when (tx.category) {
                    "SOCIAL" -> social += tx.amount
                    "TRAVAUX" -> works += tx.amount
                    "FACTURE" -> bills += tx.amount
                }
            }
        }

        return TreasurySummary(
            totalBalance = totalIncome - totalExpense,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            cashBalance = cashIncome - cashExpense,
            mobileMoneyBalance = mmIncome - mmExpense,
            bankBalance = bankIncome - bankExpense,
            tithesTotal = tithes,
            offeringsTotal = offerings,
            specialCollectionsTotal = specialCollections,
            socialTotal = social,
            worksTotal = works,
            billsTotal = bills,
            transactionsCount = list.size
        )
    }

    private fun matchesPeriodFilter(dateStr: String, filter: PeriodFilter): Boolean {
        if (filter == PeriodFilter.ALL || dateStr.isBlank()) return true
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return when (filter) {
            PeriodFilter.TODAY -> dateStr == today
            PeriodFilter.THIS_MONTH -> {
                val currentMonth = today.substring(0, minOf(7, today.length))
                dateStr.startsWith(currentMonth)
            }
            PeriodFilter.THIS_YEAR -> {
                val currentYear = today.substring(0, minOf(4, today.length))
                dateStr.startsWith(currentYear)
            }
            PeriodFilter.THIS_WEEK -> {
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val txDate = sdf.parse(dateStr) ?: return true
                    val cal = Calendar.getInstance()
                    val currentWeek = cal.get(Calendar.WEEK_OF_YEAR)
                    val currentYear = cal.get(Calendar.YEAR)
                    cal.time = txDate
                    cal.get(Calendar.WEEK_OF_YEAR) == currentWeek && cal.get(Calendar.YEAR) == currentYear
                } catch (e: Exception) {
                    true
                }
            }
            PeriodFilter.ALL -> true
        }
    }

    fun formatMoney(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale.FRANCE)
        formatter.maximumFractionDigits = 0
        return "${formatter.format(amount)} ${_currencySymbol.value}"
    }

    fun generateServiceReport(targetDate: String): String {
        val state = rawTransactionsState.value
        val list = if (state is UiState.Success) {
            state.data.filter { it.date == targetDate }
        } else emptyList()

        val summary = calculateSummary(list)
        val sb = StringBuilder()
        sb.appendLine("⛪ *RAPPORT FINANCIER DU CULTE*")
        sb.appendLine("Paroisse : ${_churchName.value}")
        sb.appendLine("Date : $targetDate")
        sb.appendLine("--------------------------------")
        sb.appendLine("💰 *RECETTES COLLECTÉES :*")
        sb.appendLine("• Dîmes : ${formatMoney(summary.tithesTotal)}")
        sb.appendLine("• Offrandes du culte : ${formatMoney(summary.offeringsTotal)}")
        sb.appendLine("• Quêtes spéciales : ${formatMoney(summary.specialCollectionsTotal)}")
        val otherIncome = summary.totalIncome - (summary.tithesTotal + summary.offeringsTotal + summary.specialCollectionsTotal)
        if (otherIncome > 0) {
            sb.appendLine("• Autres dons / libéralités : ${formatMoney(otherIncome)}")
        }
        sb.appendLine("*TOTAL ENTRÉES :* ${formatMoney(summary.totalIncome)}")
        sb.appendLine("--------------------------------")
        if (summary.totalExpense > 0) {
            sb.appendLine("💸 *DÉPENSES DU JOUR :*")
            list.filter { it.type == "DEPENSE" }.forEach {
                sb.appendLine("• ${it.categoryLabel} : ${formatMoney(it.amount)} (${it.note.ifBlank { "Dépense autorisée" }})")
            }
            sb.appendLine("*TOTAL DÉPENSES :* ${formatMoney(summary.totalExpense)}")
            sb.appendLine("--------------------------------")
        }
        sb.appendLine("📊 *SOLDE NET DU CULTE :* ${formatMoney(summary.totalBalance)}")
        sb.appendLine("💵 Espèces en caisse : ${formatMoney(summary.cashBalance)}")
        sb.appendLine("📱 Mobile Money : ${formatMoney(summary.mobileMoneyBalance)}")
        sb.appendLine("--------------------------------")
        sb.appendLine("Certifié exact par la trésorerie paroissiale.")
        return sb.toString()
    }
}

class ChurchTreasuryViewModelFactory(
    private val repository: TransactionRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ChurchTreasuryViewModel(repository) as T
    }
}

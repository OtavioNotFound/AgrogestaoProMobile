package com.agrogestao.pro.ui.safras

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.agrogestao.pro.data.local.entities.CropEntity
import com.agrogestao.pro.data.local.entities.FinancialEntity
import com.agrogestao.pro.data.local.entities.TransactionType
import com.agrogestao.pro.data.repository.AgroRepository
import com.agrogestao.pro.domain.FinancialFilterCriteria
import com.agrogestao.pro.domain.calculateFinancialSummary
import com.agrogestao.pro.domain.moneyToCents
import com.agrogestao.pro.domain.filterTransactions
import com.agrogestao.pro.domain.financialFilterCategories
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SafrasUiState(
    val safras: List<CropEntity> = emptyList(),
    val transacoes: List<FinancialEntity> = emptyList(),
    val totalTransactions: Int = 0,
    val categories: List<String> = emptyList(),
    val filters: FinancialFilterCriteria = FinancialFilterCriteria(),
    val filteredIncome: Double = 0.0,
    val filteredExpenses: Double = 0.0,
    val filteredBalance: Double = 0.0
)

class SafrasViewModel(private val repository: AgroRepository) : ViewModel() {
    private val filters = MutableStateFlow(FinancialFilterCriteria())

    val uiState: StateFlow<SafrasUiState> = combine(
        repository.allCrops,
        repository.allTransactions,
        filters
    ) { crops, transactions, criteria ->
        val filteredTransactions = filterTransactions(transactions, criteria)
        val summary = calculateFinancialSummary(filteredTransactions)
        SafrasUiState(
            safras = crops,
            transacoes = filteredTransactions,
            totalTransactions = transactions.size,
            categories = financialFilterCategories(transactions),
            filters = criteria,
            filteredIncome = summary.income,
            filteredExpenses = summary.expenses,
            filteredBalance = summary.balance
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SafrasUiState()
    )

    fun saveCrop(
        existing: CropEntity?,
        nome: String,
        area: Double,
        inicio: String,
        colheita: String,
        progresso: Int,
        statusManejo: String
    ) {
        viewModelScope.launch {
            val crop = (existing ?: CropEntity(
                nomeCultura = nome.trim(),
                areaHectares = area,
                dataInicio = inicio,
                previsaoColheita = colheita,
                progressoPercentual = progresso,
                statusManejo = statusManejo.trim()
            )).copy(
                nomeCultura = nome.trim(),
                areaHectares = area,
                dataInicio = inicio,
                previsaoColheita = colheita,
                progressoPercentual = progresso,
                statusManejo = statusManejo.trim()
            )
            if (existing == null) repository.insertCrop(crop) else repository.updateCrop(crop)
        }
    }

    fun saveTransaction(
        existing: FinancialEntity?,
        descricao: String,
        valor: Double,
        tipo: TransactionType,
        categoria: String,
        data: String,
        cropCloudId: String?
    ) {
        viewModelScope.launch {
            val transaction = (existing ?: FinancialEntity(
                descricao = descricao.trim(),
                valor = valor,
                tipo = tipo,
                data = data,
                categoria = categoria.trim(),
                cropCloudId = cropCloudId
            )).copy(
                descricao = descricao.trim(),
                valorCentavos = moneyToCents(valor),
                tipo = tipo,
                data = data,
                categoria = categoria.trim(),
                cropCloudId = cropCloudId
            )
            if (existing == null) {
                repository.insertTransaction(transaction)
            } else {
                repository.updateTransaction(transaction)
            }
        }
    }

    fun deleteCrop(cropId: Long) {
        viewModelScope.launch {
            repository.deleteCrop(cropId)
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

    fun applyFinancialFilters(criteria: FinancialFilterCriteria) {
        filters.value = criteria
    }

    fun clearFinancialFilters() {
        filters.value = FinancialFilterCriteria()
    }
}

class SafrasViewModelFactory(private val repository: AgroRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SafrasViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SafrasViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

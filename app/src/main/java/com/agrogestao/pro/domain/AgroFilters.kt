package com.agrogestao.pro.domain

import com.agrogestao.pro.data.local.entities.FinancialEntity
import com.agrogestao.pro.data.local.entities.TaskEntity
import com.agrogestao.pro.data.local.entities.TransactionType

const val FILTER_WITHOUT_CROP = "__agrogestao_without_crop__"

data class TaskFilterCriteria(
    val fromDate: String? = null,
    val toDate: String? = null,
    val cropCloudId: String? = null,
    val category: String? = null
) {
    val activeCount: Int
        get() = listOf(fromDate, toDate, cropCloudId, category).count { it != null }
}

data class FinancialFilterCriteria(
    val fromDate: String? = null,
    val toDate: String? = null,
    val cropCloudId: String? = null,
    val category: String? = null,
    val transactionType: TransactionType? = null
) {
    val activeCount: Int
        get() = listOf(fromDate, toDate, cropCloudId, category, transactionType)
            .count { it != null }
}

fun filterTasks(
    tasks: List<TaskEntity>,
    criteria: TaskFilterCriteria
): List<TaskEntity> = tasks.filter { task ->
    matchesPeriod(task.dataLimite, criteria.fromDate, criteria.toDate) &&
        matchesCrop(task.cropCloudId, criteria.cropCloudId) &&
        matchesCategory(task.categoria, criteria.category)
}

fun filterTransactions(
    transactions: List<FinancialEntity>,
    criteria: FinancialFilterCriteria
): List<FinancialEntity> = transactions.filter { transaction ->
    matchesPeriod(transaction.data, criteria.fromDate, criteria.toDate) &&
        matchesCrop(transaction.cropCloudId, criteria.cropCloudId) &&
        matchesCategory(transaction.categoria, criteria.category) &&
        (criteria.transactionType == null || transaction.tipo == criteria.transactionType)
}

fun taskFilterCategories(tasks: List<TaskEntity>): List<String> =
    distinctCategories(tasks.map(TaskEntity::categoria))

fun financialFilterCategories(transactions: List<FinancialEntity>): List<String> =
    distinctCategories(transactions.map(FinancialEntity::categoria))

private fun matchesPeriod(date: String, fromDate: String?, toDate: String?): Boolean {
    if (fromDate == null && toDate == null) return true
    if (isoDateParts(date) == null) return false
    return (fromDate == null || date >= fromDate) && (toDate == null || date <= toDate)
}

private fun matchesCrop(recordCropCloudId: String?, selectedCropCloudId: String?): Boolean =
    when (selectedCropCloudId) {
        null -> true
        FILTER_WITHOUT_CROP -> recordCropCloudId == null
        else -> recordCropCloudId == selectedCropCloudId
    }

private fun matchesCategory(recordCategory: String, selectedCategory: String?): Boolean =
    selectedCategory == null || recordCategory.equals(selectedCategory, ignoreCase = true)

private fun distinctCategories(values: List<String>): List<String> = values
    .map(String::trim)
    .filter(String::isNotBlank)
    .distinctBy(String::lowercase)
    .sortedWith(String.CASE_INSENSITIVE_ORDER)

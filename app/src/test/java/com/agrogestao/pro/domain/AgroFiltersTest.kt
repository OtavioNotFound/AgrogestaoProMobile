package com.agrogestao.pro.domain

import com.agrogestao.pro.data.local.entities.FinancialEntity
import com.agrogestao.pro.data.local.entities.TaskEntity
import com.agrogestao.pro.data.local.entities.TaskStatus
import com.agrogestao.pro.data.local.entities.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgroFiltersTest {
    @Test
    fun `task filter combines inclusive period crop and category`() {
        val tasks = listOf(
            task(1, "2026-08-01", "Manejo", "crop-a"),
            task(2, "2026-08-15", "Irrigação", "crop-a"),
            task(3, "2026-08-31", "irrigação", "crop-b"),
            task(4, "2026-09-01", "Irrigação", "crop-a")
        )

        val result = filterTasks(
            tasks,
            TaskFilterCriteria(
                fromDate = "2026-08-15",
                toDate = "2026-08-31",
                cropCloudId = "crop-a",
                category = "IRRIGAÇÃO"
            )
        )

        assertEquals(listOf(2L), result.map(TaskEntity::id))
    }

    @Test
    fun `task filter can select records without crop`() {
        val tasks = listOf(
            task(1, "2026-08-01", "Manejo", null),
            task(2, "2026-08-01", "Manejo", "crop-a")
        )

        assertEquals(
            listOf(1L),
            filterTasks(
                tasks,
                TaskFilterCriteria(cropCloudId = FILTER_WITHOUT_CROP)
            ).map(TaskEntity::id)
        )
    }

    @Test
    fun `financial filter combines period crop category and type`() {
        val transactions = listOf(
            transaction(1, "2026-07-31", "Venda", "crop-a", TransactionType.ENTRADA),
            transaction(2, "2026-08-01", "Venda", "crop-a", TransactionType.ENTRADA),
            transaction(3, "2026-08-20", "Venda", "crop-b", TransactionType.ENTRADA),
            transaction(4, "2026-08-31", "Venda", "crop-a", TransactionType.SAIDA)
        )

        val result = filterTransactions(
            transactions,
            FinancialFilterCriteria(
                fromDate = "2026-08-01",
                toDate = "2026-08-31",
                cropCloudId = "crop-a",
                category = "venda",
                transactionType = TransactionType.ENTRADA
            )
        )

        assertEquals(listOf(2L), result.map(FinancialEntity::id))
    }

    @Test
    fun `invalid record dates are excluded only when period is active`() {
        val invalid = task(1, "data-antiga-inválida", "Manejo", null)

        assertEquals(listOf(invalid), filterTasks(listOf(invalid), TaskFilterCriteria()))
        assertTrue(
            filterTasks(
                listOf(invalid),
                TaskFilterCriteria(fromDate = "2026-01-01")
            ).isEmpty()
        )
    }

    @Test
    fun `categories ignore blanks duplicates and casing`() {
        val tasks = listOf(
            task(1, "2026-08-01", " Manejo ", null),
            task(2, "2026-08-01", "manejo", null),
            task(3, "2026-08-01", "", null),
            task(4, "2026-08-01", "Irrigação", null)
        )

        assertEquals(listOf("Irrigação", "Manejo"), taskFilterCategories(tasks))
    }

    @Test
    fun `active count reports every selected criterion`() {
        assertEquals(
            4,
            TaskFilterCriteria("2026-01-01", "2026-12-31", "crop-a", "Manejo")
                .activeCount
        )
        assertEquals(
            5,
            FinancialFilterCriteria(
                "2026-01-01",
                "2026-12-31",
                "crop-a",
                "Venda",
                TransactionType.ENTRADA
            ).activeCount
        )
    }

    private fun task(
        id: Long,
        date: String,
        category: String,
        cropCloudId: String?
    ) = TaskEntity(
        id = id,
        titulo = "Tarefa $id",
        descricao = "Teste",
        categoria = category,
        dataLimite = date,
        status = TaskStatus.A_FAZER,
        cropCloudId = cropCloudId
    )

    private fun transaction(
        id: Long,
        date: String,
        category: String,
        cropCloudId: String?,
        type: TransactionType
    ) = FinancialEntity(
        id = id,
        descricao = "Lançamento $id",
        valor = 100.0,
        tipo = type,
        data = date,
        categoria = category,
        cropCloudId = cropCloudId
    )
}

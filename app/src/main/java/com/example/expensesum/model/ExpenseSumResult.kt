package com.example.expensesum.model

data class ExpenseSumResult(
    val expenses: List<ExpenseItem>,
    val totalMinorUnits: Long,
    val count: Int,
    val averageMinorUnits: Long,
    val largestMinorUnits: Long?,
    val smallestMinorUnits: Long?,
    val largestExpenseId: String?,
    val smallestExpenseId: String?,
    val currency: CurrencyOption
)

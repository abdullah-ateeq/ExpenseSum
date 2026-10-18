package com.example.expensesum.model

data class ExpenseSumHistory(
    val id: String,
    val title: String,
    val expenses: List<ExpenseItem>,
    val currencyCode: String,
    val currencySymbol: String,
    val calculatedAt: Long,
    val currencyName: String = currencyCode,
    val currencyDecimalPlaces: Int = if (currencyCode == "JPY") 0 else 2
)

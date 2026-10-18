package com.example.expensesum.model

data class ExpenseSumUiState(
    val title: String = "",
    val currency: CurrencyOption = CurrencyOption("USD", "$", "US Dollar", 2),
    val expenses: List<ExpenseItem> = emptyList(),
    val quickAddInput: String = "",
    val result: ExpenseSumResult? = null,
    val history: List<ExpenseSumHistory> = emptyList(),
    val errorMessage: String? = null,
    val isCalculating: Boolean = false,
    val calculationState: CalculationState = CalculationState.Idle,
    val settings: AppSettings = AppSettings(),
    val pendingCurrencyChange: CurrencyOption? = null,
    val showClearAllConfirmation: Boolean = false,
    val pendingHistoryDeleteId: String? = null,
    val showClearHistoryConfirmation: Boolean = false,
    val showResetSettingsConfirmation: Boolean = false,
    val editingExpense: ExpenseItem? = null,
    val showAddExpenseDialog: Boolean = false,
    val showCustomCurrencyDialog: Boolean = false,
    val statusMessage: String? = null
)

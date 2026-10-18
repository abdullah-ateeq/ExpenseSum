package com.example.expensesum.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.expensesum.domain.CurrencyRepository
import com.example.expensesum.domain.ExpenseSumCalculationException
import com.example.expensesum.domain.ExpenseSumCalculator
import com.example.expensesum.domain.ExpenseSumFormatter
import com.example.expensesum.domain.ExpenseSumValidator
import com.example.expensesum.domain.MoneyParser
import com.example.expensesum.domain.ParseResult
import com.example.expensesum.domain.ValidationResult
import com.example.expensesum.model.CalculationState
import com.example.expensesum.model.CurrencyOption
import com.example.expensesum.model.ExpenseItem
import com.example.expensesum.model.ExpenseSumHistory
import com.example.expensesum.model.ExpenseSumResult
import com.example.expensesum.model.ExpenseSumUiState
import com.example.expensesum.model.ThemeMode
import com.example.expensesum.repository.ExpenseSumHistoryRepository
import com.example.expensesum.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class ExpenseSumViewModel(
    private val settingsRepository: SettingsRepository,
    private val historyRepository: ExpenseSumHistoryRepository
) : ViewModel() {

    private val _uiState: MutableStateFlow<ExpenseSumUiState>
    val uiState: StateFlow<ExpenseSumUiState>

    init {
        val loadedSettings = settingsRepository.loadSettings()
        val initialDraft = settingsRepository.loadInitialDraft(loadedSettings)
        val initialHistory = historyRepository.loadHistory()
        val calcOutcome = runCalculation(initialDraft.expenses, initialDraft.currency)

        _uiState = MutableStateFlow(
            ExpenseSumUiState(
                title = initialDraft.title,
                currency = initialDraft.currency,
                expenses = initialDraft.expenses,
                quickAddInput = "",
                result = calcOutcome.result,
                history = initialHistory,
                errorMessage = calcOutcome.errorMessage,
                isCalculating = false,
                calculationState = calcOutcome.calculationState,
                settings = loadedSettings
            )
        )
        uiState = _uiState.asStateFlow()
    }

    private data class CalcOutcome(
        val result: ExpenseSumResult?,
        val calculationState: CalculationState,
        val errorMessage: String?
    )

    private fun runCalculation(
        expenses: List<ExpenseItem>,
        currency: CurrencyOption
    ): CalcOutcome {
        return try {
            val result = ExpenseSumCalculator.calculate(expenses, currency)
            val state = if (expenses.isEmpty()) CalculationState.Idle else CalculationState.Calculated
            CalcOutcome(
                result = result,
                calculationState = state,
                errorMessage = null
            )
        } catch (e: ExpenseSumCalculationException) {
            val errorMsg = e.message ?: "Calculation error."
            CalcOutcome(
                result = null,
                calculationState = CalculationState.Error(errorMsg),
                errorMessage = errorMsg
            )
        }
    }

    private fun updateExpensesAndRecalculate(
        newExpenses: List<ExpenseItem>,
        newTitle: String = _uiState.value.title,
        newCurrency: CurrencyOption = _uiState.value.currency,
        clearQuickInput: Boolean = false,
        statusMessage: String? = null
    ): Boolean {
        val validation = ExpenseSumValidator.validateExpenseList(newExpenses)
        if (validation is ValidationResult.Invalid) {
            _uiState.update {
                it.copy(
                    errorMessage = validation.message,
                    calculationState = CalculationState.Error(validation.message)
                )
            }
            return false
        }

        val calc = runCalculation(newExpenses, newCurrency)
        if (calc.calculationState is CalculationState.Error) {
            _uiState.update {
                it.copy(
                    errorMessage = calc.errorMessage,
                    calculationState = calc.calculationState
                )
            }
            return false
        }

        _uiState.update { current ->
            current.copy(
                title = newTitle,
                currency = newCurrency,
                expenses = newExpenses,
                quickAddInput = if (clearQuickInput) "" else current.quickAddInput,
                result = calc.result,
                errorMessage = null,
                isCalculating = false,
                calculationState = calc.calculationState,
                editingExpense = null,
                showAddExpenseDialog = false,
                statusMessage = statusMessage
            )
        }

        if (_uiState.value.settings.preserveLastList) {
            settingsRepository.saveLastList(newTitle, newCurrency, newExpenses)
        }
        return true
    }

    fun updateTitle(newTitle: String) {
        val sanitized = newTitle.take(ExpenseSumValidator.MAX_TITLE_LENGTH)
        _uiState.update { it.copy(title = sanitized, errorMessage = null) }
        if (_uiState.value.settings.preserveLastList) {
            settingsRepository.saveLastList(
                title = sanitized,
                currency = _uiState.value.currency,
                expenses = _uiState.value.expenses
            )
        }
    }

    fun updateQuickAddInput(input: String) {
        // Allow digits, one decimal point, commas, or minus (so user gets immediate negative validation feedback if needed)
        if (input.length > 20) return
        val error = if (input.trim().startsWith("-")) {
            "Expense amount cannot be negative."
        } else {
            null
        }
        _uiState.update {
            it.copy(
                quickAddInput = input,
                errorMessage = error
            )
        }
    }

    fun quickAddExpense(): Boolean {
        val state = _uiState.value
        val canAdd = ExpenseSumValidator.validateCanAddExpense(state.expenses.size)
        if (canAdd is ValidationResult.Invalid) {
            _uiState.update { it.copy(errorMessage = canAdd.message) }
            return false
        }

        return when (val parsed = MoneyParser.parse(state.quickAddInput, state.currency)) {
            is ParseResult.Error -> {
                _uiState.update { it.copy(errorMessage = parsed.message) }
                false
            }
            is ParseResult.Success -> {
                val nextIndex = state.expenses.size + 1
                val newItem = ExpenseItem(
                    id = UUID.randomUUID().toString(),
                    label = "Expense $nextIndex",
                    amountMinorUnits = parsed.minorUnits
                )
                updateExpensesAndRecalculate(
                    newExpenses = state.expenses + newItem,
                    clearQuickInput = true
                )
            }
        }
    }

    fun addExpense(label: String, amountInput: String): Boolean {
        val state = _uiState.value
        val canAdd = ExpenseSumValidator.validateCanAddExpense(state.expenses.size)
        if (canAdd is ValidationResult.Invalid) {
            _uiState.update { it.copy(errorMessage = canAdd.message) }
            return false
        }

        val labelValidation = ExpenseSumValidator.validateLabel(label)
        if (labelValidation is ValidationResult.Invalid) {
            _uiState.update { it.copy(errorMessage = labelValidation.message) }
            return false
        }

        return when (val parsed = MoneyParser.parse(amountInput, state.currency)) {
            is ParseResult.Error -> {
                _uiState.update { it.copy(errorMessage = parsed.message) }
                false
            }
            is ParseResult.Success -> {
                val nextIndex = state.expenses.size + 1
                val cleanLabel = ExpenseSumValidator.sanitizeLabel(label)
                    .ifEmpty { "Expense $nextIndex" }
                val newItem = ExpenseItem(
                    id = UUID.randomUUID().toString(),
                    label = cleanLabel,
                    amountMinorUnits = parsed.minorUnits
                )
                updateExpensesAndRecalculate(
                    newExpenses = state.expenses + newItem
                )
            }
        }
    }

    fun addExpenseMinorUnits(label: String, amountMinorUnits: Long): Boolean {
        val state = _uiState.value
        val canAdd = ExpenseSumValidator.validateCanAddExpense(state.expenses.size)
        if (canAdd is ValidationResult.Invalid) {
            _uiState.update { it.copy(errorMessage = canAdd.message) }
            return false
        }
        val amountValidation = ExpenseSumValidator.validateAmountMinorUnits(amountMinorUnits)
        if (amountValidation is ValidationResult.Invalid) {
            _uiState.update { it.copy(errorMessage = amountValidation.message) }
            return false
        }
        val nextIndex = state.expenses.size + 1
        val cleanLabel = ExpenseSumValidator.sanitizeLabel(label)
            .ifEmpty { "Expense $nextIndex" }
        val newItem = ExpenseItem(
            id = UUID.randomUUID().toString(),
            label = cleanLabel,
            amountMinorUnits = amountMinorUnits
        )
        return updateExpensesAndRecalculate(newExpenses = state.expenses + newItem)
    }

    fun addPresetExpense(majorAmount: Long): Boolean {
        val state = _uiState.value
        val minorUnits = try {
            CurrencyRepository.scaleMajorAmountToMinorUnits(majorAmount, state.currency)
        } catch (e: ArithmeticException) {
            _uiState.update { it.copy(errorMessage = "Amount is too large.") }
            return false
        }
        val nextIndex = state.expenses.size + 1
        return addExpenseMinorUnits(
            label = "Expense $nextIndex",
            amountMinorUnits = minorUnits
        )
    }

    fun editExpense(id: String, newLabel: String, newAmountInput: String): Boolean {
        val state = _uiState.value
        val index = state.expenses.indexOfFirst { it.id == id }
        if (index == -1) return false

        val labelValidation = ExpenseSumValidator.validateLabel(newLabel)
        if (labelValidation is ValidationResult.Invalid) {
            _uiState.update { it.copy(errorMessage = labelValidation.message) }
            return false
        }

        // If temporarily incomplete while live editing, do not corrupt the existing calculated total
        if (MoneyParser.isTemporarilyIncomplete(newAmountInput)) {
            _uiState.update { it.copy(errorMessage = "Enter a valid expense amount.") }
            return false
        }

        return when (val parsed = MoneyParser.parse(newAmountInput, state.currency)) {
            is ParseResult.Error -> {
                _uiState.update { it.copy(errorMessage = parsed.message) }
                false
            }
            is ParseResult.Success -> {
                val cleanLabel = ExpenseSumValidator.sanitizeLabel(newLabel)
                    .ifEmpty { "Expense ${index + 1}" }
                val updatedList = state.expenses.toMutableList().apply {
                    this[index] = this[index].copy(
                        label = cleanLabel,
                        amountMinorUnits = parsed.minorUnits
                    )
                }
                updateExpensesAndRecalculate(newExpenses = updatedList)
            }
        }
    }

    fun editExpenseMinorUnits(id: String, newLabel: String, newAmountMinorUnits: Long): Boolean {
        val state = _uiState.value
        val index = state.expenses.indexOfFirst { it.id == id }
        if (index == -1) return false

        val amountValidation = ExpenseSumValidator.validateAmountMinorUnits(newAmountMinorUnits)
        if (amountValidation is ValidationResult.Invalid) {
            _uiState.update { it.copy(errorMessage = amountValidation.message) }
            return false
        }

        val cleanLabel = ExpenseSumValidator.sanitizeLabel(newLabel)
            .ifEmpty { "Expense ${index + 1}" }
        val updatedList = state.expenses.toMutableList().apply {
            this[index] = this[index].copy(
                label = cleanLabel,
                amountMinorUnits = newAmountMinorUnits
            )
        }
        return updateExpensesAndRecalculate(newExpenses = updatedList)
    }

    fun deleteExpense(id: String) {
        val state = _uiState.value
        val updatedList = state.expenses.filterNot { it.id == id }
        updateExpensesAndRecalculate(newExpenses = updatedList)
    }

    fun requestClearAll() {
        val state = _uiState.value
        if (state.expenses.isEmpty() && state.title.isBlank()) return
        if (state.settings.confirmClearAll) {
            _uiState.update { it.copy(showClearAllConfirmation = true) }
        } else {
            confirmClearAll()
        }
    }

    fun confirmClearAll() {
        val state = _uiState.value
        val emptyCalc = runCalculation(emptyList(), state.currency)
        _uiState.update {
            it.copy(
                title = "",
                expenses = emptyList(),
                quickAddInput = "",
                result = emptyCalc.result,
                errorMessage = null,
                calculationState = CalculationState.Idle,
                showClearAllConfirmation = false,
                editingExpense = null,
                statusMessage = "Cleared all expenses"
            )
        }
        if (state.settings.preserveLastList) {
            settingsRepository.saveLastList("", state.currency, emptyList())
        }
    }

    fun dismissClearAllConfirmation() {
        _uiState.update { it.copy(showClearAllConfirmation = false) }
    }

    fun calculateTotals() {
        val state = _uiState.value
        val calc = runCalculation(state.expenses, state.currency)
        _uiState.update {
            it.copy(
                result = calc.result,
                errorMessage = calc.errorMessage,
                calculationState = calc.calculationState,
                isCalculating = false
            )
        }
    }

    fun requestCurrencyChange(newCurrency: CurrencyOption) {
        val state = _uiState.value
        if (state.currency == newCurrency) return
        if (state.expenses.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    pendingCurrencyChange = newCurrency,
                    showCustomCurrencyDialog = false
                )
            }
        } else {
            applyCurrencyChange(newCurrency)
        }
    }

    fun confirmCurrencyChange() {
        val pending = _uiState.value.pendingCurrencyChange ?: return
        applyCurrencyChange(pending)
    }

    fun cancelCurrencyChange() {
        _uiState.update { it.copy(pendingCurrencyChange = null) }
    }

    private fun applyCurrencyChange(newCurrency: CurrencyOption) {
        val state = _uiState.value
        val oldDecimals = state.currency.decimalPlaces.coerceIn(0, 4)
        val newDecimals = newCurrency.decimalPlaces.coerceIn(0, 4)

        // Retain numerical major values and update their currency interpretation if decimals differ
        val convertedExpenses = state.expenses.map { item ->
            val adjustedMinor = if (oldDecimals == newDecimals) {
                item.amountMinorUnits
            } else if (newDecimals > oldDecimals) {
                var factor = 1L
                repeat(newDecimals - oldDecimals) { factor *= 10L }
                try {
                    Math.multiplyExact(item.amountMinorUnits, factor)
                        .coerceAtMost(ExpenseSumValidator.MAX_AMOUNT_MINOR_UNITS)
                } catch (e: ArithmeticException) {
                    ExpenseSumValidator.MAX_AMOUNT_MINOR_UNITS
                }
            } else {
                var divisor = 1L
                repeat(oldDecimals - newDecimals) { divisor *= 10L }
                item.amountMinorUnits / divisor
            }
            item.copy(amountMinorUnits = adjustedMinor)
        }

        val calc = runCalculation(convertedExpenses, newCurrency)
        val updatedSettings = state.settings.copy(defaultCurrency = newCurrency)
        settingsRepository.saveSettings(updatedSettings)
        if (updatedSettings.preserveLastList) {
            settingsRepository.saveLastList(state.title, newCurrency, convertedExpenses)
        }

        _uiState.update {
            it.copy(
                currency = newCurrency,
                expenses = convertedExpenses,
                result = calc.result,
                errorMessage = calc.errorMessage,
                calculationState = calc.calculationState,
                pendingCurrencyChange = null,
                showCustomCurrencyDialog = false,
                settings = updatedSettings
            )
        }
    }

    fun saveToHistory(): Boolean {
        val state = _uiState.value
        if (state.expenses.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Add at least one expense before saving to history.") }
            return false
        }

        val (updatedHistory, wasAdded) = historyRepository.saveToHistory(
            title = state.title,
            expenses = state.expenses,
            currency = state.currency
        )

        _uiState.update {
            it.copy(
                history = updatedHistory,
                errorMessage = null,
                statusMessage = if (wasAdded) "Saved to history" else "Already saved in recent history"
            )
        }
        return wasAdded
    }

    fun loadHistory() {
        val loaded = historyRepository.loadHistory()
        _uiState.update { it.copy(history = loaded) }
    }

    fun restoreHistory(historyItem: ExpenseSumHistory) {
        val restoredCurrency = CurrencyOption(
            code = historyItem.currencyCode,
            symbol = historyItem.currencySymbol,
            name = historyItem.currencyName,
            decimalPlaces = historyItem.currencyDecimalPlaces
        )
        val restoredTitle = if (historyItem.title == "Untitled Expense List") "" else historyItem.title
        val restoredExpenses = historyItem.expenses.map { it.copy() }
        val calc = runCalculation(restoredExpenses, restoredCurrency)

        _uiState.update {
            it.copy(
                title = restoredTitle,
                currency = restoredCurrency,
                expenses = restoredExpenses,
                quickAddInput = "",
                result = calc.result,
                errorMessage = calc.errorMessage,
                calculationState = calc.calculationState,
                statusMessage = "Restored \"${historyItem.title}\""
            )
        }

        if (_uiState.value.settings.preserveLastList) {
            settingsRepository.saveLastList(restoredTitle, restoredCurrency, restoredExpenses)
        }
    }

    fun requestDeleteHistoryItem(id: String) {
        val state = _uiState.value
        if (state.settings.confirmHistoryDelete) {
            _uiState.update { it.copy(pendingHistoryDeleteId = id) }
        } else {
            confirmDeleteHistoryItem(id)
        }
    }

    fun confirmDeleteHistoryItem(id: String? = _uiState.value.pendingHistoryDeleteId) {
        if (id == null) return
        val updated = historyRepository.deleteHistoryItem(id)
        _uiState.update {
            it.copy(
                history = updated,
                pendingHistoryDeleteId = null,
                statusMessage = "History item removed"
            )
        }
    }

    fun dismissDeleteHistoryConfirmation() {
        _uiState.update { it.copy(pendingHistoryDeleteId = null) }
    }

    fun requestClearHistory() {
        if (_uiState.value.history.isEmpty()) return
        _uiState.update { it.copy(showClearHistoryConfirmation = true) }
    }

    fun confirmClearHistory() {
        val cleared = historyRepository.clearHistory()
        _uiState.update {
            it.copy(
                history = cleared,
                showClearHistoryConfirmation = false,
                statusMessage = "History cleared"
            )
        }
    }

    fun dismissClearHistoryConfirmation() {
        _uiState.update { it.copy(showClearHistoryConfirmation = false) }
    }

    // Settings updates
    fun updateThemeMode(themeMode: ThemeMode) {
        val updated = _uiState.value.settings.copy(themeMode = themeMode)
        settingsRepository.saveSettings(updated)
        _uiState.update { it.copy(settings = updated) }
    }

    fun updateDynamicColor(enabled: Boolean) {
        val updated = _uiState.value.settings.copy(dynamicColor = enabled)
        settingsRepository.saveSettings(updated)
        _uiState.update { it.copy(settings = updated) }
    }

    fun updateHapticFeedback(enabled: Boolean) {
        val updated = _uiState.value.settings.copy(hapticFeedback = enabled)
        settingsRepository.saveSettings(updated)
        _uiState.update { it.copy(settings = updated) }
    }

    fun updateAnimations(enabled: Boolean) {
        val updated = _uiState.value.settings.copy(animations = enabled)
        settingsRepository.saveSettings(updated)
        _uiState.update { it.copy(settings = updated) }
    }

    fun updateDefaultCurrency(currency: CurrencyOption) {
        val updated = _uiState.value.settings.copy(defaultCurrency = currency)
        settingsRepository.saveSettings(updated)
        _uiState.update { it.copy(settings = updated) }
        requestCurrencyChange(currency)
    }

    fun updateConfirmClearAll(enabled: Boolean) {
        val updated = _uiState.value.settings.copy(confirmClearAll = enabled)
        settingsRepository.saveSettings(updated)
        _uiState.update { it.copy(settings = updated) }
    }

    fun updateConfirmHistoryDelete(enabled: Boolean) {
        val updated = _uiState.value.settings.copy(confirmHistoryDelete = enabled)
        settingsRepository.saveSettings(updated)
        _uiState.update { it.copy(settings = updated) }
    }

    fun updatePreserveLastList(enabled: Boolean) {
        val state = _uiState.value
        val updated = state.settings.copy(preserveLastList = enabled)
        settingsRepository.saveSettings(updated)
        if (enabled) {
            settingsRepository.saveLastList(state.title, state.currency, state.expenses)
        } else {
            settingsRepository.clearSavedDraft(updated.defaultCurrency)
        }
        _uiState.update { it.copy(settings = updated) }
    }

    fun requestResetSettings() {
        _uiState.update { it.copy(showResetSettingsConfirmation = true) }
    }

    fun confirmResetSettings() {
        val defaults = settingsRepository.resetSettings()
        val state = _uiState.value
        val calc = runCalculation(state.expenses, defaults.defaultCurrency)
        _uiState.update {
            it.copy(
                settings = defaults,
                currency = defaults.defaultCurrency,
                result = calc.result,
                showResetSettingsConfirmation = false,
                statusMessage = "Settings reset to defaults"
            )
        }
    }

    fun dismissResetSettingsConfirmation() {
        _uiState.update { it.copy(showResetSettingsConfirmation = false) }
    }

    // Dialog / UI helpers
    fun openAddExpenseDialog() {
        _uiState.update { it.copy(showAddExpenseDialog = true, errorMessage = null) }
    }

    fun closeAddExpenseDialog() {
        _uiState.update { it.copy(showAddExpenseDialog = false, errorMessage = null) }
    }

    fun openEditExpenseDialog(expense: ExpenseItem) {
        _uiState.update { it.copy(editingExpense = expense, errorMessage = null) }
    }

    fun closeEditExpenseDialog() {
        _uiState.update { it.copy(editingExpense = null, errorMessage = null) }
    }

    fun openCustomCurrencyDialog() {
        _uiState.update { it.copy(showCustomCurrencyDialog = true, errorMessage = null) }
    }

    fun closeCustomCurrencyDialog() {
        _uiState.update { it.copy(showCustomCurrencyDialog = false, errorMessage = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun prepareCopyText(): String {
        val state = _uiState.value
        val currentResult = state.result
            ?: ExpenseSumCalculator.calculate(state.expenses, state.currency)
        return ExpenseSumFormatter.formatCopyText(currentResult)
    }

    fun prepareShareText(): String {
        val state = _uiState.value
        val currentResult = state.result
            ?: ExpenseSumCalculator.calculate(state.expenses, state.currency)
        return ExpenseSumFormatter.formatShareText(currentResult)
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val historyRepository: ExpenseSumHistoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ExpenseSumViewModel::class.java)) {
                return ExpenseSumViewModel(settingsRepository, historyRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

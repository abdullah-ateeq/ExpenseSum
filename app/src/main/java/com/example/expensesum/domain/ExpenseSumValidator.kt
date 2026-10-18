package com.example.expensesum.domain

import com.example.expensesum.model.CurrencyOption
import com.example.expensesum.model.ExpenseItem

sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val message: String) : ValidationResult
}

object ExpenseSumValidator {

    const val MAX_EXPENSE_COUNT: Int = 500 // Easily supports >= 100 expenses
    const val MAX_LABEL_LENGTH: Int = 40
    const val MAX_TITLE_LENGTH: Int = 50
    const val MAX_AMOUNT_MINOR_UNITS: Long = MoneyParser.MAX_MINOR_UNITS
    const val MAX_TOTAL_MINOR_UNITS: Long = 100_000_000_000_000L

    fun sanitizeLabel(label: String): String {
        return label.trim().take(MAX_LABEL_LENGTH)
    }

    fun sanitizeTitle(title: String): String {
        return title.trim().take(MAX_TITLE_LENGTH)
    }

    fun resolveDisplayTitle(title: String): String {
        val sanitized = sanitizeTitle(title)
        return sanitized.ifEmpty { "Untitled Expense List" }
    }

    fun resolveDisplayLabel(label: String, indexZeroBased: Int): String {
        val sanitized = sanitizeLabel(label)
        return sanitized.ifEmpty { "Expense ${indexZeroBased + 1}" }
    }

    fun validateLabel(label: String): ValidationResult {
        val trimmed = label.trim()
        if (trimmed.length > MAX_LABEL_LENGTH) {
            return ValidationResult.Invalid("Label cannot exceed $MAX_LABEL_LENGTH characters.")
        }
        return ValidationResult.Valid
    }

    fun validateTitle(title: String): ValidationResult {
        val trimmed = title.trim()
        if (trimmed.length > MAX_TITLE_LENGTH) {
            return ValidationResult.Invalid("Title cannot exceed $MAX_TITLE_LENGTH characters.")
        }
        return ValidationResult.Valid
    }

    fun validateCurrency(currency: CurrencyOption): ValidationResult {
        if (currency.code.isBlank()) {
            return ValidationResult.Invalid("Currency code cannot be empty.")
        }
        if (currency.symbol.isBlank()) {
            return ValidationResult.Invalid("Currency symbol cannot be empty.")
        }
        if (currency.decimalPlaces !in 0..4) {
            return ValidationResult.Invalid("Currency decimal places must be between 0 and 4.")
        }
        return ValidationResult.Valid
    }

    fun validateAmountMinorUnits(amountMinorUnits: Long): ValidationResult {
        if (amountMinorUnits < 0L) {
            return ValidationResult.Invalid("Expense amount cannot be negative.")
        }
        if (amountMinorUnits > MAX_AMOUNT_MINOR_UNITS) {
            return ValidationResult.Invalid("Amount is too large.")
        }
        return ValidationResult.Valid
    }

    fun validateCanAddExpense(currentCount: Int): ValidationResult {
        if (currentCount >= MAX_EXPENSE_COUNT) {
            return ValidationResult.Invalid("Maximum of $MAX_EXPENSE_COUNT expenses reached.")
        }
        return ValidationResult.Valid
    }

    fun validateExpenseList(expenses: List<ExpenseItem>): ValidationResult {
        if (expenses.size > MAX_EXPENSE_COUNT) {
            return ValidationResult.Invalid("Maximum of $MAX_EXPENSE_COUNT expenses reached.")
        }
        var runningTotal = 0L
        for (item in expenses) {
            val amountCheck = validateAmountMinorUnits(item.amountMinorUnits)
            if (amountCheck is ValidationResult.Invalid) {
                return amountCheck
            }
            try {
                runningTotal = Math.addExact(runningTotal, item.amountMinorUnits)
                if (runningTotal > MAX_TOTAL_MINOR_UNITS) {
                    return ValidationResult.Invalid("Total is too large.")
                }
            } catch (e: ArithmeticException) {
                return ValidationResult.Invalid("Total is too large.")
            }
        }
        return ValidationResult.Valid
    }
}

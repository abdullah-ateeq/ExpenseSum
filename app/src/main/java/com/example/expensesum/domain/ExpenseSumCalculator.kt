package com.example.expensesum.domain

import com.example.expensesum.model.CurrencyOption
import com.example.expensesum.model.ExpenseItem
import com.example.expensesum.model.ExpenseSumResult
import java.math.BigDecimal
import java.math.RoundingMode

class ExpenseSumCalculationException(message: String) : IllegalArgumentException(message)

object ExpenseSumCalculator {

    fun calculate(
        expenses: List<ExpenseItem>,
        currency: CurrencyOption
    ): ExpenseSumResult {
        val currencyValidation = ExpenseSumValidator.validateCurrency(currency)
        if (currencyValidation is ValidationResult.Invalid) {
            throw ExpenseSumCalculationException(currencyValidation.message)
        }

        if (expenses.size > ExpenseSumValidator.MAX_EXPENSE_COUNT) {
            throw ExpenseSumCalculationException(
                "Maximum of ${ExpenseSumValidator.MAX_EXPENSE_COUNT} expenses reached."
            )
        }

        if (expenses.isEmpty()) {
            return ExpenseSumResult(
                expenses = emptyList(),
                totalMinorUnits = 0L,
                count = 0,
                averageMinorUnits = 0L,
                largestMinorUnits = null,
                smallestMinorUnits = null,
                largestExpenseId = null,
                smallestExpenseId = null,
                currency = currency
            )
        }

        var totalMinorUnits = 0L
        var largestMinorUnits: Long? = null
        var smallestMinorUnits: Long? = null
        var largestExpenseId: String? = null
        var smallestExpenseId: String? = null

        val sanitizedExpenses = ArrayList<ExpenseItem>(expenses.size)

        for (item in expenses) {
            if (item.amountMinorUnits < 0L) {
                throw ExpenseSumCalculationException("Expense amount cannot be negative.")
            }
            if (item.amountMinorUnits > ExpenseSumValidator.MAX_AMOUNT_MINOR_UNITS) {
                throw ExpenseSumCalculationException("Amount is too large.")
            }

            try {
                totalMinorUnits = Math.addExact(totalMinorUnits, item.amountMinorUnits)
            } catch (e: ArithmeticException) {
                throw ExpenseSumCalculationException("Total is too large.")
            }

            if (totalMinorUnits < 0L || totalMinorUnits > ExpenseSumValidator.MAX_TOTAL_MINOR_UNITS) {
                throw ExpenseSumCalculationException("Total is too large.")
            }

            // Deterministic tie handling: strictly greater-than / less-than keeps the first occurrence
            if (largestMinorUnits == null || item.amountMinorUnits > largestMinorUnits) {
                largestMinorUnits = item.amountMinorUnits
                largestExpenseId = item.id
            }

            if (smallestMinorUnits == null || item.amountMinorUnits < smallestMinorUnits) {
                smallestMinorUnits = item.amountMinorUnits
                smallestExpenseId = item.id
            }

            sanitizedExpenses.add(
                item.copy(label = ExpenseSumValidator.sanitizeLabel(item.label))
            )
        }

        val count = sanitizedExpenses.size
        val averageMinorUnits = BigDecimal.valueOf(totalMinorUnits)
            .divide(BigDecimal.valueOf(count.toLong()), 0, RoundingMode.HALF_UP)
            .toLong()

        // Enforce core invariant: totalMinorUnits == expenses.sumOf { it.amountMinorUnits }
        check(totalMinorUnits == sanitizedExpenses.sumOf { it.amountMinorUnits }) {
            "Core total invariant violated."
        }

        return ExpenseSumResult(
            expenses = sanitizedExpenses.toList(),
            totalMinorUnits = totalMinorUnits,
            count = count,
            averageMinorUnits = averageMinorUnits,
            largestMinorUnits = largestMinorUnits,
            smallestMinorUnits = smallestMinorUnits,
            largestExpenseId = largestExpenseId,
            smallestExpenseId = smallestExpenseId,
            currency = currency
        )
    }
}

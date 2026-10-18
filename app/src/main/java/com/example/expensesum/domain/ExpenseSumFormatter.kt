package com.example.expensesum.domain

import com.example.expensesum.model.CurrencyOption
import com.example.expensesum.model.ExpenseSumResult

object ExpenseSumFormatter {

    fun formatMoney(
        minorUnits: Long,
        currency: CurrencyOption
    ): String {
        return MoneyFormatter.format(minorUnits, currency)
    }

    fun formatOptionalMoney(
        minorUnits: Long?,
        currency: CurrencyOption
    ): String {
        if (minorUnits == null) return "—"
        return MoneyFormatter.format(minorUnits, currency)
    }

    fun formatAverage(
        result: ExpenseSumResult
    ): String {
        if (result.count == 0) return "—"
        return MoneyFormatter.format(result.averageMinorUnits, result.currency)
    }

    fun formatCount(
        count: Int
    ): String {
        return if (count == 1) "1 expense" else "$count expenses"
    }

    fun formatCountBadge(
        count: Int
    ): String {
        return if (count == 1) "1 EXPENSE" else "$count EXPENSES"
    }

    fun formatCopyText(
        result: ExpenseSumResult
    ): String {
        val sb = StringBuilder()
        sb.appendLine("ExpenseSum Result")
        sb.appendLine()

        if (result.expenses.isEmpty()) {
            sb.appendLine("No expenses")
        } else {
            result.expenses.forEachIndexed { index, item ->
                val label = ExpenseSumValidator.resolveDisplayLabel(item.label, index)
                val formattedAmount = formatMoney(item.amountMinorUnits, result.currency)
                sb.appendLine("$label: $formattedAmount")
            }
        }

        sb.appendLine()
        sb.appendLine("Total: ${formatMoney(result.totalMinorUnits, result.currency)}")
        sb.appendLine("Expenses: ${result.count}")
        sb.appendLine("Average: ${formatAverage(result)}")
        sb.appendLine("Largest: ${formatOptionalMoney(result.largestMinorUnits, result.currency)}")
        sb.append("Smallest: ${formatOptionalMoney(result.smallestMinorUnits, result.currency)}")
        return sb.toString()
    }

    fun formatShareText(
        result: ExpenseSumResult
    ): String {
        val sb = StringBuilder()
        sb.appendLine("ExpenseSum")
        sb.appendLine()

        if (result.expenses.isEmpty()) {
            sb.appendLine("No expenses")
        } else {
            result.expenses.forEachIndexed { index, item ->
                val label = ExpenseSumValidator.resolveDisplayLabel(item.label, index)
                val formattedAmount = formatMoney(item.amountMinorUnits, result.currency)
                sb.appendLine("$label: $formattedAmount")
            }
        }

        sb.appendLine()
        sb.appendLine("Total: ${formatMoney(result.totalMinorUnits, result.currency)}")
        sb.appendLine(formatCount(result.count))
        sb.append("Average: ${formatAverage(result)}")
        return sb.toString()
    }
}

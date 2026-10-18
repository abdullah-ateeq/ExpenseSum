package com.example.expensesum.repository

import com.example.expensesum.data.ExpenseSerialization
import com.example.expensesum.data.KeyValueStore
import com.example.expensesum.domain.ExpenseSumValidator
import com.example.expensesum.model.CurrencyOption
import com.example.expensesum.model.ExpenseItem
import com.example.expensesum.model.ExpenseSumHistory
import java.util.UUID

class ExpenseSumHistoryRepository(
    private val store: KeyValueStore
) {
    companion object {
        private const val KEY_HISTORY_LIST = "expense_sum_history_v1"
        const val MAX_HISTORY_ENTRIES = 10
    }

    fun loadHistory(): List<ExpenseSumHistory> {
        val raw = store.getString(KEY_HISTORY_LIST, "")
        return ExpenseSerialization.deserializeHistoryList(raw)
            .sortedByDescending { it.calculatedAt }
            .take(MAX_HISTORY_ENTRIES)
    }

    /**
     * Saves a completed expense list to history.
     * Prevents accidental duplicate entries if the exact same title, currency, and expense list
     * already matches the most recent history entry.
     * Returns true if a new entry was added (or updated), false if skipped as duplicate.
     */
    fun saveToHistory(
        title: String,
        expenses: List<ExpenseItem>,
        currency: CurrencyOption,
        timestampMillis: Long = System.currentTimeMillis()
    ): Pair<List<ExpenseSumHistory>, Boolean> {
        val current = loadHistory().toMutableList()
        val resolvedTitle = ExpenseSumValidator.resolveDisplayTitle(title)
        val sanitizedExpenses = expenses.map {
            it.copy(label = ExpenseSumValidator.sanitizeLabel(it.label))
        }

        val mostRecent = current.firstOrNull()
        if (mostRecent != null && isSameSnapshot(mostRecent, resolvedTitle, currency, sanitizedExpenses)) {
            return current to false
        }

        val newEntry = ExpenseSumHistory(
            id = UUID.randomUUID().toString(),
            title = resolvedTitle,
            expenses = sanitizedExpenses,
            currencyCode = currency.code,
            currencySymbol = currency.symbol,
            calculatedAt = timestampMillis,
            currencyName = currency.name,
            currencyDecimalPlaces = currency.decimalPlaces
        )

        current.add(0, newEntry)
        val trimmed = current.take(MAX_HISTORY_ENTRIES)
        persist(trimmed)
        return trimmed to true
    }

    fun deleteHistoryItem(id: String): List<ExpenseSumHistory> {
        val updated = loadHistory().filterNot { it.id == id }
        persist(updated)
        return updated
    }

    fun clearHistory(): List<ExpenseSumHistory> {
        store.remove(KEY_HISTORY_LIST)
        return emptyList()
    }

    private fun persist(history: List<ExpenseSumHistory>) {
        store.putString(KEY_HISTORY_LIST, ExpenseSerialization.serializeHistoryList(history))
    }

    private fun isSameSnapshot(
        existing: ExpenseSumHistory,
        title: String,
        currency: CurrencyOption,
        expenses: List<ExpenseItem>
    ): Boolean {
        if (existing.title != title) return false
        if (existing.currencyCode != currency.code) return false
        if (existing.currencySymbol != currency.symbol) return false
        if (existing.currencyDecimalPlaces != currency.decimalPlaces) return false
        if (existing.expenses.size != expenses.size) return false

        return existing.expenses.zip(expenses).all { (a, b) ->
            a.label == b.label && a.amountMinorUnits == b.amountMinorUnits
        }
    }
}

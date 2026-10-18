package com.example.expensesum.data

import com.example.expensesum.domain.CurrencyRepository
import com.example.expensesum.domain.ExpenseSumValidator
import com.example.expensesum.model.CurrencyOption
import com.example.expensesum.model.ExpenseItem
import com.example.expensesum.model.ExpenseSumHistory
import java.util.Base64

data class PersistedDraftList(
    val title: String,
    val currency: CurrencyOption,
    val expenses: List<ExpenseItem>
)

object ExpenseSerialization {

    private fun encodeField(value: String): String {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))
    }

    private fun decodeField(encoded: String): String {
        return try {
            String(Base64.getUrlDecoder().decode(encoded), Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    fun serializeExpenseList(expenses: List<ExpenseItem>): String {
        if (expenses.isEmpty()) return ""
        return expenses.joinToString(separator = ";") { item ->
            "${encodeField(item.id)}:${encodeField(item.label)}:${item.amountMinorUnits}"
        }
    }

    fun deserializeExpenseList(raw: String?): List<ExpenseItem> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(";").mapNotNull { token ->
            val parts = token.split(":")
            if (parts.size != 3) return@mapNotNull null
            val id = decodeField(parts[0]).ifEmpty { return@mapNotNull null }
            val label = ExpenseSumValidator.sanitizeLabel(decodeField(parts[1]))
            val amount = parts[2].toLongOrNull() ?: return@mapNotNull null
            if (amount < 0L || amount > ExpenseSumValidator.MAX_AMOUNT_MINOR_UNITS) {
                return@mapNotNull null
            }
            ExpenseItem(id = id, label = label, amountMinorUnits = amount)
        }
    }

    fun serializeCurrency(currency: CurrencyOption): String {
        return listOf(
            encodeField(currency.code),
            encodeField(currency.symbol),
            encodeField(currency.name),
            currency.decimalPlaces.toString()
        ).joinToString(":")
    }

    fun deserializeCurrency(raw: String?): CurrencyOption {
        if (raw.isNullOrBlank()) return CurrencyRepository.defaultCurrency
        val parts = raw.split(":")
        if (parts.size != 4) return CurrencyRepository.getByCode(raw)
        val code = decodeField(parts[0]).ifEmpty { "USD" }
        val symbol = decodeField(parts[1]).ifEmpty { "$" }
        val name = decodeField(parts[2]).ifEmpty { code }
        val decimals = parts[3].toIntOrNull()?.coerceIn(0, 4) ?: 2
        return CurrencyOption(
            code = code,
            symbol = symbol,
            name = name,
            decimalPlaces = decimals
        )
    }

    fun serializeHistoryList(history: List<ExpenseSumHistory>): String {
        if (history.isEmpty()) return ""
        return history.joinToString(separator = "\n") { entry ->
            listOf(
                encodeField(entry.id),
                encodeField(entry.title),
                encodeField(entry.currencyCode),
                encodeField(entry.currencySymbol),
                entry.calculatedAt.toString(),
                encodeField(entry.currencyName),
                entry.currencyDecimalPlaces.toString(),
                encodeField(serializeExpenseList(entry.expenses))
            ).joinToString("|")
        }
    }

    fun deserializeHistoryList(raw: String?): List<ExpenseSumHistory> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.lines().mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null
            val parts = line.split("|")
            if (parts.size < 8) return@mapNotNull null
            val id = decodeField(parts[0]).ifEmpty { return@mapNotNull null }
            val title = ExpenseSumValidator.resolveDisplayTitle(decodeField(parts[1]))
            val currencyCode = decodeField(parts[2]).ifEmpty { "USD" }
            val currencySymbol = decodeField(parts[3]).ifEmpty { "$" }
            val calculatedAt = parts[4].toLongOrNull() ?: 0L
            val currencyName = decodeField(parts[5]).ifEmpty { currencyCode }
            val decimals = parts[6].toIntOrNull()?.coerceIn(0, 4) ?: 2
            val expenses = deserializeExpenseList(decodeField(parts[7]))
            ExpenseSumHistory(
                id = id,
                title = title,
                expenses = expenses,
                currencyCode = currencyCode,
                currencySymbol = currencySymbol,
                calculatedAt = calculatedAt,
                currencyName = currencyName,
                currencyDecimalPlaces = decimals
            )
        }
    }
}

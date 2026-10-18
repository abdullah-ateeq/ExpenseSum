package com.example.expensesum.domain

import com.example.expensesum.model.CurrencyOption
import java.math.BigDecimal

sealed interface ParseResult {
    data class Success(val minorUnits: Long) : ParseResult
    data class Error(val message: String) : ParseResult
}

object MoneyParser {

    const val MAX_MINOR_UNITS: Long = 1_000_000_000_000L

    fun parse(input: String, currency: CurrencyOption): ParseResult {
        val cleaned = input.trim().replace(",", "").replace(" ", "")
        if (cleaned.isEmpty()) {
            return ParseResult.Error("Enter an expense amount.")
        }

        if (cleaned.startsWith("-")) {
            return ParseResult.Error("Expense amount cannot be negative.")
        }

        val normalized = if (cleaned.startsWith("+")) cleaned.substring(1) else cleaned
        if (normalized.isEmpty() || normalized == ".") {
            return ParseResult.Error("Enter a valid expense amount.")
        }

        val parts = normalized.split(".")
        if (parts.size > 2) {
            return ParseResult.Error("Enter a valid numeric amount.")
        }

        val wholePart = parts[0].ifEmpty { "0" }
        val fractionalPart = if (parts.size == 2) parts[1] else ""

        if (!wholePart.all { it.isDigit() } || !fractionalPart.all { it.isDigit() }) {
            return ParseResult.Error("Enter a valid numeric amount.")
        }

        val allowedDecimals = currency.decimalPlaces.coerceIn(0, 4)
        if (allowedDecimals == 0 && fractionalPart.isNotEmpty() && fractionalPart.any { it != '0' }) {
            return ParseResult.Error("${currency.code} does not use decimal places.")
        }

        if (fractionalPart.length > allowedDecimals) {
            if (allowedDecimals == 0) {
                return ParseResult.Error("${currency.code} does not use decimal places.")
            }
            return ParseResult.Error("Maximum $allowedDecimals decimal places allowed for ${currency.code}.")
        }

        return try {
            val bd = BigDecimal(normalized)
            if (bd < BigDecimal.ZERO) {
                return ParseResult.Error("Expense amount cannot be negative.")
            }
            val scaled = bd.movePointRight(allowedDecimals)
            val minorUnits = scaled.longValueExact()
            if (minorUnits < 0L) {
                return ParseResult.Error("Expense amount cannot be negative.")
            }
            if (minorUnits > MAX_MINOR_UNITS) {
                return ParseResult.Error("Amount is too large.")
            }
            ParseResult.Success(minorUnits)
        } catch (e: ArithmeticException) {
            ParseResult.Error("Amount is too large.")
        } catch (e: NumberFormatException) {
            ParseResult.Error("Enter a valid expense amount.")
        }
    }

    fun isTemporarilyIncomplete(input: String): Boolean {
        val trimmed = input.trim()
        return trimmed.isEmpty() || trimmed == "." || trimmed.endsWith(".")
    }
}

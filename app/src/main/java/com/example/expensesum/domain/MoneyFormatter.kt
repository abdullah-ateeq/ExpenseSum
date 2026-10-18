package com.example.expensesum.domain

import com.example.expensesum.model.CurrencyOption
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object MoneyFormatter {

    private val symbols = DecimalFormatSymbols(Locale.US)

    private fun needsSpaceAfterSymbol(symbol: String): Boolean {
        if (symbol.isEmpty()) return false
        if (symbol.endsWith(".")) return true
        return symbol.length > 1 && symbol.last().isLetter()
    }

    fun format(minorUnits: Long, currency: CurrencyOption): String {
        val isNegative = minorUnits < 0
        val absMinor = if (minorUnits == Long.MIN_VALUE) Long.MAX_VALUE else kotlin.math.abs(minorUnits)
        val decimals = currency.decimalPlaces.coerceIn(0, 4)

        val bd = BigDecimal.valueOf(absMinor).movePointLeft(decimals)
        val pattern = if (decimals > 0) {
            "#,##0." + "0".repeat(decimals)
        } else {
            "#,##0"
        }
        val decimalFormat = DecimalFormat(pattern, symbols)
        val formattedNumber = decimalFormat.format(bd)

        val prefix = if (needsSpaceAfterSymbol(currency.symbol)) {
            "${currency.symbol} "
        } else {
            currency.symbol
        }
        return if (isNegative) "-$prefix$formattedNumber" else "$prefix$formattedNumber"
    }

    fun formatPlainInput(minorUnits: Long, currency: CurrencyOption): String {
        val decimals = currency.decimalPlaces.coerceIn(0, 4)
        val bd = BigDecimal.valueOf(minorUnits).movePointLeft(decimals)
        return if (decimals == 0) {
            bd.toPlainString()
        } else {
            val stripped = bd.stripTrailingZeros()
            if (stripped.scale() <= 0) {
                stripped.setScale(0).toPlainString()
            } else {
                bd.setScale(decimals).toPlainString()
            }
        }
    }

    fun formatPresetButton(majorAmount: Long, currency: CurrencyOption): String {
        val decimalFormat = DecimalFormat("#,##0", symbols)
        val formatted = decimalFormat.format(majorAmount)
        val prefix = if (needsSpaceAfterSymbol(currency.symbol)) {
            "${currency.symbol} "
        } else {
            currency.symbol
        }
        return "$prefix$formatted"
    }

    fun formatAccessibleAmount(minorUnits: Long, currency: CurrencyOption): String {
        val decimals = currency.decimalPlaces.coerceIn(0, 4)
        if (decimals == 0) {
            return "$minorUnits ${currency.name}"
        }
        var divisor = 1L
        repeat(decimals) { divisor *= 10L }
        val major = minorUnits / divisor
        val minor = kotlin.math.abs(minorUnits % divisor)
        return when (currency.code) {
            "USD", "CAD", "AUD" -> {
                val dollarWord = if (major == 1L) "dollar" else "dollars"
                if (minor == 0L) {
                    "$major $dollarWord"
                } else {
                    val centWord = if (minor == 1L) "cent" else "cents"
                    "$major $dollarWord and $minor $centWord"
                }
            }
            else -> format(minorUnits, currency)
        }
    }
}

package com.example.expensesum.domain

import com.example.expensesum.model.CurrencyOption

object CurrencyRepository {

    val USD = CurrencyOption(code = "USD", symbol = "$", name = "US Dollar", decimalPlaces = 2)
    val EUR = CurrencyOption(code = "EUR", symbol = "€", name = "Euro", decimalPlaces = 2)
    val GBP = CurrencyOption(code = "GBP", symbol = "£", name = "British Pound", decimalPlaces = 2)
    val PKR = CurrencyOption(code = "PKR", symbol = "Rs.", name = "Pakistani Rupee", decimalPlaces = 2)
    val AED = CurrencyOption(code = "AED", symbol = "AED", name = "UAE Dirham", decimalPlaces = 2)
    val SAR = CurrencyOption(code = "SAR", symbol = "SAR", name = "Saudi Riyal", decimalPlaces = 2)
    val CAD = CurrencyOption(code = "CAD", symbol = "CA$", name = "Canadian Dollar", decimalPlaces = 2)
    val AUD = CurrencyOption(code = "AUD", symbol = "A$", name = "Australian Dollar", decimalPlaces = 2)
    val INR = CurrencyOption(code = "INR", symbol = "₹", name = "Indian Rupee", decimalPlaces = 2)
    val JPY = CurrencyOption(code = "JPY", symbol = "¥", name = "Japanese Yen", decimalPlaces = 0)
    val CNY = CurrencyOption(code = "CNY", symbol = "CN¥", name = "Chinese Yuan", decimalPlaces = 2)

    val supportedCurrencies: List<CurrencyOption> = listOf(
        USD, EUR, GBP, PKR, AED, SAR, CAD, AUD, INR, JPY, CNY
    )

    val defaultCurrency: CurrencyOption = USD

    fun getByCode(code: String): CurrencyOption {
        return supportedCurrencies.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: USD
    }

    fun createCustomCurrency(
        code: String,
        symbol: String,
        name: String,
        decimalPlaces: Int
    ): CurrencyOption {
        val cleanCode = code.trim().uppercase().take(6).ifEmpty { "OTHER" }
        val cleanSymbol = symbol.trim().take(6).ifEmpty { cleanCode }
        val cleanName = name.trim().take(30).ifEmpty { "Custom ($cleanCode)" }
        val validDecimals = decimalPlaces.coerceIn(0, 4)
        return CurrencyOption(
            code = cleanCode,
            symbol = cleanSymbol,
            name = cleanName,
            decimalPlaces = validDecimals
        )
    }

    fun scaleMajorAmountToMinorUnits(majorAmount: Long, currency: CurrencyOption): Long {
        var factor = 1L
        repeat(currency.decimalPlaces.coerceIn(0, 4)) {
            factor = Math.multiplyExact(factor, 10L)
        }
        return Math.multiplyExact(majorAmount, factor)
    }

    fun getQuickPresetsMajorUnits(currency: CurrencyOption): List<Long> {
        return if (currency.code == "JPY") {
            listOf(500L, 1000L, 2000L, 5000L, 10000L)
        } else {
            listOf(5L, 10L, 20L, 50L, 100L)
        }
    }
}

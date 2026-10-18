package com.example.expensesum.model

enum class ThemeMode(val displayName: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark");

    companion object {
        fun fromName(name: String?): ThemeMode {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: SYSTEM
        }
    }
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val hapticFeedback: Boolean = true,
    val animations: Boolean = true,
    val defaultCurrency: CurrencyOption = CurrencyOption(
        code = "USD",
        symbol = "$",
        name = "US Dollar",
        decimalPlaces = 2
    ),
    val confirmClearAll: Boolean = true,
    val confirmHistoryDelete: Boolean = true,
    val preserveLastList: Boolean = true
)

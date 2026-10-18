package com.example.expensesum.repository

import com.example.expensesum.data.ExpenseSerialization
import com.example.expensesum.data.KeyValueStore
import com.example.expensesum.data.PersistedDraftList
import com.example.expensesum.domain.CurrencyRepository
import com.example.expensesum.domain.ExpenseSumValidator
import com.example.expensesum.model.AppSettings
import com.example.expensesum.model.CurrencyOption
import com.example.expensesum.model.ExpenseItem
import com.example.expensesum.model.ThemeMode

class SettingsRepository(
    private val store: KeyValueStore
) {
    companion object {
        private const val KEY_THEME_MODE = "settings_theme_mode"
        private const val KEY_DYNAMIC_COLOR = "settings_dynamic_color"
        private const val KEY_HAPTIC_FEEDBACK = "settings_haptic_feedback"
        private const val KEY_ANIMATIONS = "settings_animations"
        private const val KEY_DEFAULT_CURRENCY = "settings_default_currency"
        private const val KEY_CONFIRM_CLEAR_ALL = "settings_confirm_clear_all"
        private const val KEY_CONFIRM_HISTORY_DELETE = "settings_confirm_history_delete"
        private const val KEY_PRESERVE_LAST_LIST = "settings_preserve_last_list"

        private const val KEY_HAS_INITIALIZED_DRAFT = "draft_has_initialized"
        private const val KEY_DRAFT_TITLE = "draft_title"
        private const val KEY_DRAFT_CURRENCY = "draft_currency"
        private const val KEY_DRAFT_EXPENSES = "draft_expenses"

        fun defaultSampleExpenses(currency: CurrencyOption = CurrencyRepository.USD): List<ExpenseItem> {
            val ten = CurrencyRepository.scaleMajorAmountToMinorUnits(10L, currency)
            val twenty = CurrencyRepository.scaleMajorAmountToMinorUnits(20L, currency)
            val five = CurrencyRepository.scaleMajorAmountToMinorUnits(5L, currency)
            return listOf(
                ExpenseItem(id = "sample-1", label = "Expense 1", amountMinorUnits = ten),
                ExpenseItem(id = "sample-2", label = "Expense 2", amountMinorUnits = twenty),
                ExpenseItem(id = "sample-3", label = "Expense 3", amountMinorUnits = five)
            )
        }
    }

    fun loadSettings(): AppSettings {
        val themeName = store.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        val dynamicColor = store.getBoolean(KEY_DYNAMIC_COLOR, true)
        val hapticFeedback = store.getBoolean(KEY_HAPTIC_FEEDBACK, true)
        val animations = store.getBoolean(KEY_ANIMATIONS, true)
        val currencyRaw = store.getString(KEY_DEFAULT_CURRENCY, null)
        val defaultCurrency = ExpenseSerialization.deserializeCurrency(currencyRaw)
        val confirmClearAll = store.getBoolean(KEY_CONFIRM_CLEAR_ALL, true)
        val confirmHistoryDelete = store.getBoolean(KEY_CONFIRM_HISTORY_DELETE, true)
        val preserveLastList = store.getBoolean(KEY_PRESERVE_LAST_LIST, true)

        return AppSettings(
            themeMode = ThemeMode.fromName(themeName),
            dynamicColor = dynamicColor,
            hapticFeedback = hapticFeedback,
            animations = animations,
            defaultCurrency = defaultCurrency,
            confirmClearAll = confirmClearAll,
            confirmHistoryDelete = confirmHistoryDelete,
            preserveLastList = preserveLastList
        )
    }

    fun saveSettings(settings: AppSettings) {
        store.putString(KEY_THEME_MODE, settings.themeMode.name)
        store.putBoolean(KEY_DYNAMIC_COLOR, settings.dynamicColor)
        store.putBoolean(KEY_HAPTIC_FEEDBACK, settings.hapticFeedback)
        store.putBoolean(KEY_ANIMATIONS, settings.animations)
        store.putString(KEY_DEFAULT_CURRENCY, ExpenseSerialization.serializeCurrency(settings.defaultCurrency))
        store.putBoolean(KEY_CONFIRM_CLEAR_ALL, settings.confirmClearAll)
        store.putBoolean(KEY_CONFIRM_HISTORY_DELETE, settings.confirmHistoryDelete)
        store.putBoolean(KEY_PRESERVE_LAST_LIST, settings.preserveLastList)
    }

    fun resetSettings(): AppSettings {
        val defaults = AppSettings()
        saveSettings(defaults)
        return defaults
    }

    fun saveLastList(
        title: String,
        currency: CurrencyOption,
        expenses: List<ExpenseItem>
    ) {
        store.putBoolean(KEY_HAS_INITIALIZED_DRAFT, true)
        store.putString(KEY_DRAFT_TITLE, ExpenseSumValidator.sanitizeTitle(title))
        store.putString(KEY_DRAFT_CURRENCY, ExpenseSerialization.serializeCurrency(currency))
        store.putString(KEY_DRAFT_EXPENSES, ExpenseSerialization.serializeExpenseList(expenses))
    }

    fun loadInitialDraft(settings: AppSettings): PersistedDraftList {
        if (!settings.preserveLastList) {
            return PersistedDraftList(
                title = "",
                currency = settings.defaultCurrency,
                expenses = emptyList()
            )
        }

        val hasInitialized = store.getBoolean(KEY_HAS_INITIALIZED_DRAFT, false)
        if (!hasInitialized) {
            val sampleExpenses = defaultSampleExpenses(settings.defaultCurrency)
            saveLastList("", settings.defaultCurrency, sampleExpenses)
            return PersistedDraftList(
                title = "",
                currency = settings.defaultCurrency,
                expenses = sampleExpenses
            )
        }

        val title = ExpenseSumValidator.sanitizeTitle(store.getString(KEY_DRAFT_TITLE, "") ?: "")
        val currencyRaw = store.getString(KEY_DRAFT_CURRENCY, null)
        val currency = if (currencyRaw != null) {
            ExpenseSerialization.deserializeCurrency(currencyRaw)
        } else {
            settings.defaultCurrency
        }
        val expensesRaw = store.getString(KEY_DRAFT_EXPENSES, "")
        val expenses = ExpenseSerialization.deserializeExpenseList(expensesRaw)

        return PersistedDraftList(
            title = title,
            currency = currency,
            expenses = expenses
        )
    }

    fun clearSavedDraft(defaultCurrency: CurrencyOption) {
        saveLastList("", defaultCurrency, emptyList())
    }
}

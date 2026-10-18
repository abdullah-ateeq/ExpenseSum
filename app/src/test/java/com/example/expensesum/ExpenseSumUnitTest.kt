package com.example.expensesum

import com.example.expensesum.data.InMemoryKeyValueStore
import com.example.expensesum.domain.CurrencyRepository
import com.example.expensesum.domain.ExpenseSumCalculationException
import com.example.expensesum.domain.ExpenseSumCalculator
import com.example.expensesum.domain.ExpenseSumFormatter
import com.example.expensesum.domain.ExpenseSumValidator
import com.example.expensesum.domain.MoneyFormatter
import com.example.expensesum.domain.MoneyParser
import com.example.expensesum.domain.ParseResult
import com.example.expensesum.domain.ValidationResult
import com.example.expensesum.model.CurrencyOption
import com.example.expensesum.model.ExpenseItem
import com.example.expensesum.model.ThemeMode
import com.example.expensesum.repository.ExpenseSumHistoryRepository
import com.example.expensesum.repository.SettingsRepository
import com.example.expensesum.ui.ExpenseSumViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ExpenseSumUnitTest {

    // 1. Empty list (CRITICAL TEST — EMPTY)
    @Test
    fun test01_emptyList_returnsZeroTotalAndDashMetrics() {
        val result = ExpenseSumCalculator.calculate(emptyList(), CurrencyRepository.USD)
        assertEquals(0L, result.totalMinorUnits)
        assertEquals(0, result.count)
        assertEquals(0L, result.averageMinorUnits)
        assertNull(result.largestMinorUnits)
        assertNull(result.smallestMinorUnits)
        assertNull(result.largestExpenseId)
        assertNull(result.smallestExpenseId)
        assertEquals("$0.00", ExpenseSumFormatter.formatMoney(result.totalMinorUnits, result.currency))
        assertEquals("—", ExpenseSumFormatter.formatAverage(result))
        assertEquals("—", ExpenseSumFormatter.formatOptionalMoney(result.largestMinorUnits, result.currency))
        assertEquals("—", ExpenseSumFormatter.formatOptionalMoney(result.smallestMinorUnits, result.currency))
    }

    // 2. One expense (CRITICAL TEST — SINGLE EXPENSE)
    @Test
    fun test02_singleExpense_calculatesTotalCountAverageLargestSmallest() {
        val item = ExpenseItem(id = "e1", label = "Lunch", amountMinorUnits = 2500L)
        val result = ExpenseSumCalculator.calculate(listOf(item), CurrencyRepository.USD)
        assertEquals(2500L, result.totalMinorUnits)
        assertEquals(1, result.count)
        assertEquals(2500L, result.averageMinorUnits)
        assertEquals(2500L, result.largestMinorUnits)
        assertEquals(2500L, result.smallestMinorUnits)
        assertEquals("e1", result.largestExpenseId)
        assertEquals("e1", result.smallestExpenseId)
    }

    // 3. Two expenses
    @Test
    fun test03_twoExpenses_calculatesCorrectSumAndAverage() {
        val items = listOf(
            ExpenseItem(id = "1", label = "Coffee", amountMinorUnits = 500L),
            ExpenseItem(id = "2", label = "Lunch", amountMinorUnits = 1500L)
        )
        val result = ExpenseSumCalculator.calculate(items, CurrencyRepository.USD)
        assertEquals(2000L, result.totalMinorUnits)
        assertEquals(2, result.count)
        assertEquals(1000L, result.averageMinorUnits)
        assertEquals(1500L, result.largestMinorUnits)
        assertEquals(500L, result.smallestMinorUnits)
    }

    // 4. Multiple expenses (CRITICAL TEST — BASIC TOTAL: $5, $10, $20)
    @Test
    fun test04_multipleExpenses_criticalBasicTotal() {
        val items = listOf(
            ExpenseItem(id = "1", label = "A", amountMinorUnits = 500L),
            ExpenseItem(id = "2", label = "B", amountMinorUnits = 1000L),
            ExpenseItem(id = "3", label = "C", amountMinorUnits = 2000L)
        )
        val result = ExpenseSumCalculator.calculate(items, CurrencyRepository.USD)
        assertEquals(3500L, result.totalMinorUnits)
        assertEquals(3, result.count)
        assertEquals(1167L, result.averageMinorUnits)
        assertEquals("$11.67", ExpenseSumFormatter.formatAverage(result))
        assertEquals(2000L, result.largestMinorUnits)
        assertEquals(500L, result.smallestMinorUnits)
    }

    // 5. Equal expenses
    @Test
    fun test05_equalExpenses_sameTotalAverageLargestSmallest() {
        val items = listOf(
            ExpenseItem(id = "1", label = "Item 1", amountMinorUnits = 1200L),
            ExpenseItem(id = "2", label = "Item 2", amountMinorUnits = 1200L),
            ExpenseItem(id = "3", label = "Item 3", amountMinorUnits = 1200L)
        )
        val result = ExpenseSumCalculator.calculate(items, CurrencyRepository.USD)
        assertEquals(3600L, result.totalMinorUnits)
        assertEquals(1200L, result.averageMinorUnits)
        assertEquals(1200L, result.largestMinorUnits)
        assertEquals(1200L, result.smallestMinorUnits)
        assertEquals("1", result.largestExpenseId)
        assertEquals("1", result.smallestExpenseId)
    }

    // 6. Unequal expenses (Core example: Coffee $5, Fuel $40, Lunch $12.50, Parking $5)
    @Test
    fun test06_unequalExpenses_coreExampleTotal6250() {
        val items = listOf(
            ExpenseItem(id = "1", label = "Coffee", amountMinorUnits = 500L),
            ExpenseItem(id = "2", label = "Fuel", amountMinorUnits = 4000L),
            ExpenseItem(id = "3", label = "Lunch", amountMinorUnits = 1250L),
            ExpenseItem(id = "4", label = "Parking", amountMinorUnits = 500L)
        )
        val result = ExpenseSumCalculator.calculate(items, CurrencyRepository.USD)
        assertEquals(6250L, result.totalMinorUnits)
        assertEquals("$62.50", ExpenseSumFormatter.formatMoney(result.totalMinorUnits, result.currency))
        assertEquals(4, result.count)
        assertEquals(1563L, result.averageMinorUnits)
        assertEquals("$15.63", ExpenseSumFormatter.formatAverage(result))
        assertEquals(4000L, result.largestMinorUnits)
        assertEquals(500L, result.smallestMinorUnits)
    }

    // 7. Zero-value expense (CRITICAL TEST — ZERO EXPENSE)
    @Test
    fun test07_zeroValueExpense_countsAsExpense() {
        val items = listOf(
            ExpenseItem(id = "1", label = "Free Coffee", amountMinorUnits = 0L),
            ExpenseItem(id = "2", label = "Lunch", amountMinorUnits = 1000L)
        )
        val result = ExpenseSumCalculator.calculate(items, CurrencyRepository.USD)
        assertEquals(1000L, result.totalMinorUnits)
        assertEquals(2, result.count)
        assertEquals(500L, result.averageMinorUnits)
        assertEquals(1000L, result.largestMinorUnits)
        assertEquals(0L, result.smallestMinorUnits)
    }

    // 8. Negative expense rejected
    @Test
    fun test08_negativeExpense_rejectedByParserAndCalculator() {
        val parseResult = MoneyParser.parse("-5.00", CurrencyRepository.USD)
        assertTrue(parseResult is ParseResult.Error)
        assertEquals("Expense amount cannot be negative.", (parseResult as ParseResult.Error).message)

        try {
            ExpenseSumCalculator.calculate(
                listOf(ExpenseItem(id = "1", label = "Bad", amountMinorUnits = -100L)),
                CurrencyRepository.USD
            )
            fail("Expected ExpenseSumCalculationException for negative expense")
        } catch (e: ExpenseSumCalculationException) {
            assertEquals("Expense amount cannot be negative.", e.message)
        }
    }

    // 9. Total calculation accuracy
    @Test
    fun test09_totalCalculation_exactMinorUnits() {
        val items = listOf(
            ExpenseItem("1", "A", 101L),
            ExpenseItem("2", "B", 202L),
            ExpenseItem("3", "C", 303L),
            ExpenseItem("4", "D", 404L)
        )
        val result = ExpenseSumCalculator.calculate(items, CurrencyRepository.USD)
        assertEquals(1010L, result.totalMinorUnits)
    }

    // 10. Average calculation ($100 / 3 = $33.33)
    @Test
    fun test10_averageCalculation_100DividedBy3() {
        val items = listOf(
            ExpenseItem("1", "A", 3333L),
            ExpenseItem("2", "B", 3333L),
            ExpenseItem("3", "C", 3334L)
        )
        val result = ExpenseSumCalculator.calculate(items, CurrencyRepository.USD)
        assertEquals(10000L, result.totalMinorUnits)
        assertEquals(3333L, result.averageMinorUnits)
        assertEquals("$33.33", ExpenseSumFormatter.formatAverage(result))
    }

    // 11. Largest expense identification
    @Test
    fun test11_largestExpense_correctlyIdentified() {
        val items = listOf(
            ExpenseItem("1", "Small", 500L),
            ExpenseItem("2", "Big", 4000L),
            ExpenseItem("3", "Medium", 1200L)
        )
        val result = ExpenseSumCalculator.calculate(items, CurrencyRepository.USD)
        assertEquals(4000L, result.largestMinorUnits)
        assertEquals("2", result.largestExpenseId)
    }

    // 12. Smallest expense identification
    @Test
    fun test12_smallestExpense_correctlyIdentified() {
        val items = listOf(
            ExpenseItem("1", "Medium", 1200L),
            ExpenseItem("2", "Big", 4000L),
            ExpenseItem("3", "Small", 500L)
        )
        val result = ExpenseSumCalculator.calculate(items, CurrencyRepository.USD)
        assertEquals(500L, result.smallestMinorUnits)
        assertEquals("3", result.smallestExpenseId)
    }

    // 13. Duplicate amounts
    @Test
    fun test13_duplicateAmounts_summedAccurately() {
        val items = listOf(
            ExpenseItem("1", "A", 750L),
            ExpenseItem("2", "B", 750L),
            ExpenseItem("3", "C", 1500L)
        )
        val result = ExpenseSumCalculator.calculate(items, CurrencyRepository.USD)
        assertEquals(3000L, result.totalMinorUnits)
        assertEquals(3, result.count)
    }

    // 14. Duplicate labels
    @Test
    fun test14_duplicateLabels_allowedAndPreserved() {
        val items = listOf(
            ExpenseItem("1", "Coffee", 450L),
            ExpenseItem("2", "Coffee", 550L)
        )
        val result = ExpenseSumCalculator.calculate(items, CurrencyRepository.USD)
        assertEquals(1000L, result.totalMinorUnits)
        assertEquals("Coffee", result.expenses[0].label)
        assertEquals("Coffee", result.expenses[1].label)
    }

    // 15. Empty labels fallback to Expense N
    @Test
    fun test15_emptyLabels_resolveToDefaultExpenseIndex() {
        assertEquals("Expense 1", ExpenseSumValidator.resolveDisplayLabel("", 0))
        assertEquals("Expense 2", ExpenseSumValidator.resolveDisplayLabel("   ", 1))
        assertEquals("Coffee", ExpenseSumValidator.resolveDisplayLabel("Coffee", 0))
    }

    // 16. Label trimming
    @Test
    fun test16_labelTrimming_trimsLeadingAndTrailingWhitespace() {
        val sanitized = ExpenseSumValidator.sanitizeLabel("   Groceries   ")
        assertEquals("Groceries", sanitized)
    }

    // 17. Label length (maximum 40 characters)
    @Test
    fun test17_labelLength_enforcesMax40Characters() {
        val longLabel = "A".repeat(45)
        assertTrue(ExpenseSumValidator.validateLabel(longLabel) is ValidationResult.Invalid)
        assertEquals(40, ExpenseSumValidator.sanitizeLabel(longLabel).length)
    }

    // 18. USD currency formatting
    @Test
    fun test18_usdCurrency_formatsCorrectly() {
        assertEquals("$1,250.00", MoneyFormatter.format(125000L, CurrencyRepository.USD))
    }

    // 19. EUR currency formatting
    @Test
    fun test19_eurCurrency_formatsCorrectly() {
        assertEquals("€1,250.50", MoneyFormatter.format(125050L, CurrencyRepository.EUR))
    }

    // 20. GBP currency formatting
    @Test
    fun test20_gbpCurrency_formatsCorrectly() {
        assertEquals("£99.95", MoneyFormatter.format(9995L, CurrencyRepository.GBP))
    }

    // 21. PKR currency (CRITICAL TEST — PKR)
    @Test
    fun test21_pkrCurrency_criticalTest() {
        val items = listOf(
            ExpenseItem("1", "A", 50000L),
            ExpenseItem("2", "B", 75000L),
            ExpenseItem("3", "C", 25000L)
        )
        val result = ExpenseSumCalculator.calculate(items, CurrencyRepository.PKR)
        assertEquals(150000L, result.totalMinorUnits)
        assertEquals("Rs. 1,500.00", ExpenseSumFormatter.formatMoney(result.totalMinorUnits, result.currency))
    }

    // 22. AED currency
    @Test
    fun test22_aedCurrency_formatsWithSymbolAndSpace() {
        assertEquals("AED 250.00", MoneyFormatter.format(25000L, CurrencyRepository.AED))
    }

    // 23. SAR currency
    @Test
    fun test23_sarCurrency_formatsWithSymbolAndSpace() {
        assertEquals("SAR 1,000.00", MoneyFormatter.format(100000L, CurrencyRepository.SAR))
    }

    // 24. JPY currency (CRITICAL TEST — JPY)
    @Test
    fun test24_jpyCurrency_criticalTest() {
        val items = listOf(
            ExpenseItem("1", "A", 500L),
            ExpenseItem("2", "B", 750L),
            ExpenseItem("3", "C", 250L)
        )
        val result = ExpenseSumCalculator.calculate(items, CurrencyRepository.JPY)
        assertEquals(1500L, result.totalMinorUnits)
        assertEquals("¥1,500", ExpenseSumFormatter.formatMoney(result.totalMinorUnits, result.currency))
    }

    // 25. Zero-decimal currency parsing and rejection of decimals
    @Test
    fun test25_zeroDecimalCurrency_parsesWholeNumbersAndRejectsFractions() {
        val valid = MoneyParser.parse("500", CurrencyRepository.JPY)
        assertTrue(valid is ParseResult.Success)
        assertEquals(500L, (valid as ParseResult.Success).minorUnits)

        val invalid = MoneyParser.parse("500.50", CurrencyRepository.JPY)
        assertTrue(invalid is ParseResult.Error)
    }

    // 26. Two-decimal currency parsing
    @Test
    fun test26_twoDecimalCurrency_parsesWholeAndDecimalInputs() {
        val r1 = MoneyParser.parse("5", CurrencyRepository.USD) as ParseResult.Success
        val r2 = MoneyParser.parse("25.50", CurrencyRepository.USD) as ParseResult.Success
        val r3 = MoneyParser.parse("1000.00", CurrencyRepository.USD) as ParseResult.Success
        assertEquals(500L, r1.minorUnits)
        assertEquals(2550L, r2.minorUnits)
        assertEquals(100000L, r3.minorUnits)
    }

    // 27. Custom currency (0-4 decimal places)
    @Test
    fun test27_customCurrency_supportsThreeAndFourDecimalPlaces() {
        val kwd = CurrencyRepository.createCustomCurrency("KWD", "KD", "Kuwaiti Dinar", 3)
        val parsed = MoneyParser.parse("12.345", kwd) as ParseResult.Success
        assertEquals(12345L, parsed.minorUnits)
        assertEquals("KD 12.345", MoneyFormatter.format(12345L, kwd))

        val token4 = CurrencyRepository.createCustomCurrency("UNIT", "U$", "Four Decimals", 4)
        val parsed4 = MoneyParser.parse("1.2345", token4) as ParseResult.Success
        assertEquals(12345L, parsed4.minorUnits)
        assertEquals("U$1.2345", MoneyFormatter.format(12345L, token4))
    }

    // 28. Money parsing edge cases
    @Test
    fun test28_moneyParsing_handlesCommasAndRejectsInvalidInput() {
        val withCommas = MoneyParser.parse("1,250.75", CurrencyRepository.USD) as ParseResult.Success
        assertEquals(125075L, withCommas.minorUnits)

        assertTrue(MoneyParser.parse("", CurrencyRepository.USD) is ParseResult.Error)
        assertTrue(MoneyParser.parse("abc", CurrencyRepository.USD) is ParseResult.Error)
        assertTrue(MoneyParser.parse("12.34.56", CurrencyRepository.USD) is ParseResult.Error)
        assertTrue(MoneyParser.parse("12.345", CurrencyRepository.USD) is ParseResult.Error)
    }

    // 29. Money formatting
    @Test
    fun test29_moneyFormatting_handlesThousandsSeparatorsAndZero() {
        assertEquals("$0.00", MoneyFormatter.format(0L, CurrencyRepository.USD))
        assertEquals("$1,000,000.00", MoneyFormatter.format(100_000_000L, CurrencyRepository.USD))
        assertEquals("¥0", MoneyFormatter.format(0L, CurrencyRepository.JPY))
    }

    // 30. Average rounding (CRITICAL TEST — AVERAGE ROUNDING: $10, $20, $20 -> $50, avg $16.67)
    @Test
    fun test30_averageRounding_criticalTest() {
        val items = listOf(
            ExpenseItem("1", "A", 1000L),
            ExpenseItem("2", "B", 2000L),
            ExpenseItem("3", "C", 2000L)
        )
        val result = ExpenseSumCalculator.calculate(items, CurrencyRepository.USD)
        assertEquals(5000L, result.totalMinorUnits)
        assertEquals(1667L, result.averageMinorUnits)
        assertEquals("$16.67", ExpenseSumFormatter.formatAverage(result))
    }

    // 31. Large expense values (CRITICAL TEST — LARGE VALUE)
    @Test
    fun test31_largeExpenseValues_calculatesWithoutOverflow() {
        val maxSingle = MoneyParser.MAX_MINOR_UNITS // 1,000,000,000,000 minor units
        val items = listOf(
            ExpenseItem("1", "Large 1", maxSingle),
            ExpenseItem("2", "Large 2", maxSingle)
        )
        val result = ExpenseSumCalculator.calculate(items, CurrencyRepository.USD)
        assertEquals(2_000_000_000_000L, result.totalMinorUnits)
    }

    // 32. Large expense list (100+ expenses)
    @Test
    fun test32_largeExpenseList_supports100PlusExpenses() {
        val items = (1..120).map { idx ->
            ExpenseItem(id = "id-$idx", label = "Expense $idx", amountMinorUnits = 1000L)
        }
        val result = ExpenseSumCalculator.calculate(items, CurrencyRepository.USD)
        assertEquals(120, result.count)
        assertEquals(120_000L, result.totalMinorUnits)
        assertEquals(1000L, result.averageMinorUnits)
    }

    // 33. Overflow protection (CRITICAL TEST — OVERFLOW)
    @Test
    fun test33_overflowProtection_throwsErrorAndNeverProducesNegativeTotal() {
        val hugeItems = listOf(
            ExpenseItem("1", "Huge 1", Long.MAX_VALUE - 10L),
            ExpenseItem("2", "Huge 2", 100L)
        )
        try {
            ExpenseSumCalculator.calculate(hugeItems, CurrencyRepository.USD)
            fail("Expected overflow exception")
        } catch (e: ExpenseSumCalculationException) {
            assertNotNull(e.message)
        }

        // Also test exceeding MAX_TOTAL_MINOR_UNITS via many max items
        val tooManyMaxItems = (1..105).map { idx ->
            ExpenseItem("$idx", "Max $idx", ExpenseSumValidator.MAX_AMOUNT_MINOR_UNITS)
        }
        try {
            ExpenseSumCalculator.calculate(tooManyMaxItems, CurrencyRepository.USD)
            fail("Expected total too large exception")
        } catch (e: ExpenseSumCalculationException) {
            assertEquals("Total is too large.", e.message)
        }
    }

    // 34. Core total invariant (CRITICAL TEST — TOTAL INVARIANT)
    @Test
    fun test34_coreTotalInvariant_alwaysEqualsSumOfAmountMinorUnits() {
        val sampleLists = listOf(
            emptyList(),
            listOf(ExpenseItem("1", "A", 0L)),
            listOf(
                ExpenseItem("1", "A", 333L),
                ExpenseItem("2", "B", 333L),
                ExpenseItem("3", "C", 334L)
            ),
            listOf(
                ExpenseItem("1", "Coffee", 525L),
                ExpenseItem("2", "Fuel", 4189L),
                ExpenseItem("3", "Lunch", 1299L),
                ExpenseItem("4", "Parking", 700L),
                ExpenseItem("5", "Supplies", 1995L)
            )
        )
        for (list in sampleLists) {
            val result = ExpenseSumCalculator.calculate(list, CurrencyRepository.USD)
            assertEquals(list.sumOf { it.amountMinorUnits }, result.totalMinorUnits)
        }
    }

    // 35. Largest tie handling (CRITICAL TEST — LARGEST TIE)
    @Test
    fun test35_largestTieHandling_usesFirstInListOrder() {
        val items = listOf(
            ExpenseItem(id = "id-A", label = "A", amountMinorUnits = 2000L),
            ExpenseItem(id = "id-B", label = "B", amountMinorUnits = 2000L),
            ExpenseItem(id = "id-C", label = "C", amountMinorUnits = 1000L)
        )
        val result = ExpenseSumCalculator.calculate(items, CurrencyRepository.USD)
        assertEquals(2000L, result.largestMinorUnits)
        assertEquals("id-A", result.largestExpenseId)
    }

    // 36. Smallest tie handling (CRITICAL TEST — SMALLEST TIE)
    @Test
    fun test36_smallestTieHandling_usesFirstInListOrder() {
        val items = listOf(
            ExpenseItem(id = "id-A", label = "A", amountMinorUnits = 1000L),
            ExpenseItem(id = "id-B", label = "B", amountMinorUnits = 1000L),
            ExpenseItem(id = "id-C", label = "C", amountMinorUnits = 2000L)
        )
        val result = ExpenseSumCalculator.calculate(items, CurrencyRepository.USD)
        assertEquals(1000L, result.smallestMinorUnits)
        assertEquals("id-A", result.smallestExpenseId)
    }

    private fun createFreshViewModel(
        store: InMemoryKeyValueStore = InMemoryKeyValueStore(),
        clearInitialSamples: Boolean = true
    ): ExpenseSumViewModel {
        val settingsRepo = SettingsRepository(store)
        val historyRepo = ExpenseSumHistoryRepository(store)
        val vm = ExpenseSumViewModel(settingsRepo, historyRepo)
        if (clearInitialSamples) {
            vm.confirmClearAll()
        }
        return vm
    }

    // 37. Quick add (QUICK ADD TEST)
    @Test
    fun test37_quickAdd_addsExpensesAndClearsField() {
        val vm = createFreshViewModel()
        vm.updateQuickAddInput("10")
        assertTrue(vm.quickAddExpense())
        assertEquals("", vm.uiState.value.quickAddInput)
        assertEquals(1, vm.uiState.value.expenses.size)
        assertEquals(1000L, vm.uiState.value.result?.totalMinorUnits)

        vm.updateQuickAddInput("25")
        assertTrue(vm.quickAddExpense())
        assertEquals(2, vm.uiState.value.expenses.size)
        assertEquals(3500L, vm.uiState.value.result?.totalMinorUnits)
    }

    // 38. Add expense with optional label
    @Test
    fun test38_addExpense_addsLabeledAndUnlabeledItems() {
        val vm = createFreshViewModel()
        assertTrue(vm.addExpense("Coffee", "5.50"))
        assertTrue(vm.addExpense("", "14.50"))
        val state = vm.uiState.value
        assertEquals(2, state.expenses.size)
        assertEquals("Coffee", state.expenses[0].label)
        assertEquals("Expense 2", state.expenses[1].label)
        assertEquals(2000L, state.result?.totalMinorUnits)
    }

    // 39. Edit expense (EDIT TEST)
    @Test
    fun test39_editExpense_updatesTotalImmediately() {
        val vm = createFreshViewModel()
        vm.addExpense("Item", "10")
        val id = vm.uiState.value.expenses.first().id
        assertEquals(1000L, vm.uiState.value.result?.totalMinorUnits)

        assertTrue(vm.editExpense(id, "Updated Item", "25"))
        assertEquals("Updated Item", vm.uiState.value.expenses.first().label)
        assertEquals(2500L, vm.uiState.value.result?.totalMinorUnits)
    }

    // 40. Delete expense (DELETE TEST)
    @Test
    fun test40_deleteExpense_updatesTotalAndCountImmediately() {
        val vm = createFreshViewModel()
        vm.addExpense("A", "10")
        vm.addExpense("B", "20")
        vm.addExpense("C", "30")
        val middleId = vm.uiState.value.expenses[1].id

        vm.deleteExpense(middleId)
        val state = vm.uiState.value
        assertEquals(2, state.expenses.size)
        assertEquals(4000L, state.result?.totalMinorUnits)
        assertEquals(2, state.result?.count)
    }

    // 41. Clear all (CLEAR ALL TEST)
    @Test
    fun test41_clearAll_respectsConfirmationAndEmptiesList() {
        val vm = createFreshViewModel()
        vm.addExpense("Coffee", "5.00")
        vm.requestClearAll()
        assertTrue(vm.uiState.value.showClearAllConfirmation)

        vm.confirmClearAll()
        assertFalse(vm.uiState.value.showClearAllConfirmation)
        assertTrue(vm.uiState.value.expenses.isEmpty())
        assertEquals(0L, vm.uiState.value.result?.totalMinorUnits)
    }

    // 42. Quick presets
    @Test
    fun test42_quickPresets_addsPresetDirectlyInSelectedCurrency() {
        val vm = createFreshViewModel()
        assertTrue(vm.addPresetExpense(10L))
        assertTrue(vm.addPresetExpense(50L))
        assertEquals(2, vm.uiState.value.expenses.size)
        assertEquals(6000L, vm.uiState.value.result?.totalMinorUnits)
    }

    // 43. Currency switching confirmation and numerical preservation
    @Test
    fun test43_currencySwitching_promptsConfirmationWhenListNotEmpty() {
        val vm = createFreshViewModel()
        vm.addExpense("Lunch", "15.00")

        vm.requestCurrencyChange(CurrencyRepository.EUR)
        assertEquals(CurrencyRepository.EUR, vm.uiState.value.pendingCurrencyChange)
        assertEquals(CurrencyRepository.USD, vm.uiState.value.currency)

        // Cancel keeps USD
        vm.cancelCurrencyChange()
        assertNull(vm.uiState.value.pendingCurrencyChange)
        assertEquals(CurrencyRepository.USD, vm.uiState.value.currency)

        // Confirm switches to EUR and retains numerical value 15.00 (1500 minor units)
        vm.requestCurrencyChange(CurrencyRepository.EUR)
        vm.confirmCurrencyChange()
        assertEquals(CurrencyRepository.EUR, vm.uiState.value.currency)
        assertEquals(1500L, vm.uiState.value.result?.totalMinorUnits)
    }

    // 44. History save
    @Test
    fun test44_historySave_storesCurrentExpenseList() {
        val vm = createFreshViewModel()
        vm.updateTitle("Weekend Expenses")
        vm.addExpense("Coffee", "5.00")
        vm.addExpense("Fuel", "40.00")

        assertTrue(vm.saveToHistory())
        assertEquals(1, vm.uiState.value.history.size)
        assertEquals("Weekend Expenses", vm.uiState.value.history.first().title)
    }

    // 45. History load across ViewModel instances
    @Test
    fun test45_historyLoad_persistsAcrossInstances() {
        val store = InMemoryKeyValueStore()
        val vm1 = createFreshViewModel(store)
        vm1.updateTitle("Trip")
        vm1.addExpense("Hotel", "120.00")
        vm1.saveToHistory()

        val vm2 = ExpenseSumViewModel(SettingsRepository(store), ExpenseSumHistoryRepository(store))
        assertEquals(1, vm2.uiState.value.history.size)
        assertEquals("Trip", vm2.uiState.value.history.first().title)
    }

    // 46. History limit (max 10 entries, adding 11th removes oldest)
    @Test
    fun test46_historyLimit_enforcesMax10AndEvictsOldest() {
        val store = InMemoryKeyValueStore()
        val repo = ExpenseSumHistoryRepository(store)

        for (i in 1..11) {
            repo.saveToHistory(
                title = "List $i",
                expenses = listOf(ExpenseItem("id-$i", "Item $i", i * 100L)),
                currency = CurrencyRepository.USD,
                timestampMillis = i * 1000L
            )
        }

        val history = repo.loadHistory()
        assertEquals(10, history.size)
        assertEquals("List 11", history.first().title)
        assertEquals("List 2", history.last().title)
        assertFalse(history.any { it.title == "List 1" })
    }

    // 47. History deletion
    @Test
    fun test47_historyDeletion_removesIndividualEntry() {
        val vm = createFreshViewModel()
        vm.updateTitle("List A")
        vm.addExpense("Item 1", "10.00")
        vm.saveToHistory()

        vm.updateTitle("List B")
        vm.addExpense("Item 2", "20.00")
        vm.saveToHistory()

        assertEquals(2, vm.uiState.value.history.size)
        val firstId = vm.uiState.value.history.first().id
        vm.requestDeleteHistoryItem(firstId)
        vm.confirmDeleteHistoryItem(firstId)
        assertEquals(1, vm.uiState.value.history.size)
    }

    // 48. History clear
    @Test
    fun test48_historyClear_removesAllSavedLists() {
        val vm = createFreshViewModel()
        vm.addExpense("A", "10")
        vm.saveToHistory()
        assertEquals(1, vm.uiState.value.history.size)

        vm.requestClearHistory()
        vm.confirmClearHistory()
        assertTrue(vm.uiState.value.history.isEmpty())
    }

    // 49. History restore (HISTORY RESTORE TEST)
    @Test
    fun test49_historyRestore_restoresTitleCurrencyExpensesAndRecalculates() {
        val vm = createFreshViewModel()
        vm.requestCurrencyChange(CurrencyRepository.GBP)
        vm.updateTitle("London Trip")
        vm.addExpense("Tube", "6.50")
        vm.addExpense("Lunch", "18.50")
        vm.saveToHistory()
        val savedItem = vm.uiState.value.history.first()

        // Clear and switch currency
        vm.confirmClearAll()
        vm.requestCurrencyChange(CurrencyRepository.USD)
        assertEquals(0L, vm.uiState.value.result?.totalMinorUnits)

        // Restore from history
        vm.restoreHistory(savedItem)
        val state = vm.uiState.value
        assertEquals("London Trip", state.title)
        assertEquals("GBP", state.currency.code)
        assertEquals(2, state.expenses.size)
        assertEquals(2500L, state.result?.totalMinorUnits)
    }

    // 50. Settings persistence (SETTINGS TEST: Dark mode + Default Currency = PKR)
    @Test
    fun test50_settingsPersistence_persistsDarkModeAndCurrency() {
        val store = InMemoryKeyValueStore()
        val vm1 = createFreshViewModel(store)
        vm1.updateThemeMode(ThemeMode.DARK)
        vm1.updateDefaultCurrency(CurrencyRepository.PKR)

        val vm2 = ExpenseSumViewModel(SettingsRepository(store), ExpenseSumHistoryRepository(store))
        assertEquals(ThemeMode.DARK, vm2.uiState.value.settings.themeMode)
        assertEquals("PKR", vm2.uiState.value.settings.defaultCurrency.code)
        assertEquals("PKR", vm2.uiState.value.currency.code)
    }

    // 51. Last-list persistence (Preserve Last List On vs Off)
    @Test
    fun test51_lastListPersistence_restoresWhenEnabledAndOpensEmptyWhenDisabled() {
        val store = InMemoryKeyValueStore()
        val vm1 = createFreshViewModel(store)
        vm1.updateTitle("Groceries")
        vm1.addExpense("Milk", "4.25")
        vm1.addExpense("Bread", "3.75")

        // Reopen with Preserve Last List = true (default)
        val vm2 = ExpenseSumViewModel(SettingsRepository(store), ExpenseSumHistoryRepository(store))
        assertEquals("Groceries", vm2.uiState.value.title)
        assertEquals(2, vm2.uiState.value.expenses.size)
        assertEquals(800L, vm2.uiState.value.result?.totalMinorUnits)

        // Disable Preserve Last List and reopen
        vm2.updatePreserveLastList(false)
        val vm3 = ExpenseSumViewModel(SettingsRepository(store), ExpenseSumHistoryRepository(store))
        assertTrue(vm3.uiState.value.expenses.isEmpty())
        assertEquals(0L, vm3.uiState.value.result?.totalMinorUnits)
    }

    // 52. Reset settings
    @Test
    fun test52_resetSettings_restoresDefaultPreferences() {
        val vm = createFreshViewModel()
        vm.updateThemeMode(ThemeMode.DARK)
        vm.updateDynamicColor(false)
        vm.updateHapticFeedback(false)
        vm.updateAnimations(false)
        vm.updateConfirmClearAll(false)

        vm.requestResetSettings()
        vm.confirmResetSettings()

        val settings = vm.uiState.value.settings
        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertTrue(settings.dynamicColor)
        assertTrue(settings.hapticFeedback)
        assertTrue(settings.animations)
        assertTrue(settings.confirmClearAll)
        assertEquals("USD", settings.defaultCurrency.code)
    }

    // 53. Copy formatting
    @Test
    fun test53_copyFormatting_matchesSpecificationFormat() {
        val items = listOf(
            ExpenseItem("1", "Coffee", 500L),
            ExpenseItem("2", "Fuel", 4000L),
            ExpenseItem("3", "Lunch", 1250L),
            ExpenseItem("4", "Parking", 500L)
        )
        val result = ExpenseSumCalculator.calculate(items, CurrencyRepository.USD)
        val copyText = ExpenseSumFormatter.formatCopyText(result)

        val expected = """
            ExpenseSum Result

            Coffee: $5.00
            Fuel: $40.00
            Lunch: $12.50
            Parking: $5.00

            Total: $62.50
            Expenses: 4
            Average: $15.63
            Largest: $40.00
            Smallest: $5.00
        """.trimIndent()

        assertEquals(expected, copyText)
    }

    // 54. Share formatting
    @Test
    fun test54_shareFormatting_matchesSpecificationFormat() {
        val items = listOf(
            ExpenseItem("1", "Coffee", 500L),
            ExpenseItem("2", "Fuel", 4000L),
            ExpenseItem("3", "Lunch", 1250L),
            ExpenseItem("4", "Parking", 500L)
        )
        val result = ExpenseSumCalculator.calculate(items, CurrencyRepository.USD)
        val shareText = ExpenseSumFormatter.formatShareText(result)

        val expected = """
            ExpenseSum

            Coffee: $5.00
            Fuel: $40.00
            Lunch: $12.50
            Parking: $5.00

            Total: $62.50
            4 expenses
            Average: $15.63
        """.trimIndent()

        assertEquals(expected, shareText)
    }

    // 55. Rapid input protection (prevents duplicate history entries & stale totals on incomplete edits)
    @Test
    fun test55_rapidInputProtection_preventsDuplicateHistoryAndPreservesTotalOnIncompleteEdit() {
        val vm = createFreshViewModel()
        vm.addExpense("Coffee", "5.00")
        vm.addExpense("Lunch", "15.00")

        // Saving identical list twice in a row does not create duplicate history entries
        assertTrue(vm.saveToHistory())
        assertFalse(vm.saveToHistory())
        assertEquals(1, vm.uiState.value.history.size)

        // Incomplete edit input (e.g. "12.") does not corrupt the existing calculated total ($20.00)
        val firstId = vm.uiState.value.expenses.first().id
        assertFalse(vm.editExpense(firstId, "Coffee", "12."))
        assertEquals(2000L, vm.uiState.value.result?.totalMinorUnits)
    }
}

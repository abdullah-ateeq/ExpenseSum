package com.example.expensesum.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensesum.domain.CurrencyRepository
import com.example.expensesum.domain.ExpenseSumFormatter
import com.example.expensesum.domain.ExpenseSumValidator
import com.example.expensesum.domain.MoneyFormatter
import com.example.expensesum.model.CurrencyOption
import com.example.expensesum.model.ExpenseItem
import com.example.expensesum.model.ExpenseSumResult
import com.example.expensesum.model.ExpenseSumUiState
import com.example.expensesum.ui.ExpenseSumViewModel
import com.example.expensesum.util.HapticHelper
import com.example.expensesum.util.ShareAndCopyHelper

@Composable
fun HomeScreen(
    uiState: ExpenseSumUiState,
    viewModel: ExpenseSumViewModel,
    onShowSnackbar: (String) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val focusManager = LocalFocusManager.current
    val hapticsEnabled = uiState.settings.hapticFeedback
    val animationsEnabled = uiState.settings.animations

    val result = uiState.result

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header + Currency Selector
                item(key = "header") {
                    HomeHeaderSection(
                        currency = uiState.currency,
                        onSelectCurrency = { selected ->
                            HapticHelper.performLightClick(view, hapticsEnabled)
                            viewModel.requestCurrencyChange(selected)
                        },
                        onSelectCustomCurrency = {
                            HapticHelper.performLightClick(view, hapticsEnabled)
                            viewModel.openCustomCurrencyDialog()
                        }
                    )
                }

                // Optional Expense List Title
                item(key = "title_input") {
                    OutlinedTextField(
                        value = uiState.title,
                        onValueChange = { viewModel.updateTitle(it) },
                        label = { Text("List Title (optional)") },
                        placeholder = { Text("e.g. Weekend Expenses") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                // Main Result Summary Card (Hero Total + Count + Average + Largest + Smallest)
                item(key = "summary_card") {
                    ResultSummaryCard(
                        result = result,
                        currency = uiState.currency,
                        animationsEnabled = animationsEnabled
                    )
                }

                // Subtle Horizontal Comparison Visualization (when expenses exist)
                if (result != null && result.count > 0 && result.totalMinorUnits > 0L) {
                    item(key = "comparison_visualization") {
                        ComparisonVisualizationCard(
                            result = result,
                            currency = uiState.currency
                        )
                    }
                }

                // Home Actions Row (SAVE, COPY, SHARE, CLEAR ALL)
                item(key = "home_actions") {
                    HomeActionButtonsRow(
                        hasExpenses = uiState.expenses.isNotEmpty(),
                        onSave = {
                            HapticHelper.performConfirm(view, hapticsEnabled)
                            val added = viewModel.saveToHistory()
                            onShowSnackbar(
                                if (added) "Saved to history" else "Already saved in recent history"
                            )
                        },
                        onCopy = {
                            HapticHelper.performLightClick(view, hapticsEnabled)
                            val copyText = viewModel.prepareCopyText()
                            ShareAndCopyHelper.copyToClipboard(context, "ExpenseSum Result", copyText)
                            onShowSnackbar("Total copied to clipboard")
                        },
                        onShare = {
                            HapticHelper.performLightClick(view, hapticsEnabled)
                            val shareText = viewModel.prepareShareText()
                            ShareAndCopyHelper.shareText(context, shareText)
                        },
                        onClearAll = {
                            HapticHelper.performLightClick(view, hapticsEnabled)
                            viewModel.requestClearAll()
                        }
                    )
                }

                // Error Banner if any validation error occurred
                if (uiState.errorMessage != null) {
                    item(key = "error_banner") {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = uiState.errorMessage,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { viewModel.clearErrorMessage() }) {
                                    Text("Dismiss")
                                }
                            }
                        }
                    }
                }

                // Expense List Section Header + "+ ADD EXPENSE" button
                item(key = "expenses_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "EXPENSES (${uiState.expenses.size})",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        FilledTonalButton(
                            onClick = {
                                HapticHelper.performLightClick(view, hapticsEnabled)
                                viewModel.openAddExpenseDialog()
                            },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.semantics {
                                contentDescription = "Add expense."
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+ ADD EXPENSE", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Empty State vs Expense Rows
                if (uiState.expenses.isEmpty()) {
                    item(key = "empty_state") {
                        EmptyExpensesCard(
                            currency = uiState.currency,
                            onAddExpenseClick = {
                                HapticHelper.performLightClick(view, hapticsEnabled)
                                viewModel.openAddExpenseDialog()
                            }
                        )
                    }
                } else {
                    itemsIndexed(
                        items = uiState.expenses,
                        key = { _, item -> item.id }
                    ) { index, item ->
                        val isLargest = result?.largestExpenseId == item.id && (result.count > 1)
                        val isSmallest = result?.smallestExpenseId == item.id &&
                                (result.count > 1) &&
                                (result.largestExpenseId != result.smallestExpenseId)

                        ExpenseItemRow(
                            index = index,
                            item = item,
                            currency = uiState.currency,
                            isLargest = isLargest,
                            isSmallest = isSmallest,
                            modifier = if (animationsEnabled) Modifier.animateItem() else Modifier,
                            onEdit = {
                                HapticHelper.performLightClick(view, hapticsEnabled)
                                viewModel.openEditExpenseDialog(item)
                            },
                            onDelete = {
                                HapticHelper.performLightClick(view, hapticsEnabled)
                                viewModel.deleteExpense(item.id)
                            }
                        )
                    }
                }
            }

            // Bottom Quick-Add Bar + Presets
            QuickAddBottomBar(
                quickAddInput = uiState.quickAddInput,
                currency = uiState.currency,
                onInputChange = { viewModel.updateQuickAddInput(it) },
                onQuickAddSubmit = {
                    HapticHelper.performLightClick(view, hapticsEnabled)
                    viewModel.quickAddExpense()
                },
                onPresetClick = { majorAmount ->
                    HapticHelper.performLightClick(view, hapticsEnabled)
                    viewModel.addPresetExpense(majorAmount)
                }
            )
        }
    }

    // Dialogs
    if (uiState.showAddExpenseDialog) {
        ExpenseEntryDialog(
            title = "Add Expense",
            confirmText = "Add",
            currency = uiState.currency,
            onConfirm = { label, amount ->
                val ok = viewModel.addExpense(label, amount)
                if (ok) HapticHelper.performConfirm(view, hapticsEnabled)
                ok
            },
            onDismiss = { viewModel.closeAddExpenseDialog() }
        )
    }

    val editingExpense = uiState.editingExpense
    if (editingExpense != null) {
        ExpenseEntryDialog(
            title = "Edit Expense",
            confirmText = "Done",
            currency = uiState.currency,
            initialLabel = editingExpense.label,
            initialAmount = MoneyFormatter.formatPlainInput(
                editingExpense.amountMinorUnits,
                uiState.currency
            ),
            liveUpdateOnEdit = { liveLabel, liveAmount ->
                viewModel.editExpense(editingExpense.id, liveLabel, liveAmount)
            },
            onConfirm = { label, amount ->
                val ok = viewModel.editExpense(editingExpense.id, label, amount)
                if (ok) {
                    HapticHelper.performConfirm(view, hapticsEnabled)
                    viewModel.closeEditExpenseDialog()
                }
                ok
            },
            onDismiss = { viewModel.closeEditExpenseDialog() }
        )
    }

    if (uiState.showCustomCurrencyDialog) {
        CustomCurrencyDialog(
            onConfirm = { customCurrency ->
                viewModel.requestCurrencyChange(customCurrency)
            },
            onDismiss = { viewModel.closeCustomCurrencyDialog() }
        )
    }

    val pendingCurrency = uiState.pendingCurrencyChange
    if (pendingCurrency != null) {
        CurrencyChangeConfirmDialog(
            targetCurrency = pendingCurrency,
            onConfirm = {
                HapticHelper.performConfirm(view, hapticsEnabled)
                viewModel.confirmCurrencyChange()
            },
            onCancel = { viewModel.cancelCurrencyChange() }
        )
    }

    if (uiState.showClearAllConfirmation) {
        ClearAllConfirmDialog(
            onConfirm = {
                HapticHelper.performConfirm(view, hapticsEnabled)
                viewModel.confirmClearAll()
            },
            onCancel = { viewModel.dismissClearAllConfirmation() }
        )
    }
}

@Composable
private fun HomeHeaderSection(
    currency: CurrencyOption,
    onSelectCurrency: (CurrencyOption) -> Unit,
    onSelectCustomCurrency: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "ExpenseSum",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Quickly total a list of expenses.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box {
            OutlinedButton(
                onClick = { menuExpanded = true },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "${currency.code} (${currency.symbol})",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select currency"
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                CurrencyRepository.supportedCurrencies.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text("${option.code} (${option.symbol}) — ${option.name}")
                        },
                        onClick = {
                            menuExpanded = false
                            onSelectCurrency(option)
                        }
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Other (Custom Currency…)",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onSelectCustomCurrency()
                    }
                )
            }
        }
    }
}

@Composable
private fun ResultSummaryCard(
    result: ExpenseSumResult?,
    currency: CurrencyOption,
    animationsEnabled: Boolean
) {
    val totalMinor = result?.totalMinorUnits ?: 0L
    val count = result?.count ?: 0
    val formattedTotal = ExpenseSumFormatter.formatMoney(totalMinor, currency)
    val countBadge = ExpenseSumFormatter.formatCountBadge(count)
    val averageText = if (result != null) ExpenseSumFormatter.formatAverage(result) else "—"
    val largestText = ExpenseSumFormatter.formatOptionalMoney(result?.largestMinorUnits, currency)
    val smallestText = ExpenseSumFormatter.formatOptionalMoney(result?.smallestMinorUnits, currency)

    val totalAccDesc = "Total expenses, ${MoneyFormatter.formatAccessibleAmount(totalMinor, currency)}."
    val avgAccDesc = if (count > 0 && result != null) {
        "Average expense, ${MoneyFormatter.formatAccessibleAmount(result.averageMinorUnits, currency)}."
    } else {
        "Average expense, none."
    }

    ElevatedCard(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOTAL EXPENSES",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = countBadge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (animationsEnabled) {
                AnimatedContent(
                    targetState = formattedTotal,
                    transitionSpec = {
                        fadeIn(tween(220)) togetherWith fadeOut(tween(180))
                    },
                    label = "total_amount_anim"
                ) { targetTotal ->
                    Text(
                        text = targetTotal,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.semantics {
                            contentDescription = totalAccDesc
                        }
                    )
                }
            } else {
                Text(
                    text = formattedTotal,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.semantics {
                        contentDescription = totalAccDesc
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricColumn(
                        label = "Average",
                        value = averageText,
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = avgAccDesc }
                    )
                    MetricColumn(
                        label = "Largest",
                        value = largestText,
                        modifier = Modifier.weight(1f)
                    )
                    MetricColumn(
                        label = "Smallest",
                        value = smallestText,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ComparisonVisualizationCard(
    result: ExpenseSumResult,
    currency: CurrencyOption
) {
    val total = result.totalMinorUnits.coerceAtLeast(1L)
    val largest = result.largestMinorUnits ?: 0L
    val smallest = result.smallestMinorUnits ?: 0L

    val largestRatio = (largest.toFloat() / total.toFloat()).coerceIn(0.06f, 1f)
    val smallestRatio = (smallest.toFloat() / total.toFloat()).coerceIn(0.04f, 1f)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ComparisonBarRow(
                label = "Largest",
                amountText = ExpenseSumFormatter.formatMoney(largest, currency),
                fraction = largestRatio,
                barColor = MaterialTheme.colorScheme.primary
            )
            ComparisonBarRow(
                label = "Smallest",
                amountText = ExpenseSumFormatter.formatMoney(smallest, currency),
                fraction = smallestRatio,
                barColor = MaterialTheme.colorScheme.tertiary
            )
            ComparisonBarRow(
                label = "Total",
                amountText = ExpenseSumFormatter.formatMoney(result.totalMinorUnits, currency),
                fraction = 1f,
                barColor = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun ComparisonBarRow(
    label: String,
    amountText: String,
    fraction: Float,
    barColor: androidx.compose.ui.graphics.Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = amountText,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(barColor)
            )
        }
    }
}

@Composable
private fun HomeActionButtonsRow(
    hasExpenses: Boolean,
    onSave: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onClearAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilledTonalButton(
            onClick = onSave,
            enabled = hasExpenses,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.BookmarkBorder,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("SAVE", fontWeight = FontWeight.SemiBold)
        }

        OutlinedButton(
            onClick = onCopy,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("COPY", fontWeight = FontWeight.SemiBold)
        }

        OutlinedButton(
            onClick = onShare,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("SHARE", fontWeight = FontWeight.SemiBold)
        }

        OutlinedButton(
            onClick = onClearAll,
            enabled = hasExpenses,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("CLEAR ALL", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EmptyExpensesCard(
    currency: CurrencyOption,
    onAddExpenseClick: () -> Unit
) {
    val fiveMinor = CurrencyRepository.scaleMajorAmountToMinorUnits(5L, currency)
    val exampleFormatted = ExpenseSumFormatter.formatMoney(fiveMinor, currency)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No expenses yet",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Add your first expense to see the total.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onAddExpenseClick,
                modifier = Modifier.semantics {
                    contentDescription = "Add expense."
                }
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("+ ADD EXPENSE", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Example: Coffee — $exampleFormatted",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun ExpenseItemRow(
    index: Int,
    item: ExpenseItem,
    currency: CurrencyOption,
    isLargest: Boolean,
    isSmallest: Boolean,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val displayLabel = ExpenseSumValidator.resolveDisplayLabel(item.label, index)
    val formattedAmount = ExpenseSumFormatter.formatMoney(item.amountMinorUnits, currency)
    val rowAccessibility = "$displayLabel, ${MoneyFormatter.formatAccessibleAmount(item.amountMinorUnits, currency)}."
    val deleteAccessibility = "Delete $displayLabel expense."

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .semantics { contentDescription = rowAccessibility }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = displayLabel,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (isLargest) {
                        BadgePill(
                            text = "Largest",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else if (isSmallest) {
                        BadgePill(
                            text = "Smallest",
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
                Text(
                    text = "Tap to edit label or amount",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = formattedAmount,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit $displayLabel expense",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .semantics { contentDescription = deleteAccessibility }
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = deleteAccessibility,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BadgePill(
    text: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = contentColor
        )
    }
}

@Composable
private fun QuickAddBottomBar(
    quickAddInput: String,
    currency: CurrencyOption,
    onInputChange: (String) -> Unit,
    onQuickAddSubmit: () -> Unit,
    onPresetClick: (Long) -> Unit
) {
    val presets = remember(currency) {
        CurrencyRepository.getQuickPresetsMajorUnits(currency)
    }

    Surface(
        tonalElevation = 4.dp,
        shadowElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Quick Presets Row ($5 · $10 · $20 · $50 · $100)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                presets.forEach { presetMajor ->
                    val labelText = MoneyFormatter.formatPresetButton(presetMajor, currency)
                    AssistChip(
                        onClick = { onPresetClick(presetMajor) },
                        label = {
                            Text(
                                text = labelText,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    )
                }
            }

            // Compact Quick-Add Input Field + [+] Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = quickAddInput,
                    onValueChange = onInputChange,
                    label = { Text("Add amount") },
                    prefix = { Text("${currency.symbol} ") },
                    placeholder = {
                        Text(if (currency.decimalPlaces == 0) "1000" else "10.00")
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (currency.decimalPlaces == 0) {
                            KeyboardType.Number
                        } else {
                            KeyboardType.Decimal
                        },
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { onQuickAddSubmit() }
                    ),
                    modifier = Modifier.weight(1f)
                )

                FilledIconButton(
                    onClick = onQuickAddSubmit,
                    modifier = Modifier
                        .size(54.dp)
                        .semantics { contentDescription = "Add expense." },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add expense."
                    )
                }
            }
        }
    }
}

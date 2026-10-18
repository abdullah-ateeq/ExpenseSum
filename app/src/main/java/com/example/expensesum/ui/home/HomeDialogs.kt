package com.example.expensesum.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.expensesum.domain.CurrencyRepository
import com.example.expensesum.domain.ExpenseSumValidator
import com.example.expensesum.domain.MoneyFormatter
import com.example.expensesum.model.CurrencyOption
import com.example.expensesum.model.ExpenseItem

@Composable
fun ExpenseEntryDialog(
    title: String,
    confirmText: String,
    currency: CurrencyOption,
    initialLabel: String = "",
    initialAmount: String = "",
    liveUpdateOnEdit: ((String, String) -> Unit)? = null,
    onConfirm: (label: String, amount: String) -> Boolean,
    onDismiss: () -> Unit
) {
    var label by remember(initialLabel) { mutableStateOf(initialLabel) }
    var amount by remember(initialAmount) { mutableStateOf(initialAmount) }
    var localError by remember { mutableStateOf<String?>(null) }
    val amountFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (initialAmount.isEmpty()) {
            amountFocusRequester.requestFocus()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { newValue ->
                        if (newValue.length <= ExpenseSumValidator.MAX_LABEL_LENGTH) {
                            label = newValue
                            localError = null
                            liveUpdateOnEdit?.invoke(newValue, amount)
                        }
                    },
                    label = { Text("Label (optional)") },
                    placeholder = { Text("e.g. Coffee, Fuel, Lunch") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    supportingText = {
                        Text("${label.trim().length}/${ExpenseSumValidator.MAX_LABEL_LENGTH}")
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { newValue ->
                        if (newValue.length <= 18) {
                            amount = newValue
                            localError = if (newValue.trim().startsWith("-")) {
                                "Expense amount cannot be negative."
                            } else {
                                null
                            }
                            liveUpdateOnEdit?.invoke(label, newValue)
                        }
                    },
                    label = { Text("Amount (${currency.code})") },
                    prefix = { Text("${currency.symbol} ") },
                    placeholder = {
                        Text(if (currency.decimalPlaces == 0) "500" else "5.00")
                    },
                    singleLine = true,
                    isError = localError != null,
                    supportingText = localError?.let { err ->
                        { Text(text = err, color = MaterialTheme.colorScheme.error) }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (currency.decimalPlaces == 0) {
                            KeyboardType.Number
                        } else {
                            KeyboardType.Decimal
                        },
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val ok = onConfirm(label, amount)
                            if (!ok && localError == null) {
                                localError = "Enter a valid amount."
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(amountFocusRequester)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val ok = onConfirm(label, amount)
                    if (!ok && localError == null) {
                        localError = "Enter a valid amount."
                    }
                }
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CustomCurrencyDialog(
    onConfirm: (CurrencyOption) -> Unit,
    onDismiss: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    var symbol by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var decimalPlaces by remember { mutableIntStateOf(2) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Currency") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = {
                        code = it.uppercase().take(6)
                        error = null
                    },
                    label = { Text("Currency Code") },
                    placeholder = { Text("e.g. CHF, SGD, BRL") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = symbol,
                    onValueChange = {
                        symbol = it.take(6)
                        error = null
                    },
                    label = { Text("Symbol") },
                    placeholder = { Text("e.g. Fr., S$, R$") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it.take(30)
                        error = null
                    },
                    label = { Text("Currency Name (optional)") },
                    placeholder = { Text("e.g. Swiss Franc") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Decimal Places (0–4)",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    (0..4).forEach { decimals ->
                        FilterChip(
                            selected = decimalPlaces == decimals,
                            onClick = { decimalPlaces = decimals },
                            label = { Text(decimals.toString()) }
                        )
                    }
                }

                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (code.isBlank() && symbol.isBlank()) {
                        error = "Enter a currency code or symbol."
                    } else {
                        val custom = CurrencyRepository.createCustomCurrency(
                            code = code.ifBlank { symbol },
                            symbol = symbol.ifBlank { code },
                            name = name,
                            decimalPlaces = decimalPlaces
                        )
                        onConfirm(custom)
                    }
                }
            ) {
                Text("Use Currency")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CurrencyChangeConfirmDialog(
    targetCurrency: CurrencyOption,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Change Currency") },
        text = {
            Text("Change currency for all current expenses to ${targetCurrency.code} (${targetCurrency.symbol})?")
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Change")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ClearAllConfirmDialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Clear All Expenses") },
        text = { Text("Remove all expenses?") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Clear")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}

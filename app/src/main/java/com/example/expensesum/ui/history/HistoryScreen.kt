package com.example.expensesum.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensesum.domain.ExpenseSumCalculator
import com.example.expensesum.domain.ExpenseSumFormatter
import com.example.expensesum.model.CurrencyOption
import com.example.expensesum.model.ExpenseSumHistory
import com.example.expensesum.model.ExpenseSumUiState
import com.example.expensesum.ui.ExpenseSumViewModel
import com.example.expensesum.util.HapticHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    uiState: ExpenseSumUiState,
    viewModel: ExpenseSumViewModel,
    onNavigateToHome: () -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    val view = LocalView.current
    val hapticsEnabled = uiState.settings.hapticFeedback
    val animationsEnabled = uiState.settings.animations

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "RECENT TOTALS",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Up to 10 saved expense lists. Tap any item to restore.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (uiState.history.isNotEmpty()) {
                OutlinedButton(
                    onClick = {
                        HapticHelper.performLightClick(view, hapticsEnabled)
                        viewModel.requestClearHistory()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Clear All")
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (uiState.history.isEmpty()) {
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
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No saved history yet",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap SAVE on the Home screen to store a completed expense list.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = uiState.history,
                    key = { it.id }
                ) { historyItem ->
                    HistoryItemCard(
                        historyItem = historyItem,
                        modifier = if (animationsEnabled) Modifier.animateItem() else Modifier,
                        onRestore = {
                            HapticHelper.performConfirm(view, hapticsEnabled)
                            viewModel.restoreHistory(historyItem)
                            onShowSnackbar("Restored \"${historyItem.title}\"")
                            onNavigateToHome()
                        },
                        onDelete = {
                            HapticHelper.performLightClick(view, hapticsEnabled)
                            viewModel.requestDeleteHistoryItem(historyItem.id)
                        }
                    )
                }
            }
        }
    }

    // Delete single history item confirmation
    val pendingDeleteId = uiState.pendingHistoryDeleteId
    if (pendingDeleteId != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteHistoryConfirmation() },
            title = { Text("Delete Saved List") },
            text = { Text("Remove this expense list from history?") },
            confirmButton = {
                Button(
                    onClick = {
                        HapticHelper.performConfirm(view, hapticsEnabled)
                        viewModel.confirmDeleteHistoryItem(pendingDeleteId)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteHistoryConfirmation() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear all history confirmation
    if (uiState.showClearHistoryConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissClearHistoryConfirmation() },
            title = { Text("Clear History") },
            text = { Text("Clear all saved expense lists?") },
            confirmButton = {
                Button(
                    onClick = {
                        HapticHelper.performConfirm(view, hapticsEnabled)
                        viewModel.confirmClearHistory()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissClearHistoryConfirmation() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun HistoryItemCard(
    historyItem: ExpenseSumHistory,
    modifier: Modifier = Modifier,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val currency = remember(historyItem) {
        CurrencyOption(
            code = historyItem.currencyCode,
            symbol = historyItem.currencySymbol,
            name = historyItem.currencyName,
            decimalPlaces = historyItem.currencyDecimalPlaces
        )
    }
    val calcResult = remember(historyItem, currency) {
        try {
            ExpenseSumCalculator.calculate(historyItem.expenses, currency)
        } catch (e: Exception) {
            null
        }
    }
    val totalFormatted = ExpenseSumFormatter.formatMoney(
        calcResult?.totalMinorUnits ?: 0L,
        currency
    )
    val countFormatted = ExpenseSumFormatter.formatCount(historyItem.expenses.size)
    val dateFormatted = remember(historyItem.calculatedAt) {
        SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.US).format(Date(historyItem.calculatedAt))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onRestore)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = historyItem.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = countFormatted,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = dateFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = totalFormatted,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.3).sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Tap to restore",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete ${historyItem.title} from history",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

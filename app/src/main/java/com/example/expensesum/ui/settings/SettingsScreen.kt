package com.example.expensesum.ui.settings

import android.os.Build
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.expensesum.domain.CurrencyRepository
import com.example.expensesum.model.ExpenseSumUiState
import com.example.expensesum.model.ThemeMode
import com.example.expensesum.ui.ExpenseSumViewModel
import com.example.expensesum.ui.home.CurrencyChangeConfirmDialog
import com.example.expensesum.ui.home.CustomCurrencyDialog
import com.example.expensesum.util.HapticHelper

@Composable
fun SettingsScreen(
    uiState: ExpenseSumUiState,
    viewModel: ExpenseSumViewModel,
    onShowSnackbar: (String) -> Unit
) {
    val view = LocalView.current
    val settings = uiState.settings
    val hapticsEnabled = settings.hapticFeedback
    var currencyMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Customize appearance, currency, and behavior.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Appearance Card
        SettingsSectionCard(title = "APPEARANCE") {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.themeMode == mode,
                        onClick = {
                            HapticHelper.performLightClick(view, hapticsEnabled)
                            viewModel.updateThemeMode(mode)
                        },
                        label = { Text(mode.displayName) }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            SettingsSwitchRow(
                title = "Dynamic Color",
                subtitle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    "Use Material You wallpaper colors"
                } else {
                    "Available on Android 12+"
                },
                checked = settings.dynamicColor,
                onCheckedChange = {
                    HapticHelper.performLightClick(view, hapticsEnabled)
                    viewModel.updateDynamicColor(it)
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            SettingsSwitchRow(
                title = "Animations",
                subtitle = "Smooth transitions for expense rows and totals",
                checked = settings.animations,
                onCheckedChange = {
                    HapticHelper.performLightClick(view, hapticsEnabled)
                    viewModel.updateAnimations(it)
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            SettingsSwitchRow(
                title = "Haptic Feedback",
                subtitle = "Subtle vibration when adding or deleting expenses",
                checked = settings.hapticFeedback,
                onCheckedChange = {
                    HapticHelper.performLightClick(view, it)
                    viewModel.updateHapticFeedback(it)
                }
            )
        }

        // Currency & Behavior Card
        SettingsSectionCard(title = "CURRENCY & BEHAVIOR") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Default Currency",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${settings.defaultCurrency.name} (${settings.defaultCurrency.code})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box {
                    OutlinedButton(
                        onClick = { currencyMenuExpanded = true },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${settings.defaultCurrency.code} (${settings.defaultCurrency.symbol})",
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select default currency"
                        )
                    }

                    DropdownMenu(
                        expanded = currencyMenuExpanded,
                        onDismissRequest = { currencyMenuExpanded = false }
                    ) {
                        CurrencyRepository.supportedCurrencies.forEach { option ->
                            DropdownMenuItem(
                                text = { Text("${option.code} (${option.symbol}) — ${option.name}") },
                                onClick = {
                                    currencyMenuExpanded = false
                                    HapticHelper.performLightClick(view, hapticsEnabled)
                                    viewModel.updateDefaultCurrency(option)
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
                                currencyMenuExpanded = false
                                HapticHelper.performLightClick(view, hapticsEnabled)
                                viewModel.openCustomCurrencyDialog()
                            }
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            SettingsSwitchRow(
                title = "Preserve Last List",
                subtitle = "Remember current title, currency, and expenses on reopen",
                checked = settings.preserveLastList,
                onCheckedChange = {
                    HapticHelper.performLightClick(view, hapticsEnabled)
                    viewModel.updatePreserveLastList(it)
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            SettingsSwitchRow(
                title = "Confirm Clear All",
                subtitle = "Ask before removing all expenses on Home screen",
                checked = settings.confirmClearAll,
                onCheckedChange = {
                    HapticHelper.performLightClick(view, hapticsEnabled)
                    viewModel.updateConfirmClearAll(it)
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            SettingsSwitchRow(
                title = "Confirm History Delete",
                subtitle = "Ask before deleting an individual saved history list",
                checked = settings.confirmHistoryDelete,
                onCheckedChange = {
                    HapticHelper.performLightClick(view, hapticsEnabled)
                    viewModel.updateConfirmHistoryDelete(it)
                }
            )
        }

        // Data & Reset Card
        SettingsSectionCard(title = "DATA & RESET") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        HapticHelper.performLightClick(view, hapticsEnabled)
                        if (uiState.history.isEmpty()) {
                            onShowSnackbar("History is already empty")
                        } else {
                            viewModel.requestClearHistory()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Clear History")
                }

                OutlinedButton(
                    onClick = {
                        HapticHelper.performLightClick(view, hapticsEnabled)
                        viewModel.requestResetSettings()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Reset Settings")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (uiState.showCustomCurrencyDialog) {
        CustomCurrencyDialog(
            onConfirm = { custom ->
                viewModel.updateDefaultCurrency(custom)
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
                        onShowSnackbar("History cleared")
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

    if (uiState.showResetSettingsConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissResetSettingsConfirmation() },
            title = { Text("Reset Settings") },
            text = { Text("Restore all preferences to their default values?") },
            confirmButton = {
                Button(
                    onClick = {
                        HapticHelper.performConfirm(view, hapticsEnabled)
                        viewModel.confirmResetSettings()
                        onShowSnackbar("Settings reset to defaults")
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissResetSettingsConfirmation() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

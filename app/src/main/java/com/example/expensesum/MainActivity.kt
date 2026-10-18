package com.example.expensesum

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.expensesum.data.SharedPreferencesStore
import com.example.expensesum.repository.ExpenseSumHistoryRepository
import com.example.expensesum.repository.SettingsRepository
import com.example.expensesum.ui.ExpenseSumApp
import com.example.expensesum.ui.ExpenseSumViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = getSharedPreferences("expense_sum_prefs", Context.MODE_PRIVATE)
        val store = SharedPreferencesStore(prefs)
        val settingsRepository = SettingsRepository(store)
        val historyRepository = ExpenseSumHistoryRepository(store)

        val viewModel = ViewModelProvider(
            this,
            ExpenseSumViewModel.Factory(settingsRepository, historyRepository)
        )[ExpenseSumViewModel::class.java]

        setContent {
            ExpenseSumApp(viewModel = viewModel)
        }
    }
}
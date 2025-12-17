package com.example.context

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.context.ui.HomeScreen
import com.example.context.ui.mockTransactions
import com.example.context.ui.ContextTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ContextTheme {
                HomeScreen(transactions = mockTransactions)
            }
        }
    }
}

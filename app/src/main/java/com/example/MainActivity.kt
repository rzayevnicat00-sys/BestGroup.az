package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.MainApp
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleNotificationIntent(intent)
        setContent {
            MainApp(viewModel = viewModel)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return
        val type = intent.getStringExtra("EXTRA_NOTIF_TYPE")
        val orderId = intent.getStringExtra("EXTRA_ORDER_ID")
        val conversationId = intent.getStringExtra("EXTRA_CONVERSATION_ID")

        if (orderId != null) {
            viewModel.handleNotificationDeepLink(type = type, orderId = orderId, conversationId = conversationId)
        } else if (conversationId != null) {
            viewModel.handleNotificationDeepLink(type = type, orderId = null, conversationId = conversationId)
        }
    }
}


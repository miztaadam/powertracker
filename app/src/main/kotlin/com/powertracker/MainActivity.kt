package com.powertracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.powertracker.ui.AppShell
import com.powertracker.ui.theme.PowertrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PowertrackerTheme {
                AppShell((application as PowertrackerApp).container)
            }
        }
    }
}

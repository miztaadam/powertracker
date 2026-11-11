package com.powertracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.powertracker.ui.AppShell
import com.powertracker.ui.theme.PowertrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Edge to edge is enforced on current Android; this also keeps status-bar icons readable in both themes.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            PowertrackerTheme {
                AppShell((application as PowertrackerApp).container)
            }
        }
    }
}

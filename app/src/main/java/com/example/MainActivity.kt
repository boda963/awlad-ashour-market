package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AddStockScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.InvoicesScreen
import com.example.ui.screens.PosScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MarketViewModel
import com.example.ui.viewmodel.Screen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        MarketApp()
                    }
                }
            }
        }
    }
}

@Composable
fun MarketApp(
    viewModel: MarketViewModel = viewModel()
) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()

    Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
        when (screen) {
            Screen.SPLASH -> SplashScreen(
                onSplashFinished = {
                    viewModel.navigateTo(Screen.HOME)
                }
            )
            Screen.HOME -> HomeScreen(viewModel = viewModel)
            Screen.POS -> PosScreen(viewModel = viewModel)
            Screen.ADD_STOCK -> AddStockScreen(viewModel = viewModel)
            Screen.INVENTORY -> InventoryScreen(viewModel = viewModel)
            Screen.REPORTS -> ReportsScreen(viewModel = viewModel)
            Screen.INVOICES -> InvoicesScreen(viewModel = viewModel)
            Screen.SETTINGS -> SettingsScreen(viewModel = viewModel)
        }
    }
}

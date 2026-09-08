package com.decli.codehelper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.decli.codehelper.model.ThemeMode
import com.decli.codehelper.ui.CodeHelperApp
import com.decli.codehelper.ui.home.HomeViewModel
import com.decli.codehelper.ui.theme.CodeHelperTheme

class MainActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val displaySettings by viewModel.displaySettings.collectAsStateWithLifecycle()
            val darkTheme = when (displaySettings.themeMode) {
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
                ThemeMode.System -> isSystemInDarkTheme()
            }

            // 状态栏 / 导航栏图标颜色跟随应用内的外观设置，而不是只跟随系统
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }

            CodeHelperTheme(
                darkTheme = darkTheme,
                codeScale = displaySettings.codeScale,
            ) {
                CodeHelperApp(viewModel = viewModel)
            }
        }
    }
}

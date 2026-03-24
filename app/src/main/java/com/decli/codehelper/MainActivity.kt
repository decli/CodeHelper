package com.decli.codehelper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.decli.codehelper.ui.CodeHelperApp
import com.decli.codehelper.ui.home.HomeViewModel
import com.decli.codehelper.ui.theme.CodeHelperTheme

class MainActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CodeHelperTheme {
                CodeHelperApp(viewModel = viewModel)
            }
        }
    }
}


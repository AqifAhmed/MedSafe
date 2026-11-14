package com.medsafe.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.medsafe.app.data.settings.ThemeMode
import com.medsafe.app.ui.MainViewModel
import com.medsafe.app.ui.navigation.MedSafeApp
import com.medsafe.app.ui.theme.MedSafeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            MedSafeTheme(themeMode = settings?.themeMode ?: ThemeMode.SYSTEM) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    if (settings == null) {
                        Box(Modifier.fillMaxSize())
                    } else {
                        val navController = rememberNavController()
                        MedSafeApp(
                            navController = navController,
                            startOnboarding = !settings!!.onboardingComplete,
                        )
                    }
                }
            }
        }
    }
}

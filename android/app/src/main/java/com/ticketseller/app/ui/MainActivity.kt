package com.ticketseller.app.ui

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.ticketseller.app.ui.theme.AppTheme
import com.ticketseller.app.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val updateLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge-to-edge so Compose gets IME (keyboard) + system-bar insets on Android 15+.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        maybeCheckForUpdate()
        setContent {
            val themeVm: ThemeViewModel = hiltViewModel()
            val c = themeVm.controller
            val dark = c.darkOverride ?: isSystemInDarkTheme()

            // Keep status/nav bar icons readable against the current theme background.
            val view = LocalView.current
            LaunchedEffect(dark) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
            }

            AppTheme(themeKey = c.themeKey, dark = dark) {
                AppNav()
            }
        }
    }

    private fun maybeCheckForUpdate() {
        val manager = AppUpdateManagerFactory.create(this)
        manager.appUpdateInfo.addOnSuccessListener { info ->
            val canFlexible = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && canFlexible) {
                val listener = InstallStateUpdatedListener { state ->
                    if (state.installStatus() == InstallStatus.DOWNLOADED) {
                        Toast.makeText(this, "Update downloaded — restarting to finish.", Toast.LENGTH_LONG).show()
                        manager.completeUpdate()
                    }
                }
                manager.registerListener(listener)
                runCatching {
                    manager.startUpdateFlowForResult(
                        info,
                        updateLauncher,
                        AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE),
                    )
                }
            }
        }
    }
}

package com.ticketseller.app.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
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

    // Launcher for the Play in-app update flow (no-op result handling — Play drives the UI).
    private val updateLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        maybeCheckForUpdate()
        setContent {
            val themeVm: ThemeViewModel = hiltViewModel()
            val c = themeVm.controller
            val dark = c.darkOverride ?: isSystemInDarkTheme()
            AppTheme(themeKey = c.themeKey, dark = dark) {
                AppNav()
            }
        }
    }

    /**
     * Play In-App Update. When the app is installed from Play and a newer version exists,
     * this shows Google's flexible-update prompt. On a sideloaded/debug build it's a no-op.
     */
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

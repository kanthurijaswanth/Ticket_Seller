package com.ticketseller.app.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    vm: AuthViewModel = hiltViewModel(),
    onSignedIn: () -> Unit,
    onGoSignup: () -> Unit,
) {
    val ui by vm.ui.collectAsState()
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text("Welcome back") }) }) { pad ->
        Column(
            Modifier.padding(pad).padding(24.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Resell last-minute tickets you can't use, or grab one at a discount.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = pass, onValueChange = { pass = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { vm.signIn(email, pass, onSignedIn) },
                enabled = !ui.loading,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (ui.loading) "Signing in…" else "Sign in") }

            ui.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onGoSignup, modifier = Modifier.fillMaxWidth()) {
                Text("New here? Create an account", textAlign = TextAlign.Center)
            }

            // ────────────────────────────────────────────────
            // DEV ONLY — delete this whole block before release.
            // ────────────────────────────────────────────────
            Divider()
            Button(
                onClick = { vm.devBypassSignIn(onSignedIn) },
                enabled = !ui.loading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("DEV: Skip sign-in") }
            Text(
                "Development shortcut — remove before release.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

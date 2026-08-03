package com.ticketseller.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ticketseller.app.ui.common.AppBackground
import com.ticketseller.app.ui.common.GradientButton
import com.ticketseller.app.ui.theme.LocalGlass

@Composable
fun LoginScreen(
    vm: AuthViewModel = hiltViewModel(),
    onSignedIn: () -> Unit,
    onGoSignup: () -> Unit,
) {
    val g = LocalGlass.current
    val ui by vm.ui.collectAsState()
    var signUp by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }

    AppBackground {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(top = 24.dp, bottom = 28.dp),
        ) {
            // Brand hero
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(56.dp).clip(RoundedCornerShape(19.dp)).background(Brush.linearGradient(listOf(g.brand, g.deep))),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.ConfirmationNumber, null, tint = g.onBrandSurface, modifier = Modifier.size(28.dp)) }
                Spacer(Modifier.width(13.dp))
                Column {
                    Text("TicketResell", style = MaterialTheme.typography.headlineMedium, color = g.ink, fontWeight = FontWeight.ExtraBold)
                    Text("SOMEONE ELSE'S SEAT, YOUR TRIP", style = MaterialTheme.typography.labelSmall, color = g.ink3)
                }
            }

            Spacer(Modifier.height(30.dp))
            Text(if (signUp) "Let's get you set up." else "Welcome back.", style = MaterialTheme.typography.displaySmall, color = g.ink, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(10.dp))
            Text(
                if (signUp) "Takes a minute. You can buy and sell from the same account." else "Sign in to see tickets leaving near you tonight.",
                style = MaterialTheme.typography.bodyMedium, color = g.ink2,
            )

            // Tabs
            Spacer(Modifier.height(22.dp))
            Row(
                Modifier.clip(RoundedCornerShape(18.dp)).background(g.glass).border(1.dp, g.glassBorder, RoundedCornerShape(18.dp)).padding(5.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AuthTab("I have an account", !signUp, Modifier.weight(1f)) { signUp = false; vm.clearError() }
                AuthTab("I'm new here", signUp, Modifier.weight(1f)) { signUp = true; vm.clearError() }
            }

            // Fields
            Spacer(Modifier.height(16.dp))
            if (signUp) {
                GlassField("YOUR NAME", name, { name = it }, "Aditya Sharma")
                Spacer(Modifier.height(11.dp))
            }
            GlassField("EMAIL", email, { email = it }, "you@email.com", keyboard = KeyboardType.Email)
            Spacer(Modifier.height(11.dp))
            GlassField(
                "PASSWORD", pass, { pass = it }, "••••••••",
                keyboard = KeyboardType.Password,
                visual = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                trailing = { Text(if (showPass) "Hide" else "Show", style = MaterialTheme.typography.labelMedium, color = g.accent, modifier = Modifier.clickable { showPass = !showPass }) },
            )

            if (!signUp) {
                Spacer(Modifier.height(12.dp))
                Text("I forgot my password", style = MaterialTheme.typography.labelMedium, color = g.accent, modifier = Modifier.align(Alignment.End).clickable { vm.sendPasswordReset(email) })
            } else {
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.clip(RoundedCornerShape(18.dp)).background(g.tint).border(1.dp, g.glassBorder, RoundedCornerShape(18.dp)).padding(13.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(Modifier.size(20.dp).clip(RoundedCornerShape(7.dp)).background(g.brand), contentAlignment = Alignment.Center) {
                        Text("✓", color = g.onBrand, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("One account does both — buy tickets and sell the ones you can't use. No separate seller sign-up.", style = MaterialTheme.typography.bodySmall, color = g.ink2)
                }
            }

            Spacer(Modifier.height(20.dp))
            GradientButton(
                text = if (ui.loading) "Please wait…" else if (signUp) "Create my account" else "Sign in",
                enabled = !ui.loading,
                height = 56,
            ) { if (signUp) vm.signUp(email, pass, name, onSignedIn) else vm.signIn(email, pass, onSignedIn) }

            ui.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            ui.info?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = g.accent, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(22.dp))
            // DEV ONLY — remove before release.
            Box(
                Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(16.dp)).background(g.glass)
                    .border(1.dp, g.glassBorder, RoundedCornerShape(16.dp)).clickable(enabled = !ui.loading) { vm.devBypassSignIn(onSignedIn) }
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            ) { Text("DEV: Skip sign-in", style = MaterialTheme.typography.labelLarge, color = g.ink2) }

            Spacer(Modifier.height(18.dp))
            Text(
                "By continuing you agree that tickets are sold person-to-person and payment happens directly over UPI.",
                style = MaterialTheme.typography.labelMedium, color = g.ink3, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AuthTab(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val g = LocalGlass.current
    Box(
        modifier.clip(RoundedCornerShape(14.dp)).background(if (selected) g.ink else androidx.compose.ui.graphics.Color.Transparent).clickable { onClick() }.padding(vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) g.onInk else g.ink2) }
}

@Composable
private fun GlassField(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    placeholder: String,
    keyboard: KeyboardType = KeyboardType.Text,
    visual: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
) {
    val g = LocalGlass.current
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(19.dp)).background(g.glass).border(1.dp, g.glassBorder, RoundedCornerShape(19.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = g.ink3)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.titleMedium, color = g.ink3)
                BasicTextField(
                    value = value, onValueChange = onValue, singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(color = g.ink, fontWeight = FontWeight.Bold),
                    cursorBrush = SolidColor(g.deep),
                    visualTransformation = visual,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboard),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (trailing != null) { Spacer(Modifier.width(8.dp)); trailing() }
        }
    }
}

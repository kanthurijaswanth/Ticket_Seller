package com.ticketseller.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ticketseller.app.data.repo.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUi(
    val loading: Boolean = false,
    val error: String? = null,
    val info: String? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    /** True whenever there's any signed-in identity — Supabase user OR dev bypass. */
    val isSignedIn: StateFlow<Boolean> = authRepository.effectiveUidFlow()
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, authRepository.effectiveUid() != null)

    private val _ui = MutableStateFlow(AuthUi())
    val ui: StateFlow<AuthUi> = _ui

    fun signIn(email: String, password: String, onDone: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _ui.value = _ui.value.copy(error = "Enter email & password"); return
        }
        viewModelScope.launch {
            _ui.value = AuthUi(loading = true)
            runCatching { authRepository.signInEmail(email.trim(), password) }
                .onSuccess { _ui.value = AuthUi(); onDone() }
                .onFailure { _ui.value = AuthUi(error = friendlyError(it)) }
        }
    }

    fun signUp(email: String, password: String, name: String, onDone: () -> Unit) {
        if (email.isBlank() || password.length < 6 || name.isBlank()) {
            _ui.value = _ui.value.copy(error = "Name, email and 6+ char password required"); return
        }
        viewModelScope.launch {
            _ui.value = AuthUi(loading = true)
            runCatching { authRepository.signUpEmail(email.trim(), password, name.trim()) }
                .onSuccess { _ui.value = AuthUi(); onDone() }
                .onFailure { _ui.value = AuthUi(error = friendlyError(it)) }
        }
    }

    /**
     * Translates raw Supabase / SMTP errors into clean, end-user-friendly text.
     * Catches the most common signup failures: email already used, invalid email,
     * weak password, and — importantly — SMTP / email-sending quota exhaustion.
     */
    private fun friendlyError(e: Throwable): String {
        val raw = (e.message ?: "").lowercase()
        return when {
            // Email server (Gmail / Supabase built-in) ran out of daily quota
            "error sending" in raw && "email" in raw ->
                "Today's email server quota is full. Please try creating an account tomorrow."
            "rate limit" in raw ->
                "Today's email server quota is full. Please try creating an account tomorrow."
            "smtp" in raw ->
                "Today's email server quota is full. Please try creating an account tomorrow."
            "over_email_send_rate_limit" in raw ->
                "Today's email server quota is full. Please try creating an account tomorrow."

            // Account problems
            "user already registered" in raw || "already exists" in raw ->
                "An account with this email already exists. Try signing in instead."
            "invalid login credentials" in raw || "invalid_credentials" in raw ->
                "Wrong email or password."
            "confirm_email" in raw ->
                "Account created! Check your inbox for a confirmation link, then sign in."
            "email not confirmed" in raw ->
                "Please check your inbox and click the confirmation link before signing in."
            "invalid email" in raw || "invalid format" in raw ->
                "That doesn't look like a valid email address."
            "password should be at least" in raw || "weak password" in raw ->
                "Password is too short — use at least 6 characters."

            // Network
            "unable to resolve host" in raw || "failed to connect" in raw || "timeout" in raw ->
                "Can't reach the server. Check your internet and try again."

            // Fallback
            raw.isBlank() -> "Something went wrong. Please try again."
            else -> e.message ?: "Something went wrong."
        }
    }

    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _ui.value = _ui.value.copy(error = "Enter your email above first, then tap Forgot password.")
            return
        }
        viewModelScope.launch {
            _ui.value = AuthUi(loading = true)
            runCatching { authRepository.sendPasswordReset(email.trim()) }
                .onSuccess { _ui.value = AuthUi(info = "If that email has an account, a reset link is on its way.") }
                .onFailure { _ui.value = AuthUi(error = friendlyError(it)) }
        }
    }

    /** DEV ONLY — no Supabase auth required. */
    fun devBypassSignIn(onDone: () -> Unit) {
        viewModelScope.launch {
            _ui.value = AuthUi(loading = true)
            runCatching { authRepository.devBypassSignIn() }
                .onSuccess { _ui.value = AuthUi(); onDone() }
                .onFailure { _ui.value = AuthUi(error = "Dev bypass failed: ${it.message ?: "unknown"}") }
        }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }

    fun clearError() { _ui.value = _ui.value.copy(error = null) }
}

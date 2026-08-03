package com.ticketseller.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ticketseller.app.data.repo.AuthRepository
import com.ticketseller.app.data.repo.UserRepository
import com.ticketseller.app.ui.common.AppBackground
import com.ticketseller.app.ui.common.GlassIconButton
import com.ticketseller.app.ui.common.GradientButton
import com.ticketseller.app.ui.common.glassBox
import com.ticketseller.app.ui.theme.LocalGlass
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
) : ViewModel() {
    private val _name = MutableStateFlow("")
    private val _phone = MutableStateFlow("")
    private val _upiId = MutableStateFlow("")
    private val _saving = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _done = MutableStateFlow(false)

    val name: StateFlow<String> = _name
    val phone: StateFlow<String> = _phone
    val upiId: StateFlow<String> = _upiId
    val saving: StateFlow<Boolean> = _saving
    val error: StateFlow<String?> = _error
    val done: StateFlow<Boolean> = _done

    init {
        val uid = authRepository.effectiveUid()
        if (!uid.isNullOrBlank()) viewModelScope.launch {
            userRepository.get(uid)?.let {
                _name.value = it.name
                _phone.value = it.phone
                _upiId.value = it.upiId
            }
        }
    }

    fun setName(v: String) { _name.value = v }
    fun setPhone(v: String) { _phone.value = v.filter { c -> c.isDigit() || c == '+' } }
    fun setUpiId(v: String) { _upiId.value = v.trim().filterNot { it.isWhitespace() } }

    fun save() {
        val uid = authRepository.effectiveUid() ?: return
        if (_phone.value.length !in 7..15) { _error.value = "Enter a valid phone number"; return }
        val upi = _upiId.value.trim()
        if (upi.isNotBlank() && !upi.matches(Regex("^[a-zA-Z0-9.\\-_]{2,}@[a-zA-Z]{2,}$"))) {
            _error.value = "That doesn't look like a UPI ID (example: yourname@okhdfcbank)"
            return
        }
        viewModelScope.launch {
            _saving.value = true
            runCatching {
                userRepository.update(uid, mapOf("name" to _name.value.trim(), "phone" to _phone.value.trim(), "upi_id" to upi))
            }.onSuccess { _done.value = true }.onFailure { _error.value = it.message ?: "Save failed" }
            _saving.value = false
        }
    }
}

@Composable
fun EditProfileScreen(
    vm: EditProfileViewModel = hiltViewModel(),
    onDone: () -> Unit,
) {
    val g = LocalGlass.current
    val name by vm.name.collectAsState()
    val phone by vm.phone.collectAsState()
    val upiId by vm.upiId.collectAsState()
    val saving by vm.saving.collectAsState()
    val err by vm.error.collectAsState()
    val done by vm.done.collectAsState()

    LaunchedEffect(done) { if (done) onDone() }

    AppBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onDone)
                Spacer(Modifier.width(12.dp))
                Text("Edit profile", style = MaterialTheme.typography.titleMedium, color = g.ink, fontWeight = FontWeight.ExtraBold)
            }

            Spacer(Modifier.height(20.dp))
            Field("YOUR NAME", name, vm::setName, "Your name")
            Spacer(Modifier.height(11.dp))
            Field("PHONE (shared only with consent in chat)", phone, vm::setPhone, "98765 43210", KeyboardType.Phone)

            Spacer(Modifier.height(20.dp))
            Text("Payments (for sellers)", style = MaterialTheme.typography.titleSmall, color = g.accent, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(6.dp))
            Text("Add your UPI ID so buyers can pay you directly when they buy your ticket. Leave blank if you only buy.", style = MaterialTheme.typography.bodySmall, color = g.ink3)
            Spacer(Modifier.height(11.dp))
            Field("UPI ID", upiId, vm::setUpiId, "yourname@okhdfcbank", KeyboardType.Email)

            err?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(22.dp))
            GradientButton(if (saving) "Saving…" else "Save", enabled = !saving, onClick = vm::save)
        }
    }
}

@Composable
private fun Field(label: String, value: String, onValue: (String) -> Unit, placeholder: String, keyboard: KeyboardType = KeyboardType.Text) {
    val g = LocalGlass.current
    Column(Modifier.fillMaxWidth().glassBox(RoundedCornerShape(19.dp)).padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = g.ink3)
        Spacer(Modifier.height(6.dp))
        Box {
            if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.titleMedium, color = g.ink3)
            BasicTextField(
                value = value, onValueChange = onValue, singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(color = g.ink, fontWeight = FontWeight.Bold),
                cursorBrush = SolidColor(g.deep),
                keyboardOptions = KeyboardOptions(keyboardType = keyboard),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

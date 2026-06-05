package com.ticketseller.app.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ticketseller.app.data.repo.AuthRepository
import com.ticketseller.app.data.repo.UserRepository
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
    private val _saving = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _done = MutableStateFlow(false)

    val name: StateFlow<String> = _name
    val phone: StateFlow<String> = _phone
    val saving: StateFlow<Boolean> = _saving
    val error: StateFlow<String?> = _error
    val done: StateFlow<Boolean> = _done

    init {
        val uid = authRepository.effectiveUid()
        if (!uid.isNullOrBlank()) viewModelScope.launch {
            userRepository.get(uid)?.let {
                _name.value = it.name
                _phone.value = it.phone
            }
        }
    }

    fun setName(v: String) { _name.value = v }
    fun setPhone(v: String) { _phone.value = v.filter { c -> c.isDigit() || c == '+' } }

    fun save() {
        val uid = authRepository.effectiveUid() ?: return
        if (_phone.value.length !in 7..15) { _error.value = "Enter a valid phone number"; return }
        viewModelScope.launch {
            _saving.value = true
            runCatching {
                userRepository.update(uid, mapOf("name" to _name.value.trim(), "phone" to _phone.value.trim()))
            }
                .onSuccess { _done.value = true }
                .onFailure { _error.value = it.message ?: "Save failed" }
            _saving.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    vm: EditProfileViewModel = hiltViewModel(),
    onDone: () -> Unit,
) {
    val name by vm.name.collectAsState()
    val phone by vm.phone.collectAsState()
    val saving by vm.saving.collectAsState()
    val err by vm.error.collectAsState()
    val done by vm.done.collectAsState()

    LaunchedEffect(done) { if (done) onDone() }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Edit profile") },
            navigationIcon = {
                IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            },
        )
    }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = name, onValueChange = vm::setName,
                label = { Text("Name") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = phone, onValueChange = vm::setPhone,
                label = { Text("Phone (shared only with consent in chat)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
            )
            err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = vm::save, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
                Text(if (saving) "Saving…" else "Save")
            }
        }
    }
}

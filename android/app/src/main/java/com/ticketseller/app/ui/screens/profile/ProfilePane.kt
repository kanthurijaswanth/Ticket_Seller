package com.ticketseller.app.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ticketseller.app.data.model.User
import com.ticketseller.app.data.repo.AuthRepository
import com.ticketseller.app.data.repo.UserRepository
import com.ticketseller.app.data.seed.DemoSeed
import com.ticketseller.app.ui.common.LoadingBox
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    userRepository: UserRepository,
    private val demoSeed: DemoSeed,
) : ViewModel() {
    private val uid = MutableStateFlow(authRepository.effectiveUid().orEmpty())
    val me: StateFlow<User?> = uid.flatMapLatest {
        if (it.isBlank()) flowOf(null) else userRepository.observe(it)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _seedState = MutableStateFlow<SeedState>(SeedState.Idle)
    val seedState: StateFlow<SeedState> = _seedState

    sealed interface SeedState {
        data object Idle : SeedState
        data object Loading : SeedState
        data object Done : SeedState
        data class Error(val msg: String) : SeedState
    }

    fun seedDemoTickets() {
        val u = me.value
        val id = authRepository.effectiveUid()
        if (id.isNullOrBlank()) {
            _seedState.value = SeedState.Error("Not signed in")
            return
        }
        viewModelScope.launch {
            _seedState.value = SeedState.Loading
            runCatching { demoSeed.seed(id, u?.name.orEmpty()) }
                .onSuccess { _seedState.value = SeedState.Done }
                .onFailure { _seedState.value = SeedState.Error(it.message ?: "Failed") }
        }
    }
}

@Composable
fun ProfilePane(
    vm: ProfileViewModel = hiltViewModel(),
    onEditProfile: () -> Unit,
    onSignOut: () -> Unit,
) {
    val me by vm.me.collectAsState()
    val seedState by vm.seedState.collectAsState()
    if (me == null) LoadingBox()
    else Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Profile header card
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier.size(64.dp),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            me!!.name.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(me!!.name.ifBlank { "Add your name" }, style = MaterialTheme.typography.titleMedium)
                    Text(
                        me!!.email.ifBlank { "—" },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Star, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${"%.1f".format(me!!.ratingAvg)} (${me!!.ratingCount} ratings)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onEditProfile) { Icon(Icons.Filled.Edit, null) }
            }
        }

        // Phone card
        Card(modifier = Modifier.fillMaxWidth()) {
            ListItem(
                headlineContent = { Text("Phone number") },
                supportingContent = {
                    Text(
                        me!!.phone.ifBlank { "Add a number — shared only after consent in chat" },
                        color = if (me!!.phone.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    )
                },
            )
        }

        // ────────────────────────────────────────────────
        // DEV ONLY — remove before release.
        // ────────────────────────────────────────────────
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Developer tools",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Text(
                    "Seed 10 realistic demo tickets (Mumbai → Pune, Bangalore → Chennai, …) so you can explore Browse.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Button(
                    onClick = vm::seedDemoTickets,
                    enabled = seedState !is ProfileViewModel.SeedState.Loading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        when (seedState) {
                            ProfileViewModel.SeedState.Loading -> "Seeding…"
                            ProfileViewModel.SeedState.Done -> "Seeded ✓ — tap again to add more"
                            is ProfileViewModel.SeedState.Error -> "Retry"
                            ProfileViewModel.SeedState.Idle -> "Seed demo tickets"
                        }
                    )
                }
                (seedState as? ProfileViewModel.SeedState.Error)?.let {
                    Text(it.msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Logout, null)
            Spacer(Modifier.width(8.dp))
            Text("Sign out")
        }
    }
}

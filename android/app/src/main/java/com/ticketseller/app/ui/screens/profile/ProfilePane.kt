package com.ticketseller.app.ui.screens.profile

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ticketseller.app.data.model.User
import com.ticketseller.app.data.repo.AuthRepository
import com.ticketseller.app.data.repo.UserRepository
import com.ticketseller.app.data.seed.DemoSeed
import com.ticketseller.app.ui.common.GlassCard
import com.ticketseller.app.ui.common.GradientButton
import com.ticketseller.app.ui.common.LoadingBox
import com.ticketseller.app.ui.theme.LocalGlass
import com.ticketseller.app.util.PlayHelpers
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
    val me: StateFlow<User?> = uid.flatMapLatest { id ->
        if (id.isBlank()) flowOf(User()) else userRepository.observe(id).map { it ?: User(uid = id) }
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
        if (id.isNullOrBlank()) { _seedState.value = SeedState.Error("Not signed in"); return }
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
    onOpenTheme: () -> Unit = {},
) {
    val g = LocalGlass.current
    val me by vm.me.collectAsState()
    val seedState by vm.seedState.collectAsState()
    val ctx = LocalContext.current
    val u = me

    if (u == null) {
        LoadingBox()
        return
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("You", style = MaterialTheme.typography.headlineSmall, color = g.ink, fontWeight = FontWeight.ExtraBold) }

        // Header card
        item {
            GlassCard(strong = true) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(g.brand, g.deep))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(u.name.firstOrNull()?.uppercase() ?: "🙂", style = MaterialTheme.typography.headlineMedium, color = g.onBrandStrong, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(u.name.ifBlank { "Add your name" }, style = MaterialTheme.typography.titleLarge, color = g.ink, fontWeight = FontWeight.ExtraBold)
                        Text(u.email.ifBlank { "—" }, style = MaterialTheme.typography.bodySmall, color = g.ink3)
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Star, null, Modifier.size(14.dp), tint = g.accent)
                            Spacer(Modifier.width(4.dp))
                            Text("${"%.1f".format(u.ratingAvg)} · ${u.ratingCount} ratings", style = MaterialTheme.typography.labelMedium, color = g.ink3)
                        }
                    }
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(g.tint).clickable { onEditProfile() },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.Edit, "Edit", tint = g.tintInk, modifier = Modifier.size(18.dp)) }
                }
            }
        }

        item { SectionLabel("Your details") }
        item { ProfileRow(Icons.Filled.Phone, "Phone number", u.phone.ifBlank { "Add a number — shared only after consent in chat" }, onClick = onEditProfile) }
        item { ProfileRow(Icons.Filled.AccountBalanceWallet, "UPI ID (to receive payments)", u.upiId.ifBlank { "Add your UPI ID so buyers can pay you" }, onClick = onEditProfile) }

        item { SectionLabel("Settings") }
        item { ProfileRow(Icons.Filled.Palette, "Colours & theme", "6 themes, light or dark", onClick = onOpenTheme) }
        item { ProfileRow(Icons.Filled.StarRate, "Rate us on the Play Store", "Enjoying TicketResell? Leave a rating") { (ctx as? Activity)?.let { PlayHelpers.requestReview(it) } } }

        // Dev tools (remove before release)
        item {
            GlassCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = g.accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Developer tools", style = MaterialTheme.typography.titleSmall, color = g.ink, fontWeight = FontWeight.ExtraBold)
                    }
                    Text("Seed 10 realistic demo tickets so you can explore.", style = MaterialTheme.typography.bodySmall, color = g.ink3)
                    GradientButton(
                        text = when (seedState) {
                            ProfileViewModel.SeedState.Loading -> "Seeding…"
                            ProfileViewModel.SeedState.Done -> "Seeded ✓ — tap for more"
                            is ProfileViewModel.SeedState.Error -> "Retry"
                            ProfileViewModel.SeedState.Idle -> "Seed demo tickets"
                        },
                        enabled = seedState !is ProfileViewModel.SeedState.Loading,
                        height = 48,
                        onClick = vm::seedDemoTickets,
                    )
                    (seedState as? ProfileViewModel.SeedState.Error)?.let {
                        Text(it.msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(g.glass).border(1.dp, g.glassBorder, RoundedCornerShape(18.dp)).clickable { onSignOut() }.padding(vertical = 15.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null, tint = g.ink2, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sign out", style = MaterialTheme.typography.labelLarge, color = g.ink2)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val g = LocalGlass.current
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = g.ink3, modifier = Modifier.padding(top = 6.dp, start = 4.dp))
}

@Composable
private fun ProfileRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    val g = LocalGlass.current
    GlassCard(onClick = onClick) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(g.tint), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = g.tintInk, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = g.ink, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = g.ink3)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = g.ink3, modifier = Modifier.size(16.dp))
        }
    }
}

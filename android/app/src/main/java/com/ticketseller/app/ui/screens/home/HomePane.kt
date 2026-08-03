package com.ticketseller.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ticketseller.app.data.model.Listing
import com.ticketseller.app.data.model.TicketType
import com.ticketseller.app.data.repo.AuthRepository
import com.ticketseller.app.data.repo.ListingRepository
import com.ticketseller.app.data.repo.UserRepository
import com.ticketseller.app.ui.common.GlassCard
import com.ticketseller.app.ui.common.TintChip
import com.ticketseller.app.ui.common.glassBox
import com.ticketseller.app.ui.theme.LocalGlass
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class HomeViewModel @Inject constructor(
    authRepository: AuthRepository,
    userRepository: UserRepository,
    listingRepository: ListingRepository,
) : ViewModel() {
    val listings: StateFlow<List<Listing>?> =
        listingRepository.observeActive().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name

    init {
        val uid = authRepository.effectiveUid()
        if (!uid.isNullOrBlank()) viewModelScope.launch {
            _name.value = userRepository.get(uid)?.name.orEmpty()
        }
    }
}

@Composable
fun HomePane(
    vm: HomeViewModel = hiltViewModel(),
    onListing: (String) -> Unit,
    onSell: () -> Unit,
    onNotifications: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
) {
    val g = LocalGlass.current
    val all by vm.listings.collectAsState()
    val name by vm.name.collectAsState()
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf<TicketType?>(null) }

    val filtered = remember(all, query, filter) {
        (all ?: emptyList()).filter { l ->
            (filter == null || l.ticket.type == filter) &&
                (query.isBlank() || listOf(
                    l.ticket.source, l.ticket.destination, l.ticket.operator,
                    l.address.pickupCity, l.address.dropCity,
                ).any { it.contains(query, ignoreCase = true) })
        }.sortedBy { it.ticket.journeyDateMillis }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            // Greeting row — tap the avatar/name to open your profile
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).clickable { onOpenProfile() }.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(15.dp))
                            .background(Brush.linearGradient(listOf(g.brand, g.deep))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            name.firstOrNull()?.uppercase() ?: "🙂",
                            style = MaterialTheme.typography.titleMedium, color = g.onBrandStrong, fontWeight = FontWeight.ExtraBold,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Namaste", style = MaterialTheme.typography.bodySmall, color = g.ink3)
                        Text(name.ifBlank { "Traveller" }, style = MaterialTheme.typography.titleLarge, color = g.ink, fontWeight = FontWeight.ExtraBold)
                    }
                }
                com.ticketseller.app.ui.common.GlassIconButton(
                    icon = Icons.Filled.Notifications,
                    contentDescription = "Notifications",
                    onClick = onNotifications,
                )
            }
        }

        item {
            // Search
            Row(
                Modifier.fillMaxWidth().glassBox(RoundedCornerShape(20.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Search, null, tint = g.ink3, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(10.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = g.ink, fontWeight = FontWeight.SemiBold),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(g.deep),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (query.isEmpty()) Text("Which city are you going to?", style = MaterialTheme.typography.bodyLarge, color = g.ink3, fontWeight = FontWeight.SemiBold)
                        inner()
                    },
                )
                if (query.isNotEmpty()) {
                    Icon(Icons.Filled.Close, "Clear", tint = g.ink3, modifier = Modifier.size(18.dp).clickable { query = "" })
                }
            }
        }

        item {
            // Buy / Sell tiles
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionTile(
                    title = "Buy a\nticket", subtitle = "${(all?.size ?: 0)} leaving soon",
                    gradient = listOf(g.deep, g.deepDark), fg = Color.White, subFg = Color.White.copy(alpha = 0.75f),
                    icon = Icons.AutoMirrored.Filled.ArrowForward, iconBg = Color.White.copy(alpha = 0.22f), iconTint = Color.White,
                    modifier = Modifier.weight(1f),
                ) { /* stays on home list */ }
                ActionTile(
                    title = "Sell my\nticket", subtitle = "Photo does the typing",
                    gradient = listOf(g.brandLight, g.brand), fg = g.onBrand, subFg = g.onBrand.copy(alpha = 0.62f),
                    icon = Icons.Filled.Add, iconBg = Color.White.copy(alpha = 0.45f), iconTint = g.onBrand,
                    modifier = Modifier.weight(1f), onClick = onSell,
                )
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Text("Leaving soon near you", style = MaterialTheme.typography.titleMedium, color = g.ink, fontWeight = FontWeight.ExtraBold)
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterPill("All", filter == null) { filter = null }
                FilterPill("Bus", filter == TicketType.BUS) { filter = TicketType.BUS }
                FilterPill("Train", filter == TicketType.TRAIN) { filter = TicketType.TRAIN }
            }
        }

        if (all == null) {
            item { Text("Loading…", color = g.ink3, modifier = Modifier.padding(24.dp)) }
        } else if (filtered.isEmpty()) {
            item { EmptyHome() }
        } else {
            items(filtered, key = { it.id }) { HomeListingCard(it) { onListing(it.id) } }
        }
    }
}

@Composable
private fun ActionTile(
    title: String, subtitle: String, gradient: List<Color>, fg: Color, subFg: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector, iconBg: Color, iconTint: Color,
    modifier: Modifier = Modifier, onClick: () -> Unit,
) {
    Column(
        modifier.clip(RoundedCornerShape(22.dp)).background(Brush.linearGradient(gradient)).clickable { onClick() }
            .padding(15.dp).height(130.dp),
    ) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(12.dp)).background(iconBg), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.weight(1f))
        Text(title, style = MaterialTheme.typography.titleMedium, color = fg, fontWeight = FontWeight.ExtraBold, lineHeight = androidx.compose.ui.unit.TextUnit(18f, androidx.compose.ui.unit.TextUnitType.Sp))
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.labelMedium, color = subFg, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val g = LocalGlass.current
    val bg = if (selected) g.ink else g.glass
    val fg = if (selected) g.onInk else g.ink2
    Box(
        Modifier.clip(RoundedCornerShape(20.dp)).background(bg).border(1.dp, g.glassBorder, RoundedCornerShape(20.dp))
            .clickable { onClick() }.padding(horizontal = 15.dp, vertical = 8.dp),
    ) { Text(label, color = fg, style = MaterialTheme.typography.labelLarge) }
}

@Composable
private fun HomeListingCard(l: Listing, onClick: () -> Unit) {
    val g = LocalGlass.current
    val stripe = if (l.ticket.type == TicketType.BUS) g.brand else g.deep
    GlassCard(onClick = onClick, shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TintChip(if (l.ticket.type == TicketType.BUS) "Bus" else "Train")
                Spacer(Modifier.width(8.dp))
                Text(
                    l.ticket.operator.ifBlank { "Operator" }, style = MaterialTheme.typography.labelMedium, color = g.ink3,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                TintChip("Checked", leading = Icons.Filled.Check)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(l.ticket.source.ifBlank { "—" }, style = MaterialTheme.typography.titleLarge, color = g.ink, fontWeight = FontWeight.ExtraBold)
                    Text(l.address.pickupCity.ifBlank { l.ticket.source }, style = MaterialTheme.typography.labelMedium, color = g.ink3, maxLines = 1)
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = g.accent, modifier = Modifier.size(16.dp).padding(horizontal = 2.dp))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(l.ticket.destination.ifBlank { "—" }, style = MaterialTheme.typography.titleLarge, color = g.ink, fontWeight = FontWeight.ExtraBold)
                    Text(l.address.dropCity.ifBlank { l.ticket.destination }, style = MaterialTheme.typography.labelMedium, color = g.ink3, maxLines = 1)
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalHair()
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("₹${l.askingPrice / 100}", style = MaterialTheme.typography.titleLarge, color = g.ink, fontWeight = FontWeight.ExtraBold)
                if (l.ticket.originalFare > l.askingPrice && l.ticket.originalFare > 0) {
                    Spacer(Modifier.width(6.dp))
                    Text("₹${l.ticket.originalFare / 100}", style = MaterialTheme.typography.bodySmall, color = g.ink3, textDecoration = TextDecoration.LineThrough)
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.clip(RoundedCornerShape(13.dp)).background(g.deep).padding(horizontal = 9.dp, vertical = 4.dp)) {
                        Text("Save ₹${(l.ticket.originalFare - l.askingPrice) / 100}", style = MaterialTheme.typography.labelSmall, color = g.onDeep)
                    }
                }
                Spacer(Modifier.weight(1f))
                Text("${relTime(l.ticket.journeyDateMillis)}", style = MaterialTheme.typography.labelMedium, color = g.ink3)
            }
        }
    }
}

@Composable
private fun HorizontalHair() {
    val g = LocalGlass.current
    Box(Modifier.fillMaxWidth().height(1.dp).background(g.hair))
}

@Composable
private fun EmptyHome() {
    val g = LocalGlass.current
    Column(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("No tickets available yet", style = MaterialTheme.typography.titleMedium, color = g.ink)
        Spacer(Modifier.height(6.dp))
        Text("Tap “Sell my ticket” to list one, or seed demo tickets from Profile.", style = MaterialTheme.typography.bodySmall, color = g.ink3)
    }
}

private fun relTime(millis: Long): String {
    if (millis <= 0) return "Date TBD"
    val now = System.currentTimeMillis()
    val days = ((millis - now) / (24L * 3600_000)).toInt()
    return when {
        days < 0 -> "${abs(days)}d ago"
        days == 0 -> "Today"
        days == 1 -> "Tomorrow"
        days < 7 -> "in ${days}d"
        else -> SimpleDateFormat("d MMM", Locale.ENGLISH).format(Date(millis))
    }
}

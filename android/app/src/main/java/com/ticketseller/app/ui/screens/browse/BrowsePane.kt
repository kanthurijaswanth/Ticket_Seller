package com.ticketseller.app.ui.screens.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ticketseller.app.data.model.Listing
import com.ticketseller.app.data.model.TicketType
import com.ticketseller.app.data.repo.ListingRepository
import com.ticketseller.app.ui.common.EmptyState
import com.ticketseller.app.ui.common.LoadingBox
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class BrowseViewModel @Inject constructor(
    listingRepository: ListingRepository,
) : ViewModel() {
    val listings: StateFlow<List<Listing>?> =
        listingRepository.observeActive()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun BrowsePane(
    vm: BrowseViewModel = hiltViewModel(),
    onListing: (String) -> Unit,
) {
    val all by vm.listings.collectAsState()
    var query by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf<TicketType?>(null) }

    Column(Modifier.fillMaxSize()) {
        // Search + filter section
        Surface(color = MaterialTheme.colorScheme.primary, tonalElevation = 0.dp) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search city, route or operator") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterPill("All", typeFilter == null) { typeFilter = null }
                    FilterPill("Bus", typeFilter == TicketType.BUS, Icons.Filled.DirectionsBus) {
                        typeFilter = TicketType.BUS
                    }
                    FilterPill("Train", typeFilter == TicketType.TRAIN, Icons.Filled.Train) {
                        typeFilter = TicketType.TRAIN
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        val items = all
        if (items == null) LoadingBox()
        else {
            val filtered = items.filter { l ->
                (typeFilter == null || l.ticket.type == typeFilter) &&
                    (query.isBlank() || listOf(
                        l.ticket.source, l.ticket.destination,
                        l.ticket.operator, l.ticket.trainOrBusNumber,
                        l.address.pickupCity, l.address.dropCity,
                    ).any { it.contains(query, ignoreCase = true) })
            }.sortedBy { it.ticket.journeyDateMillis }

            if (filtered.isEmpty()) {
                EmptyState(
                    "No tickets available yet",
                    "Be the first to list your unused ticket. Tap the 'Sell ticket' button at the bottom right, or seed demo tickets from Profile.",
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(filtered, key = { it.id }) { ListingCard(it) { onListing(it.id) } }
                }
            }
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    selected: Boolean,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    val container = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
    val content = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary
    Surface(
        color = container,
        shape = RoundedCornerShape(50),
        modifier = Modifier.clip(RoundedCornerShape(50)),
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary),
        onClick = onClick,
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, null, Modifier.size(16.dp), tint = content)
                Spacer(Modifier.width(4.dp))
            }
            Text(label, color = content, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ListingCard(l: Listing, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            // Row 1: type icon + operator + verified + price
            Row(verticalAlignment = Alignment.CenterVertically) {
                TypePill(l.ticket.type)
                Spacer(Modifier.width(8.dp))
                Text(
                    l.ticket.operator.ifBlank { "Operator" },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                PricePill(l.askingPrice, l.ticket.originalFare)
            }
            Spacer(Modifier.height(10.dp))

            // Row 2: route - big & bold
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        l.ticket.source.ifBlank { "—" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        l.address.pickupCity.ifBlank { l.ticket.source },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        l.ticket.destination.ifBlank { "—" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        l.address.dropCity.ifBlank { l.ticket.destination },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Row 3: when, seat, train#
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Schedule,
                    null,
                    Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    formatJourney(l.ticket.journeyDateMillis, l.ticket.departureTime),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.weight(1f))
                if (l.ticket.trainOrBusNumber.isNotBlank()) {
                    Text(
                        "#${l.ticket.trainOrBusNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            // Row 4: badges
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                VerifiedBadge()
                if (l.ticket.seatOrCoach.isNotBlank()) {
                    MiniBadge("Seat ${l.ticket.seatOrCoach}")
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "${l.sellerName.ifBlank { "Seller" }} · ${"%.1f".format(l.sellerRating)}★",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TypePill(type: TicketType) {
    val (label, icon) = when (type) {
        TicketType.BUS -> "Bus" to Icons.Filled.DirectionsBus
        TicketType.TRAIN -> "Train" to Icons.Filled.Train
    }
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(50),
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.width(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun PricePill(askingPriceMinor: Long, originalFareMinor: Long) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(50),
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                "₹${askingPriceMinor / 100}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            if (originalFareMinor > askingPriceMinor && originalFareMinor > 0) {
                Text(
                    "₹${originalFareMinor / 100}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f),
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                )
            }
        }
    }
}

@Composable
private fun VerifiedBadge() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(50),
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Verified,
                null,
                Modifier.size(12.dp),
                tint = androidx.compose.ui.graphics.Color(0xFF1B8A3A),
            )
            Spacer(Modifier.width(3.dp))
            Text("Verified", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MiniBadge(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

private fun formatJourney(millis: Long, time: String): String {
    if (millis <= 0) return "Date TBD"
    val now = System.currentTimeMillis()
    val days = ((millis - now) / (24L * 3600_000)).toInt()
    val relative = when {
        days < 0 -> "${abs(days)}d ago"
        days == 0 -> "Today"
        days == 1 -> "Tomorrow"
        days < 7 -> "in ${days}d"
        else -> SimpleDateFormat("d MMM", Locale.ENGLISH).format(Date(millis))
    }
    val dateLabel = SimpleDateFormat("EEE, d MMM", Locale.ENGLISH).format(Date(millis))
    return if (time.isNotBlank()) "$relative · $dateLabel · $time" else "$relative · $dateLabel"
}

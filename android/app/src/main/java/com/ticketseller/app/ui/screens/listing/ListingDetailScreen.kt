package com.ticketseller.app.ui.screens.listing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AirlineSeatReclineNormal
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ticketseller.app.data.model.ChatThread
import com.ticketseller.app.data.model.Listing
import com.ticketseller.app.data.model.Order
import com.ticketseller.app.data.model.OrderStatus
import com.ticketseller.app.data.model.TicketType
import com.ticketseller.app.data.repo.AuthRepository
import com.ticketseller.app.data.repo.ChatRepository
import com.ticketseller.app.data.repo.ListingRepository
import com.ticketseller.app.data.repo.OrderRepository
import com.ticketseller.app.data.repo.UserRepository
import com.ticketseller.app.ui.common.Base64Image
import com.ticketseller.app.ui.common.LoadingBox
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
class ListingDetailViewModel @Inject constructor(
    state: SavedStateHandle,
    listingRepository: ListingRepository,
    private val chatRepository: ChatRepository,
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val id: String = state["id"] ?: ""
    val listing: StateFlow<Listing?> =
        listingRepository.observe(id).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _action = MutableStateFlow<DetailAction?>(null)
    val action: StateFlow<DetailAction?> = _action

    sealed interface DetailAction {
        data class OpenChat(val threadId: String) : DetailAction
        data class OpenPayment(val orderId: String) : DetailAction
    }

    fun startChat() {
        val l = listing.value ?: return
        val uid = authRepository.effectiveUid() ?: return
        if (uid == l.sellerId) return
        viewModelScope.launch {
            val me = userRepository.get(uid)
            val seller = userRepository.get(l.sellerId)
            val thread = chatRepository.openThread(
                ChatThread(
                    listingId = l.id,
                    listingTitle = "${l.ticket.source} → ${l.ticket.destination}",
                    buyerId = uid,
                    sellerId = l.sellerId,
                    buyerName = me?.name.orEmpty(),
                    sellerName = seller?.name.orEmpty(),
                )
            )
            _action.value = DetailAction.OpenChat(thread.id)
        }
    }

    fun beginOrder() {
        val l = listing.value ?: return
        val uid = authRepository.effectiveUid() ?: return
        if (uid == l.sellerId) return
        viewModelScope.launch {
            val orderId = orderRepository.create(
                Order(
                    listingId = l.id,
                    buyerId = uid,
                    sellerId = l.sellerId,
                    amount = l.askingPrice,
                    status = OrderStatus.AWAITING_PAYMENT,
                )
            )
            _action.value = DetailAction.OpenPayment(orderId)
        }
    }

    fun consume() { _action.value = null }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingDetailScreen(
    listingId: String,
    vm: ListingDetailViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    onOpenPayment: (String) -> Unit,
) {
    val listing by vm.listing.collectAsState()
    val action by vm.action.collectAsState()
    LaunchedEffect(action) {
        when (val a = action) {
            is ListingDetailViewModel.DetailAction.OpenChat -> { onOpenChat(a.threadId); vm.consume() }
            is ListingDetailViewModel.DetailAction.OpenPayment -> { onOpenPayment(a.orderId); vm.consume() }
            null -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ticket details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        bottomBar = {
            val l = listing
            if (l != null) {
                Surface(tonalElevation = 8.dp) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(
                            onClick = vm::startChat,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Chat, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Chat")
                        }
                        Button(
                            onClick = vm::beginOrder,
                            modifier = Modifier.weight(1.4f),
                        ) {
                            Icon(Icons.Filled.ShoppingBag, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Buy · ₹${l.askingPrice / 100}", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        },
    ) { pad ->
        val l = listing
        if (l == null) LoadingBox()
        else Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()),
        ) {
            // ─── Hero ─────────────────────────────────
            HeroSection(l)

            Column(
                Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Spacer(Modifier.height(4.dp))
                JourneyCard(l)
                OperatorCard(l)
                AddressCard(l)
                if (l.ticketImageBase64.isNotBlank()) ImageCard(l)
                SellerCard(l)
                VerificationCard(l)
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun HeroSection(l: Listing) {
    Surface(color = MaterialTheme.colorScheme.primary) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(50),
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (l.ticket.type == TicketType.BUS) Icons.Filled.DirectionsBus else Icons.Filled.Train,
                            null,
                            Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            l.ticket.type.name.lowercase().replaceFirstChar { it.titlecase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Surface(
                    color = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(50),
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Verified,
                            null,
                            Modifier.size(14.dp),
                            tint = androidx.compose.ui.graphics.Color(0xFF1B8A3A),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Verified",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        l.ticket.source.ifBlank { "—" },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Text(
                        l.address.pickupCity.ifBlank { l.ticket.source },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        l.ticket.destination.ifBlank { "—" },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Text(
                        l.address.dropCity.ifBlank { l.ticket.destination },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    )
                }
            }
        }
    }
}

@Composable
private fun JourneyCard(l: Listing) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionTitle("When", Icons.Filled.Schedule)
            Spacer(Modifier.height(8.dp))
            Text(
                if (l.ticket.journeyDateMillis > 0)
                    SimpleDateFormat("EEEE, d MMM yyyy", Locale.ENGLISH).format(Date(l.ticket.journeyDateMillis))
                else "Date to be confirmed",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (l.ticket.departureTime.isNotBlank()) {
                Text(
                    "Departure ${l.ticket.departureTime}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                relativeTime(l.ticket.journeyDateMillis),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun OperatorCard(l: Listing) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionTitle(
                if (l.ticket.type == TicketType.BUS) "Bus details" else "Train details",
                if (l.ticket.type == TicketType.BUS) Icons.Filled.DirectionsBus else Icons.Filled.Train,
            )
            Spacer(Modifier.height(8.dp))
            KeyValue("Operator", l.ticket.operator.ifBlank { "—" })
            KeyValue(
                if (l.ticket.type == TicketType.BUS) "Bus number" else "Train number",
                l.ticket.trainOrBusNumber.ifBlank { "—" },
            )
            KeyValue("PNR / Booking", l.ticket.pnrOrBookingId.ifBlank { "—" })
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.weight(1f)) { KeyValue("Seat / coach", l.ticket.seatOrCoach.ifBlank { "—" }) }
                if (l.ticket.berth.name != "UNSPECIFIED") {
                    Box(Modifier.weight(1f)) {
                        KeyValue("Berth", l.ticket.berth.name.lowercase().replace('_', ' '))
                    }
                }
            }
            if (l.ticket.passengerName.isNotBlank()) {
                KeyValue("Passenger", l.ticket.passengerName)
            }
        }
    }
}

@Composable
private fun AddressCard(l: Listing) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionTitle("Boarding & arrival", Icons.Filled.LocationOn)
            Spacer(Modifier.height(8.dp))
            AddressRow(
                label = "Boarding",
                line1 = l.address.pickupLine1,
                city = l.address.pickupCity,
                pincode = l.address.pickupPincode,
                landmark = l.address.pickupLandmark,
            )
            Spacer(Modifier.height(12.dp))
            AddressRow(
                label = "Arrival",
                line1 = l.address.dropLine1,
                city = l.address.dropCity,
                pincode = l.address.dropPincode,
                landmark = null,
            )
            if (l.address.notes.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        "Note: ${l.address.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageCard(l: Listing) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionTitle("Ticket image", Icons.Filled.ConfirmationNumber)
            Spacer(Modifier.height(8.dp))
            Base64Image(
                l.ticketImageBase64,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
        }
    }
}

@Composable
private fun SellerCard(l: Listing) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = androidx.compose.foundation.shape.CircleShape,
                modifier = Modifier.size(44.dp),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        l.sellerName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    l.sellerName.ifBlank { "Seller" },
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Star, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "%.1f rating".format(l.sellerRating),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun VerificationCard(l: Listing) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Filled.Verified,
                null,
                tint = androidx.compose.ui.graphics.Color(0xFF1B8A3A),
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    "Verified by TicketResell",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    l.verificationNotes.ifBlank {
                        "We auto-check the ticket image against existing listings and validate key fields."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(6.dp))
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun KeyValue(key: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            key,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AddressRow(
    label: String,
    line1: String,
    city: String,
    pincode: String,
    landmark: String?,
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            Icons.Filled.LocationOn,
            null,
            Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                line1.ifBlank { "—" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            val rest = listOfNotNull(
                city.ifBlank { null },
                pincode.ifBlank { null },
                landmark?.takeIf { it.isNotBlank() },
            )
            if (rest.isNotEmpty()) {
                Text(
                    rest.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun relativeTime(millis: Long): String {
    if (millis <= 0) return ""
    val now = System.currentTimeMillis()
    val diffMs = millis - now
    val absMs = abs(diffMs)
    val days = (absMs / (24L * 3600_000)).toInt()
    val hours = ((absMs / 3600_000) % 24).toInt()
    val verb = if (diffMs < 0) "ago" else "from now"
    return when {
        days >= 1 && hours > 0 -> "$days day${if (days > 1) "s" else ""} $hours h $verb"
        days >= 1 -> "$days day${if (days > 1) "s" else ""} $verb"
        hours >= 1 -> "$hours h $verb"
        else -> "Soon"
    }
}

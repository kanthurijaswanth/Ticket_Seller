package com.ticketseller.app.ui.screens.listing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ticketseller.app.data.model.ChatThread
import com.ticketseller.app.data.model.Listing
import com.ticketseller.app.data.model.ListingStatus
import com.ticketseller.app.data.model.Order
import com.ticketseller.app.data.model.OrderStatus
import com.ticketseller.app.data.model.TicketType
import com.ticketseller.app.data.repo.AuthRepository
import com.ticketseller.app.data.repo.ChatRepository
import com.ticketseller.app.data.repo.ListingRepository
import com.ticketseller.app.data.repo.OrderRepository
import com.ticketseller.app.data.repo.UserRepository
import com.ticketseller.app.ui.common.AppBackground
import com.ticketseller.app.ui.common.GlassCard
import com.ticketseller.app.ui.common.GlassIconButton
import com.ticketseller.app.ui.common.GradientButton
import com.ticketseller.app.ui.common.LoadingBox
import com.ticketseller.app.ui.common.TintChip
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

@HiltViewModel
class ListingDetailViewModel @Inject constructor(
    state: SavedStateHandle,
    private val listingRepository: ListingRepository,
    private val chatRepository: ChatRepository,
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val id: String = state["id"] ?: ""
    val myUid: String? = authRepository.effectiveUid()

    val listing: StateFlow<Listing?> =
        listingRepository.observe(id).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _action = MutableStateFlow<DetailAction?>(null)
    val action: StateFlow<DetailAction?> = _action

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message
    fun clearMessage() { _message.value = null }

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
                    buyerId = uid, sellerId = l.sellerId,
                    buyerName = me?.name.orEmpty(), sellerName = seller?.name.orEmpty(),
                ),
            )
            _action.value = DetailAction.OpenChat(thread.id)
        }
    }

    fun beginOrder() {
        val l = listing.value ?: return
        val uid = authRepository.effectiveUid() ?: return
        if (uid == l.sellerId) return
        viewModelScope.launch {
            if (l.status != ListingStatus.ACTIVE) { _message.value = "Sorry — this ticket is no longer available."; return@launch }
            val existing = orderRepository.activeOrderForListing(l.id)
            if (existing != null) {
                if (existing.buyerId == uid) _action.value = DetailAction.OpenPayment(existing.id)
                else _message.value = "Someone else is currently buying this ticket."
                return@launch
            }
            val orderId = orderRepository.create(
                Order(listingId = l.id, buyerId = uid, sellerId = l.sellerId, amount = l.askingPrice, status = OrderStatus.AWAITING_PAYMENT),
            )
            listingRepository.setStatus(l.id, ListingStatus.RESERVED)
            _action.value = DetailAction.OpenPayment(orderId)
        }
    }

    fun consume() { _action.value = null }
}

@Composable
fun ListingDetailScreen(
    listingId: String,
    vm: ListingDetailViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    onOpenPayment: (String) -> Unit,
) {
    val g = LocalGlass.current
    val listing by vm.listing.collectAsState()
    val action by vm.action.collectAsState()
    val message by vm.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(action) {
        when (val a = action) {
            is ListingDetailViewModel.DetailAction.OpenChat -> { onOpenChat(a.threadId); vm.consume() }
            is ListingDetailViewModel.DetailAction.OpenPayment -> { onOpenPayment(a.orderId); vm.consume() }
            null -> Unit
        }
    }
    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); vm.clearMessage() } }

    AppBackground {
        val l = listing
        Box(Modifier.fillMaxSize().statusBarsPadding()) {
            if (l == null) {
                LoadingBox()
            } else {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp).padding(bottom = 110.dp)) {
                    // Header
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GlassIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
                        Spacer(Modifier.width(12.dp))
                        Text("Ticket details", style = MaterialTheme.typography.titleMedium, color = g.ink, fontWeight = FontWeight.ExtraBold)
                    }

                    Spacer(Modifier.height(16.dp))
                    HeroCard(l)
                    Spacer(Modifier.height(12.dp))
                    BoardingCard(l)
                    Spacer(Modifier.height(12.dp))
                    PaperworkCard(l)
                    Spacer(Modifier.height(12.dp))
                    SellerCard(l)
                    Spacer(Modifier.height(12.dp))
                    SafetyCard()
                }

                // Bottom action bar
                BottomBar(
                    l = l,
                    isMine = l.sellerId == vm.myUid,
                    onChat = vm::startChat,
                    onBuy = vm::beginOrder,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp))
        }
    }
}

@Composable
private fun HeroCard(l: Listing) {
    val g = LocalGlass.current
    val verified = l.status in listOf(ListingStatus.ACTIVE, ListingStatus.RESERVED, ListingStatus.SOLD)
    GlassCard(strong = true, shape = RoundedCornerShape(28.dp)) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TintChip(if (l.ticket.type == TicketType.BUS) "Bus" else "Train")
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.clip(RoundedCornerShape(14.dp)).background(if (verified) g.deep else g.tint).padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (verified) Icon(Icons.Filled.Check, null, tint = g.onDeep, modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (verified) "Checked twice" else "Pending review", style = MaterialTheme.typography.labelSmall, color = if (verified) g.onDeep else g.tintInk)
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(l.ticket.source.ifBlank { "—" }, style = MaterialTheme.typography.displaySmall, color = g.ink, fontWeight = FontWeight.ExtraBold)
                    Text(l.address.pickupCity.ifBlank { l.ticket.source }, style = MaterialTheme.typography.labelMedium, color = g.ink3, modifier = Modifier.padding(top = 4.dp))
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = g.accent, modifier = Modifier.size(16.dp).padding(top = 6.dp))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(l.ticket.destination.ifBlank { "—" }, style = MaterialTheme.typography.displaySmall, color = g.ink, fontWeight = FontWeight.ExtraBold)
                    Text(l.address.dropCity.ifBlank { l.ticket.destination }, style = MaterialTheme.typography.labelMedium, color = g.ink3, modifier = Modifier.padding(top = 4.dp))
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                MiniStat("Leaves", if (l.ticket.journeyDateMillis > 0) SimpleDateFormat("EEE, d MMM", Locale.ENGLISH).format(Date(l.ticket.journeyDateMillis)) else "TBD", l.ticket.departureTime, Modifier.weight(1f))
                MiniStat("Your seat", l.ticket.seatOrCoach.ifBlank { "—" }, l.ticket.berth.name.lowercase().replace('_', ' '), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, sub: String, modifier: Modifier = Modifier) {
    val g = LocalGlass.current
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(g.glass).padding(12.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = g.ink3)
        Text(value, style = MaterialTheme.typography.titleSmall, color = g.ink, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 6.dp))
        if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.labelMedium, color = g.accent, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun BoardingCard(l: Listing) {
    val g = LocalGlass.current
    GlassCard {
        Column(Modifier.padding(18.dp)) {
            Text("Where you get on and off", style = MaterialTheme.typography.titleSmall, color = g.ink, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(14.dp))
            Row {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(11.dp).clip(CircleShape).background(g.deep))
                    Box(Modifier.width(2.dp).height(38.dp).background(g.hair))
                    Box(Modifier.size(11.dp).clip(CircleShape).background(g.ink))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(l.address.pickupLine1.ifBlank { l.ticket.source }, style = MaterialTheme.typography.titleSmall, color = g.ink)
                    Text("${l.address.pickupCity} · ${l.ticket.departureTime}", style = MaterialTheme.typography.labelMedium, color = g.ink3, modifier = Modifier.padding(top = 2.dp))
                    Spacer(Modifier.height(20.dp))
                    Text(l.address.dropLine1.ifBlank { l.ticket.destination }, style = MaterialTheme.typography.titleSmall, color = g.ink)
                    Text(l.address.dropCity, style = MaterialTheme.typography.labelMedium, color = g.ink3, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}

@Composable
private fun PaperworkCard(l: Listing) {
    val g = LocalGlass.current
    GlassCard {
        Column(Modifier.padding(18.dp)) {
            Text("Ticket paperwork", style = MaterialTheme.typography.titleSmall, color = g.ink, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(10.dp))
            val rows = listOf(
                "Operator" to l.ticket.operator,
                (if (l.ticket.type == TicketType.BUS) "Bus number" else "Train number") to l.ticket.trainOrBusNumber,
                "PNR / booking" to l.ticket.pnrOrBookingId,
                "Name on ticket" to l.ticket.passengerName,
            )
            rows.forEach { (k, v) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(k, style = MaterialTheme.typography.bodySmall, color = g.ink3, modifier = Modifier.weight(1f))
                    Text(v.ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium, color = g.ink, fontWeight = FontWeight.SemiBold)
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(g.hair))
            }
        }
    }
}

@Composable
private fun SellerCard(l: Listing) {
    val g = LocalGlass.current
    GlassCard {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).background(Brush.linearGradient(listOf(g.brand, g.deep))), contentAlignment = Alignment.Center) {
                Text(l.sellerName.firstOrNull()?.uppercase() ?: "?", color = g.onBrandStrong, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(l.sellerName.ifBlank { "Seller" }, style = MaterialTheme.typography.titleSmall, color = g.ink, fontWeight = FontWeight.Bold)
                Text("%.1f★ · replies fast".format(l.sellerRating), style = MaterialTheme.typography.labelMedium, color = g.ink3, modifier = Modifier.padding(top = 3.dp))
            }
        }
    }
}

@Composable
private fun SafetyCard() {
    val g = LocalGlass.current
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(g.tint).padding(16.dp)) {
        Text("Why this is safe", style = MaterialTheme.typography.titleSmall, color = g.tintInk, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(9.dp))
        listOf(
            "The ticket photo was read by the app and matched to the details above.",
            "Chat with the seller first. Share phone numbers only when you both agree.",
        ).forEach {
            Row(Modifier.padding(vertical = 4.dp)) {
                Box(Modifier.padding(top = 6.dp).size(6.dp).clip(CircleShape).background(g.accent))
                Spacer(Modifier.width(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = g.ink2)
            }
        }
    }
}

@Composable
private fun BottomBar(l: Listing, isMine: Boolean, onChat: () -> Unit, onBuy: () -> Unit, modifier: Modifier = Modifier) {
    val g = LocalGlass.current
    Row(
        modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)
            .clip(RoundedCornerShape(24.dp)).background(g.bg1).background(g.glassStrong).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when {
            isMine -> Text("This is your listing" + statusSuffix(l.status), style = MaterialTheme.typography.titleSmall, color = g.ink, modifier = Modifier.padding(8.dp))
            l.status == ListingStatus.SOLD -> Text("This ticket has been sold", color = g.ink3, modifier = Modifier.padding(8.dp))
            l.status == ListingStatus.RESERVED -> Text("Reserved by another buyer", color = g.ink3, modifier = Modifier.padding(8.dp))
            else -> {
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(g.glass).clickable(onClick = onChat).padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Chat, null, tint = g.ink, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp)); Text("Chat", color = g.ink, style = MaterialTheme.typography.labelLarge)
                    }
                }
                GradientButton(text = "Buy · ₹${l.askingPrice / 100}", modifier = Modifier.weight(1.4f), height = 50, onClick = onBuy)
            }
        }
    }
}

private fun statusSuffix(s: ListingStatus) = when (s) {
    ListingStatus.SOLD -> " · Sold"
    ListingStatus.RESERVED -> " · Reserved"
    ListingStatus.PENDING_REVIEW -> " · Pending"
    else -> ""
}

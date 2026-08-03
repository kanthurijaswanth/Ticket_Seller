package com.ticketseller.app.ui.screens.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ticketseller.app.data.model.Listing
import com.ticketseller.app.data.model.ListingStatus
import com.ticketseller.app.data.model.Order
import com.ticketseller.app.data.model.OrderStatus
import com.ticketseller.app.data.model.Rating
import com.ticketseller.app.data.repo.AuthRepository
import com.ticketseller.app.data.repo.ListingRepository
import com.ticketseller.app.data.repo.OrderRepository
import com.ticketseller.app.data.repo.RatingRepository
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
import javax.inject.Inject

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    state: SavedStateHandle,
    private val orderRepository: OrderRepository,
    private val listingRepository: ListingRepository,
    private val ratingRepository: RatingRepository,
    authRepository: AuthRepository,
) : ViewModel() {
    private val id: String = state["id"] ?: ""
    val myUid: String = authRepository.effectiveUid().orEmpty()

    val order: StateFlow<Order?> =
        orderRepository.observe(id).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _listing = MutableStateFlow<Listing?>(null)
    val listing: StateFlow<Listing?> = _listing
    private val _hasRated = MutableStateFlow(false)
    val hasRated: StateFlow<Boolean> = _hasRated
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    init {
        viewModelScope.launch {
            orderRepository.get(id)?.let { _listing.value = listingRepository.get(it.listingId) }
            _hasRated.value = ratingRepository.hasRated(id, myUid)
        }
    }

    fun markCompleted() = viewModelScope.launch {
        _busy.value = true
        runCatching { orderRepository.setStatus(id, OrderStatus.COMPLETED) }
        _busy.value = false
    }

    fun cancel() = viewModelScope.launch {
        _busy.value = true
        runCatching {
            orderRepository.setStatus(id, OrderStatus.CANCELLED)
            _listing.value?.let { listingRepository.setStatus(it.id, ListingStatus.ACTIVE) }
        }
        _busy.value = false
    }

    fun submitRating(stars: Int, comment: String) = viewModelScope.launch {
        val o = order.value ?: return@launch
        val rateeId = if (o.sellerId == myUid) o.buyerId else o.sellerId
        if (rateeId.isBlank() || rateeId == myUid) return@launch
        _busy.value = true
        runCatching { ratingRepository.submit(Rating(orderId = id, raterId = myUid, rateeId = rateeId, stars = stars, comment = comment.trim())) }
            .onSuccess { _hasRated.value = true }
        _busy.value = false
    }
}

@Composable
fun OrderDetailScreen(
    orderId: String,
    vm: OrderDetailViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onPay: () -> Unit,
) {
    val g = LocalGlass.current
    val o by vm.order.collectAsState()
    val listing by vm.listing.collectAsState()
    val hasRated by vm.hasRated.collectAsState()
    val busy by vm.busy.collectAsState()

    AppBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(18.dp).padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
                Spacer(Modifier.width(12.dp))
                Text("Order", style = MaterialTheme.typography.titleMedium, color = g.ink, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(16.dp))

            val order = o
            if (order == null) { LoadingBox(); return@Column }
            val isSeller = order.sellerId == vm.myUid

            GlassCard(strong = true) {
                Column(Modifier.padding(20.dp)) {
                    listing?.let {
                        Text("${it.ticket.source} → ${it.ticket.destination}", style = MaterialTheme.typography.titleLarge, color = g.ink, fontWeight = FontWeight.ExtraBold)
                        Text(it.ticket.operator, style = MaterialTheme.typography.labelMedium, color = g.ink3, modifier = Modifier.padding(top = 4.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("₹${order.amount / 100}", style = MaterialTheme.typography.headlineSmall, color = g.ink, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.weight(1f))
                        TintChip(order.status.name.replace('_', ' '))
                    }
                    Text(if (isSeller) "You are the seller" else "You are the buyer", style = MaterialTheme.typography.labelMedium, color = g.ink3, modifier = Modifier.padding(top = 8.dp))
                }
            }

            Spacer(Modifier.height(12.dp))
            StatusCard(order.status, isSeller)
            Spacer(Modifier.height(12.dp))

            when (order.status) {
                OrderStatus.AWAITING_PAYMENT, OrderStatus.INITIATED -> {
                    if (isSeller) InfoNote("Waiting for the buyer to pay. You'll see it here once they do.")
                    else {
                        GradientButton("Pay now", enabled = !busy, onClick = onPay)
                        Spacer(Modifier.height(10.dp))
                        GlassButton("Cancel order", enabled = !busy, onClick = vm::cancel)
                    }
                }
                OrderStatus.PAID_OFFLINE, OrderStatus.CONTACT_SHARED -> {
                    InfoNote(if (isSeller) "The buyer has paid. Share the ticket in chat, then mark it completed." else "You've paid. Coordinate the handover with the seller in chat.")
                    Spacer(Modifier.height(10.dp))
                    GradientButton("Mark as completed", enabled = !busy, onClick = vm::markCompleted)
                }
                OrderStatus.COMPLETED -> {
                    if (hasRated) InfoNote("Thanks for rating! This order is complete.")
                    else RatingSection(isSeller = isSeller, busy = busy, onSubmit = vm::submitRating)
                }
                OrderStatus.CANCELLED -> InfoNote("This order was cancelled.")
                OrderStatus.DISPUTED -> InfoNote("This order is under dispute. Please contact support.")
            }

            Spacer(Modifier.height(16.dp))
            Text("Order ID: ${order.id}", style = MaterialTheme.typography.labelSmall, color = g.ink3)
        }
    }
}

@Composable
private fun StatusCard(status: OrderStatus, isSeller: Boolean) {
    val g = LocalGlass.current
    val (icon, title) = when (status) {
        OrderStatus.AWAITING_PAYMENT, OrderStatus.INITIATED -> Icons.Filled.HourglassEmpty to (if (isSeller) "Awaiting the buyer's payment" else "Payment pending")
        OrderStatus.PAID_OFFLINE, OrderStatus.CONTACT_SHARED -> Icons.Filled.CheckCircle to "Paid"
        OrderStatus.COMPLETED -> Icons.Filled.CheckCircle to "Completed"
        OrderStatus.CANCELLED -> Icons.Filled.Info to "Cancelled"
        OrderStatus.DISPUTED -> Icons.Filled.Info to "Disputed"
    }
    GlassCard {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = g.accent)
            Spacer(Modifier.width(10.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, color = g.ink, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun InfoNote(text: String) {
    val g = LocalGlass.current
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
        Icon(Icons.Filled.Info, null, Modifier.size(18.dp), tint = g.ink3)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = g.ink2)
    }
}

@Composable
private fun GlassButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    val g = LocalGlass.current
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(g.glass).border(1.dp, g.glassBorder, RoundedCornerShape(18.dp))
            .clickable(enabled = enabled) { onClick() }.padding(vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = g.ink, style = MaterialTheme.typography.labelLarge) }
}

@Composable
private fun RatingSection(isSeller: Boolean, busy: Boolean, onSubmit: (Int, String) -> Unit) {
    val g = LocalGlass.current
    var stars by remember { mutableStateOf(5) }
    var comment by remember { mutableStateOf("") }
    GlassCard {
        Column(Modifier.padding(16.dp)) {
            Text(if (isSeller) "Rate the buyer" else "Rate the seller", style = MaterialTheme.typography.titleSmall, color = g.ink, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(10.dp))
            Row {
                (1..5).forEach { i ->
                    Icon(
                        if (i <= stars) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        "$i stars", tint = g.accent,
                        modifier = Modifier.size(34.dp).clickable { stars = i }.padding(2.dp),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(g.glass).border(1.dp, g.glassBorder, RoundedCornerShape(16.dp)).padding(14.dp)) {
                if (comment.isEmpty()) Text("Add a comment (optional)", style = MaterialTheme.typography.bodyMedium, color = g.ink3)
                BasicTextField(
                    value = comment, onValueChange = { comment = it },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = g.ink),
                    cursorBrush = SolidColor(g.deep), minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(12.dp))
            GradientButton("Submit rating", enabled = !busy) { onSubmit(stars, comment) }
        }
    }
}

package com.ticketseller.app.ui.screens.orders

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ticketseller.app.data.model.Listing
import com.ticketseller.app.data.model.ListingStatus
import com.ticketseller.app.data.model.Order
import com.ticketseller.app.data.model.OrderStatus
import com.ticketseller.app.data.model.User
import com.ticketseller.app.data.repo.ListingRepository
import com.ticketseller.app.data.repo.OrderRepository
import com.ticketseller.app.data.repo.UserRepository
import com.ticketseller.app.ui.common.AppBackground
import com.ticketseller.app.ui.common.GlassCard
import com.ticketseller.app.ui.common.GlassIconButton
import com.ticketseller.app.ui.common.GradientButton
import com.ticketseller.app.ui.common.LoadingBox
import com.ticketseller.app.ui.theme.LocalGlass
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaymentViewModel @Inject constructor(
    state: SavedStateHandle,
    private val orderRepository: OrderRepository,
    private val listingRepository: ListingRepository,
    private val userRepository: UserRepository,
) : ViewModel() {
    private val orderId: String = state["orderId"] ?: ""

    val order: StateFlow<Order?> =
        orderRepository.observe(orderId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _seller = MutableStateFlow<User?>(null)
    val seller: StateFlow<User?> = _seller
    private val _listing = MutableStateFlow<Listing?>(null)
    val listing: StateFlow<Listing?> = _listing
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy
    // Optimistic: shows the success state immediately after confirming, even before realtime.
    private val _justPaid = MutableStateFlow(false)
    val justPaid: StateFlow<Boolean> = _justPaid

    init {
        viewModelScope.launch {
            val o = orderRepository.get(orderId) ?: return@launch
            _seller.value = userRepository.get(o.sellerId)
            _listing.value = listingRepository.get(o.listingId)
        }
    }

    fun confirmPaid(paymentRef: String) {
        viewModelScope.launch {
            _busy.value = true
            runCatching {
                orderRepository.markPaid(orderId, paymentRef)
                _listing.value?.let { listingRepository.setStatus(it.id, ListingStatus.SOLD) }
            }.onSuccess { _justPaid.value = true }
            _busy.value = false
        }
    }

    fun cancel(onDone: () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            runCatching {
                orderRepository.setStatus(orderId, OrderStatus.CANCELLED)
                _listing.value?.let { listingRepository.setStatus(it.id, ListingStatus.ACTIVE) }
            }
            _busy.value = false
            onDone()
        }
    }
}

@Composable
fun PaymentScreen(
    orderId: String,
    vm: PaymentViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val g = LocalGlass.current
    val order by vm.order.collectAsState()
    val seller by vm.seller.collectAsState()
    val listing by vm.listing.collectAsState()
    val busy by vm.busy.collectAsState()
    val justPaid by vm.justPaid.collectAsState()
    val ctx = LocalContext.current

    var showConfirm by remember { mutableStateOf(false) }
    var pendingRef by remember { mutableStateOf("upi") }

    val upiLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val resp = result.data?.getStringExtra("response") ?: result.data?.getStringExtra("Status")
        when (parseUpiStatus(resp)) {
            UpiResult.SUCCESS -> vm.confirmPaid(resp ?: "upi-success")
            UpiResult.FAILURE -> Toast.makeText(ctx, "Payment failed or was declined.", Toast.LENGTH_LONG).show()
            UpiResult.UNKNOWN -> { pendingRef = resp ?: "upi"; showConfirm = true }
        }
    }

    AppBackground {
        val o = order
        Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(18.dp).padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
                Spacer(Modifier.width(12.dp))
                Text("Payment", style = MaterialTheme.typography.titleMedium, color = g.ink, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(18.dp))

            if (o == null) { LoadingBox(); return@Column }
            val paid = justPaid || o.status == OrderStatus.PAID_OFFLINE || o.status == OrderStatus.COMPLETED
            val cancelled = o.status == OrderStatus.CANCELLED

            when {
                paid -> PaidState(o)
                cancelled -> Centered(Icons.Filled.Info, "Order cancelled", g.ink3)
                else -> {
                    // Ticket summary
                    listing?.let { l ->
                        GlassCard {
                            Column(Modifier.padding(16.dp)) {
                                Text("${l.ticket.source} → ${l.ticket.destination}", style = MaterialTheme.typography.titleMedium, color = g.ink, fontWeight = FontWeight.ExtraBold)
                                Text(l.ticket.operator.ifBlank { if (l.ticket.type.name == "BUS") "Bus ticket" else "Train ticket" }, style = MaterialTheme.typography.labelMedium, color = g.ink3)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    // Amount
                    GlassCard(strong = true) {
                        Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("AMOUNT TO PAY", style = MaterialTheme.typography.labelSmall, color = g.ink3)
                            Text("₹${o.amount / 100}", style = MaterialTheme.typography.displaySmall, color = g.ink, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 6.dp))
                            Text("to ${seller?.name?.ifBlank { "the seller" } ?: "the seller"}", style = MaterialTheme.typography.bodyMedium, color = g.ink3, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                    Spacer(Modifier.height(14.dp))

                    val sellerUpi = seller?.upiId.orEmpty()
                    if (sellerUpi.isNotBlank()) {
                        GlassCard {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.AccountBalanceWallet, null, tint = g.accent)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("SELLER UPI ID", style = MaterialTheme.typography.labelSmall, color = g.ink3)
                                    Text(sellerUpi, style = MaterialTheme.typography.titleSmall, color = g.ink, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        GradientButton(text = "Pay ₹${o.amount / 100} via UPI", enabled = !busy) {
                            val intent = buildUpiIntent(sellerUpi, seller?.name?.ifBlank { "TicketResell seller" } ?: "Seller", o.amount / 100.0, "TicketResell ${o.id.take(8)}")
                            runCatching { upiLauncher.launch(Intent.createChooser(intent, "Pay with")) }
                                .onFailure { Toast.makeText(ctx, "No UPI app found. Install GPay / PhonePe / Paytm, or pay the seller manually.", Toast.LENGTH_LONG).show() }
                        }
                        Spacer(Modifier.height(10.dp))
                        GlassOutlineButton("I've already paid the seller", enabled = !busy) { pendingRef = "manual"; showConfirm = true }
                    } else {
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(g.tint).padding(16.dp)) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Filled.Info, null, tint = g.accent)
                                Spacer(Modifier.width(10.dp))
                                Text("The seller hasn't added a UPI ID yet. Open the chat and ask for it, then tap below once you've paid.", style = MaterialTheme.typography.bodyMedium, color = g.ink2)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        GlassOutlineButton("I've paid the seller", enabled = !busy) { pendingRef = "manual"; showConfirm = true }
                    }

                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { vm.cancel(onBack) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Cancel this order", color = g.ink3) }
                    Spacer(Modifier.height(6.dp))
                    Text("TicketResell doesn't hold your money — payment goes straight to the seller's UPI. Only confirm after you've actually sent it.", style = MaterialTheme.typography.labelMedium, color = g.ink3, textAlign = TextAlign.Center)
                }
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Confirm payment") },
            text = { Text("Only tap Yes if you've actually sent the money to the seller. This marks the ticket as sold.") },
            confirmButton = { TextButton(onClick = { showConfirm = false; vm.confirmPaid(pendingRef) }) { Text("Yes, I've paid") } },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Not yet") } },
        )
    }
}

@Composable
private fun PaidState(order: Order) {
    val g = LocalGlass.current
    Column(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.CheckCircle, null, Modifier.size(72.dp), tint = Color(0xFF1B8A3A))
        Spacer(Modifier.height(12.dp))
        Text("Payment recorded", style = MaterialTheme.typography.headlineSmall, color = g.ink, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))
        Text("You paid ₹${order.amount / 100}. The seller can now confirm the handover. Coordinate the ticket details in chat.", style = MaterialTheme.typography.bodyMedium, color = g.ink2, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Centered(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, tint: Color) {
    val g = LocalGlass.current
    Column(Modifier.fillMaxWidth().padding(top = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, Modifier.size(56.dp), tint = tint)
        Spacer(Modifier.height(10.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, color = g.ink)
    }
}

@Composable
private fun GlassOutlineButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    val g = LocalGlass.current
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(g.glass).border(1.dp, g.glassBorder, RoundedCornerShape(18.dp))
            .clickable(enabled = enabled) { onClick() }.padding(vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = g.ink, style = MaterialTheme.typography.labelLarge) }
}

private enum class UpiResult { SUCCESS, FAILURE, UNKNOWN }

private fun parseUpiStatus(response: String?): UpiResult {
    if (response.isNullOrBlank()) return UpiResult.UNKNOWN
    val s = response.lowercase()
    return when {
        "status=success" in s || ("txnid" in s && "success" in s) -> UpiResult.SUCCESS
        "failure" in s || "declined" in s -> UpiResult.FAILURE
        else -> UpiResult.UNKNOWN
    }
}

private fun buildUpiIntent(upiId: String, payeeName: String, amountRupees: Double, note: String): Intent {
    val uri = Uri.parse("upi://pay").buildUpon()
        .appendQueryParameter("pa", upiId)
        .appendQueryParameter("pn", payeeName)
        .appendQueryParameter("am", "%.2f".format(amountRupees))
        .appendQueryParameter("cu", "INR")
        .appendQueryParameter("tn", note)
        .build()
    return Intent(Intent.ACTION_VIEW, uri)
}

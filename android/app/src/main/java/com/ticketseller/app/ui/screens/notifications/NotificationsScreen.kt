package com.ticketseller.app.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ticketseller.app.data.model.Order
import com.ticketseller.app.data.model.OrderStatus
import com.ticketseller.app.data.repo.AuthRepository
import com.ticketseller.app.data.repo.OrderRepository
import com.ticketseller.app.ui.common.AppBackground
import com.ticketseller.app.ui.common.GlassCard
import com.ticketseller.app.ui.theme.LocalGlass
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.math.abs

data class Notif(
    val glyph: String,
    val title: String,
    val body: String,
    val at: String,
    val unread: Boolean,
    val orderId: String?,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    authRepository: AuthRepository,
    orderRepository: OrderRepository,
) : ViewModel() {
    private val uid = MutableStateFlow(authRepository.effectiveUid().orEmpty())

    val items: StateFlow<List<Notif>?> = uid.flatMapLatest { id ->
        if (id.isBlank()) flowOf(emptyList())
        else combine(
            orderRepository.observeMine(id, asBuyer = true),
            orderRepository.observeMine(id, asBuyer = false),
        ) { bought, sold -> build(bought, sold) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun build(bought: List<Order>, sold: List<Order>): List<Notif> {
        val out = mutableListOf<Notif>()
        sold.forEach { o ->
            when (o.status) {
                OrderStatus.AWAITING_PAYMENT, OrderStatus.INITIATED ->
                    out += Notif("→", "Someone is buying your ticket", "A buyer started an order for ₹${o.amount / 100}. They'll pay shortly.", rel(o.updatedAt), true, o.id)
                OrderStatus.PAID_OFFLINE, OrderStatus.CONTACT_SHARED ->
                    out += Notif("₹", "Payment received", "You've been paid ₹${o.amount / 100}. Share the ticket in chat, then mark it completed.", rel(o.updatedAt), true, o.id)
                OrderStatus.COMPLETED ->
                    out += Notif("★", "Sale completed", "Your sale of ₹${o.amount / 100} is done. Rate the buyer.", rel(o.updatedAt), false, o.id)
                else -> Unit
            }
        }
        bought.forEach { o ->
            when (o.status) {
                OrderStatus.PAID_OFFLINE, OrderStatus.CONTACT_SHARED ->
                    out += Notif("✓", "Payment recorded", "You paid ₹${o.amount / 100}. The seller will share the ticket.", rel(o.updatedAt), true, o.id)
                OrderStatus.COMPLETED ->
                    out += Notif("★", "Trip done", "Order for ₹${o.amount / 100} completed. Rate the seller.", rel(o.updatedAt), false, o.id)
                else -> Unit
            }
        }
        out.sortByDescending { it.at }
        out += Notif("✦", "Welcome to TicketResell", "Buy last-minute tickets or sell the ones you can't use. Payment is direct over UPI.", "", false, null)
        return out
    }

    private fun rel(millis: Long): String {
        val diff = System.currentTimeMillis() - millis
        val mins = diff / 60000
        return when {
            mins < 1 -> "now"
            mins < 60 -> "${mins}m"
            mins < 1440 -> "${mins / 60}h"
            else -> "${mins / 1440}d"
        }
    }
}

@Composable
fun NotificationsScreen(
    vm: NotificationsViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onOpenOrder: (String) -> Unit,
) {
    val g = LocalGlass.current
    val items by vm.items.collectAsState()

    AppBackground {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    com.ticketseller.app.ui.common.GlassIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
                    Spacer(Modifier.width(12.dp))
                    Text("Notifications", style = MaterialTheme.typography.titleMedium, color = g.ink, fontWeight = FontWeight.ExtraBold)
                }
            }

            val list = items
            if (list == null) {
                item { Text("Loading…", color = g.ink3, modifier = Modifier.padding(24.dp)) }
            } else {
                items(list, key = { it.title + it.at }) { n ->
                    GlassCard(onClick = { n.orderId?.let(onOpenOrder) }) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                            Box(Modifier.size(38.dp).clip(RoundedCornerShape(13.dp)).background(g.tint), contentAlignment = Alignment.Center) {
                                Text(n.glyph, style = MaterialTheme.typography.titleMedium, color = g.tintInk, fontWeight = FontWeight.ExtraBold)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(n.title, style = MaterialTheme.typography.titleSmall, color = g.ink, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    if (n.at.isNotBlank()) Text(n.at, style = MaterialTheme.typography.labelMedium, color = g.ink3)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(n.body, style = MaterialTheme.typography.bodySmall, color = g.ink2)
                            }
                            if (n.unread) {
                                Spacer(Modifier.width(8.dp))
                                Box(Modifier.padding(top = 4.dp).size(8.dp).clip(CircleShape).background(g.deep))
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.ticketseller.app.ui.screens.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ticketseller.app.data.model.Listing
import com.ticketseller.app.data.model.Order
import com.ticketseller.app.data.repo.AuthRepository
import com.ticketseller.app.data.repo.ListingRepository
import com.ticketseller.app.data.repo.OrderRepository
import com.ticketseller.app.ui.common.GlassCard
import com.ticketseller.app.ui.common.TintChip
import com.ticketseller.app.ui.theme.LocalGlass
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MyTicketsViewModel @Inject constructor(
    authRepository: AuthRepository,
    listingRepository: ListingRepository,
    orderRepository: OrderRepository,
) : ViewModel() {
    private val uid = MutableStateFlow(authRepository.effectiveUid().orEmpty())

    val myListings: StateFlow<List<Listing>?> = uid.flatMapLatest {
        if (it.isBlank()) flowOf(emptyList()) else listingRepository.observeBySeller(it)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val myPurchases: StateFlow<List<Order>?> = uid.flatMapLatest {
        if (it.isBlank()) flowOf(emptyList()) else orderRepository.observeMine(it, asBuyer = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val myIncoming: StateFlow<List<Order>?> = uid.flatMapLatest {
        if (it.isBlank()) flowOf(emptyList()) else orderRepository.observeMine(it, asBuyer = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun MyTicketsPane(
    vm: MyTicketsViewModel = hiltViewModel(),
    onListing: (String) -> Unit,
    onOrder: (String) -> Unit,
) {
    val g = LocalGlass.current
    var tab by remember { mutableStateOf(0) }
    val listings by vm.myListings.collectAsState()
    val incoming by vm.myIncoming.collectAsState()
    val purchases by vm.myPurchases.collectAsState()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { Text("My tickets", style = MaterialTheme.typography.headlineSmall, color = g.ink, fontWeight = FontWeight.ExtraBold) }
        item {
            Row(
                Modifier.clip(RoundedCornerShape(18.dp)).background(g.glass).border(1.dp, g.glassBorder, RoundedCornerShape(18.dp)).padding(5.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                SegTab("For sale", tab == 0, Modifier.weight(1f)) { tab = 0 }
                SegTab("Sold", tab == 1, Modifier.weight(1f)) { tab = 1 }
                SegTab("Bought", tab == 2, Modifier.weight(1f)) { tab = 2 }
            }
        }

        when (tab) {
            0 -> renderListings(listings, onListing)
            1 -> renderOrders(incoming, "You receive", "No sales yet", "When someone buys your ticket, it shows here.", onOrder)
            else -> renderOrders(purchases, "You pay", "No purchases yet", "Browse tickets and buy one to see it here.", onOrder)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.renderListings(listings: List<Listing>?, onListing: (String) -> Unit) {
    if (listings == null) { item { Loading() } ; return }
    if (listings.isEmpty()) { item { Empty("No listings yet", "Tap Sell on Home to list your first ticket.") } ; return }
    items(listings, key = { it.id }) { l ->
        GlassCard(onClick = { onListing(l.id) }) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${l.ticket.source} → ${l.ticket.destination}", style = MaterialTheme.typography.titleSmall, color = LocalGlassInk(), fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                    StatusPill(l.status.name.replace('_', ' '))
                }
                Spacer(Modifier.height(6.dp))
                SubText("${l.ticket.operator} · ₹${l.askingPrice / 100}")
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.renderOrders(orders: List<Order>?, amountLabel: String, emptyTitle: String, emptyBody: String, onOrder: (String) -> Unit) {
    if (orders == null) { item { Loading() } ; return }
    if (orders.isEmpty()) { item { Empty(emptyTitle, emptyBody) } ; return }
    items(orders, key = { it.id }) { o ->
        GlassCard(onClick = { onOrder(o.id) }) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Order ${o.id.take(6)}", style = MaterialTheme.typography.titleSmall, color = LocalGlassInk(), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    SubText("$amountLabel ₹${o.amount / 100}")
                }
                StatusPill(o.status.name.replace('_', ' '))
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = LocalGlassInk3(), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun SegTab(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val g = LocalGlass.current
    Box(
        modifier.clip(RoundedCornerShape(14.dp)).background(if (selected) g.ink else Color.Transparent).clickable { onClick() }.padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) g.onInk else g.ink2) }
}

@Composable
private fun StatusPill(text: String) = TintChip(text)

@Composable
private fun SubText(text: String) {
    val g = LocalGlass.current
    Text(text, style = MaterialTheme.typography.bodySmall, color = g.ink3)
}

@Composable
private fun Loading() {
    val g = LocalGlass.current
    Text("Loading…", color = g.ink3, modifier = Modifier.padding(24.dp))
}

@Composable
private fun Empty(title: String, body: String) {
    val g = LocalGlass.current
    Column(Modifier.fillMaxWidth().padding(top = 50.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = g.ink)
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodySmall, color = g.ink3, modifier = Modifier.padding(horizontal = 20.dp))
    }
}

@Composable private fun LocalGlassInk() = LocalGlass.current.ink
@Composable private fun LocalGlassInk3() = LocalGlass.current.ink3

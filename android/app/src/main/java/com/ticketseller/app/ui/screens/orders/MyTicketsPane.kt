package com.ticketseller.app.ui.screens.orders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ticketseller.app.data.model.Listing
import com.ticketseller.app.data.model.Order
import com.ticketseller.app.data.repo.AuthRepository
import com.ticketseller.app.data.repo.ListingRepository
import com.ticketseller.app.data.repo.OrderRepository
import com.ticketseller.app.ui.common.EmptyState
import com.ticketseller.app.ui.common.LoadingBox
import com.ticketseller.app.ui.common.Pill
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
}

@Composable
fun MyTicketsPane(
    vm: MyTicketsViewModel = hiltViewModel(),
    onListing: (String) -> Unit,
    onOrder: (String) -> Unit,
) {
    var tab by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("For sale") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Bought") })
        }
        if (tab == 0) {
            val listings by vm.myListings.collectAsState()
            when {
                listings == null -> LoadingBox()
                listings!!.isEmpty() -> EmptyState("No listings yet", "Tap “Sell ticket” to put your first ticket up.")
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
                    items(listings!!, key = { it.id }) {
                        ListItem(
                            headlineContent = { Text("${it.ticket.source} → ${it.ticket.destination}") },
                            supportingContent = { Text("${it.ticket.operator} · ₹${it.askingPrice / 100}") },
                            trailingContent = { Pill(it.status.name.replace('_', ' ')) },
                            modifier = Modifier.clickable { onListing(it.id) },
                        )
                        Divider()
                    }
                }
            }
        } else {
            val orders by vm.myPurchases.collectAsState()
            when {
                orders == null -> LoadingBox()
                orders!!.isEmpty() -> EmptyState("No purchases yet", "Browse tickets and start a chat with a seller.")
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
                    items(orders!!, key = { it.id }) {
                        ListItem(
                            headlineContent = { Text("Order ${it.id.take(6)}") },
                            supportingContent = { Text("₹${it.amount / 100}") },
                            trailingContent = { Pill(it.status.name.replace('_', ' ')) },
                            modifier = Modifier.clickable { onOrder(it.id) },
                        )
                        Divider()
                    }
                }
            }
        }
    }
}

package com.ticketseller.app.ui.screens.orders

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ticketseller.app.data.model.Order
import com.ticketseller.app.data.model.OrderStatus
import com.ticketseller.app.data.repo.OrderRepository
import com.ticketseller.app.ui.common.LoadingBox
import com.ticketseller.app.ui.common.Pill
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    state: SavedStateHandle,
    private val orderRepository: OrderRepository,
) : ViewModel() {
    private val id: String = state["id"] ?: ""

    val order: StateFlow<Order?> =
        orderRepository.observe(id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun markCompleted() = viewModelScope.launch { orderRepository.setStatus(id, OrderStatus.COMPLETED) }
    fun cancel() = viewModelScope.launch { orderRepository.setStatus(id, OrderStatus.CANCELLED) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: String,
    vm: OrderDetailViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onPay: () -> Unit,
) {
    val o by vm.order.collectAsState()
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Order") },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            },
        )
    }) { pad ->
        val order = o
        if (order == null) LoadingBox()
        else Column(
            Modifier.padding(pad).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Order ${order.id.take(8)}", style = MaterialTheme.typography.titleLarge)
            Pill(order.status.name.replace('_', ' '))
            Text("Amount: ₹${order.amount / 100}", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            when (order.status) {
                OrderStatus.AWAITING_PAYMENT, OrderStatus.INITIATED -> {
                    Button(onClick = onPay, modifier = Modifier.fillMaxWidth()) { Text("Pay (Coming soon)") }
                    OutlinedButton(onClick = vm::cancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel order") }
                }
                OrderStatus.PAID_OFFLINE, OrderStatus.CONTACT_SHARED -> {
                    Button(onClick = vm::markCompleted, modifier = Modifier.fillMaxWidth()) { Text("Mark as completed") }
                }
                else -> Unit
            }
        }
    }
}

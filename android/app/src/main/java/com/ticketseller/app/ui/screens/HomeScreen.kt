package com.ticketseller.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import com.ticketseller.app.data.api.ApiService
import com.ticketseller.app.data.api.ListingDto

@HiltViewModel
class HomeVm @Inject constructor(
  private val api: ApiService
) : ViewModel() {
  var listings by mutableStateOf(listOf<ListingDto>()); private set

  init {
    viewModelScope.launch {
      listings = try { api.listings() } catch (_: Exception) { emptyList() }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: HomeVm = hiltViewModel()) {
  Scaffold(topBar = { TopAppBar(title = { Text("Ticket Seller") }) }) { pad ->
    Column(Modifier.padding(pad).padding(16.dp)) {
      Text("Nearby Deals", style = MaterialTheme.typography.titleLarge)
      Spacer(Modifier.height(12.dp))
      LazyColumn {
        items(vm.listings) { item ->
          Card(Modifier.padding(bottom = 8.dp)) {
            ListItem(
              headlineContent = { Text(item.title) },
              supportingContent = { Text("₹${item.priceMinor / 100.0}") }
            )
          }
        }
      }
    }
  }
}

package com.ticketseller.app.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ticketseller.app.data.model.ChatThread
import com.ticketseller.app.data.repo.AuthRepository
import com.ticketseller.app.data.repo.ChatRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
) : ViewModel() {
    private val uid = MutableStateFlow(authRepository.effectiveUid().orEmpty())
    val myUid: String get() = uid.value

    val threads: StateFlow<List<ChatThread>?> = uid.flatMapLatest { id ->
        if (id.isBlank()) flowOf(emptyList())
        else combine(
            chatRepository.observeMyThreads(id, asBuyer = true),
            chatRepository.observeMyThreads(id, asBuyer = false),
        ) { a, b -> (a + b).distinctBy { it.id }.sortedByDescending { it.updatedAt } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun ChatListPane(
    vm: ChatListViewModel = hiltViewModel(),
    onChat: (String) -> Unit,
) {
    val g = LocalGlass.current
    val threads by vm.threads.collectAsState()
    val myUid = vm.myUid

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { Text("Chats", style = MaterialTheme.typography.headlineSmall, color = g.ink, fontWeight = FontWeight.ExtraBold) }

        val list = threads
        when {
            list == null -> item { Text("Loading…", color = g.ink3, modifier = Modifier.padding(24.dp)) }
            list.isEmpty() -> item {
                Column(Modifier.fillMaxWidth().padding(top = 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No chats yet", style = MaterialTheme.typography.titleMedium, color = g.ink)
                    Spacer(Modifier.height(6.dp))
                    Text("Open a ticket and tap Chat to talk to a seller.", style = MaterialTheme.typography.bodySmall, color = g.ink3)
                }
            }
            else -> items(list, key = { it.id }) { t ->
                val other = if (t.sellerId == myUid) t.buyerName else t.sellerName
                GlassCard(onClick = { onChat(t.id) }) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(Brush.linearGradient(listOf(g.brand, g.deep))), contentAlignment = Alignment.Center) {
                            Text(other.firstOrNull()?.uppercase() ?: "?", color = g.onBrandStrong, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(other.ifBlank { "Ticket chat" }, style = MaterialTheme.typography.titleSmall, color = g.ink, fontWeight = FontWeight.Bold)
                            Text(t.listingTitle.ifBlank { "Ticket" }, style = MaterialTheme.typography.labelMedium, color = g.accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(3.dp))
                            Text(t.lastMessage.ifBlank { "Say hi to break the ice" }, style = MaterialTheme.typography.bodySmall, color = g.ink3, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

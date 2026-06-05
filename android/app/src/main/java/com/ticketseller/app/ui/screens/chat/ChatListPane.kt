package com.ticketseller.app.ui.screens.chat

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
import com.ticketseller.app.data.model.ChatThread
import com.ticketseller.app.data.repo.AuthRepository
import com.ticketseller.app.data.repo.ChatRepository
import com.ticketseller.app.ui.common.EmptyState
import com.ticketseller.app.ui.common.LoadingBox
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
    val threads by vm.threads.collectAsState()
    when {
        threads == null -> LoadingBox()
        threads!!.isEmpty() -> EmptyState("No chats yet", "Start a chat from a listing to talk to a seller.")
        else -> LazyColumn(Modifier.fillMaxSize()) {
            items(threads!!, key = { it.id }) { thread ->
                ListItem(
                    headlineContent = { Text(thread.listingTitle.ifBlank { "Ticket chat" }) },
                    supportingContent = { Text(thread.lastMessage.ifBlank { "Say hi to break the ice" }, maxLines = 1) },
                    overlineContent = { Text("${thread.sellerName} ↔ ${thread.buyerName}") },
                    modifier = Modifier.clickable { onChat(thread.id) }.padding(horizontal = 4.dp),
                )
                Divider()
            }
        }
    }
}

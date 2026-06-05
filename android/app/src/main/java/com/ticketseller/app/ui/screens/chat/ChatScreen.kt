package com.ticketseller.app.ui.screens.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ticketseller.app.data.model.ChatMessage
import com.ticketseller.app.data.model.ChatMessageType
import com.ticketseller.app.data.model.ChatThread
import com.ticketseller.app.data.repo.AuthRepository
import com.ticketseller.app.data.repo.ChatRepository
import com.ticketseller.app.data.repo.UserRepository
import com.ticketseller.app.ui.common.LoadingBox
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    state: SavedStateHandle,
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    val threadId: String = state["threadId"] ?: ""
    val uid: String = authRepository.effectiveUid().orEmpty()

    val thread: StateFlow<ChatThread?> =
        chatRepository.observeThread(threadId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val messages: StateFlow<List<ChatMessage>?> =
        chatRepository.observeMessages(threadId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun send(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            chatRepository.send(threadId, uid, text)
        }
    }

    fun consentReveal() {
        val t = thread.value ?: return
        val asBuyer = uid == t.buyerId
        viewModelScope.launch {
            val me = userRepository.get(uid) ?: return@launch
            chatRepository.consentReveal(threadId, asBuyer, me.phone)
            chatRepository.send(
                threadId, uid,
                if (asBuyer) "Buyer agreed to share phone number"
                else "Seller agreed to share phone number",
                type = ChatMessageType.SYSTEM,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    threadId: String,
    vm: ChatViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val thread by vm.thread.collectAsState()
    val messages by vm.messages.collectAsState()
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages?.size) {
        val n = messages?.size ?: 0
        if (n > 0) listState.animateScrollToItem(n - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(thread?.listingTitle?.ifBlank { "Chat" } ?: "Chat")
                        thread?.let {
                            val other = if (vm.uid == it.buyerId) it.sellerName else it.buyerName
                            Text(other, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        placeholder = { Text("Type a message…") },
                        modifier = Modifier.weight(1f),
                        maxLines = 4,
                    )
                    Spacer(Modifier.width(6.dp))
                    IconButton(onClick = { vm.send(draft); draft = "" }) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            thread?.let { ContactCard(it, vm.uid, onConsent = vm::consentReveal) }
            val ms = messages
            if (ms == null) Box(Modifier.weight(1f).fillMaxWidth()) { LoadingBox() }
            else LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(ms, key = { it.id }) { Bubble(it, isMe = it.senderId == vm.uid) }
            }
        }
    }
}

@Composable
private fun ContactCard(thread: ChatThread, uid: String, onConsent: () -> Unit) {
    val asBuyer = uid == thread.buyerId
    val mine = if (asBuyer) thread.buyerConsentReveal else thread.sellerConsentReveal
    val theirs = if (asBuyer) thread.sellerConsentReveal else thread.buyerConsentReveal
    val bothAgreed = thread.buyerConsentReveal && thread.sellerConsentReveal

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                if (bothAgreed) "Contacts shared 🎉" else "Share contact details when you both agree",
                fontWeight = FontWeight.SemiBold,
            )
            if (bothAgreed) {
                Spacer(Modifier.height(6.dp))
                val phone = if (asBuyer) thread.sellerPhone else thread.buyerPhone
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Phone, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.width(6.dp))
                    Text(if (phone.isNotBlank()) phone else "Contact will appear once the other side adds their phone in profile.")
                }
            } else {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onConsent, enabled = !mine) {
                        Text(if (mine) "You agreed" else "I agree to share my number")
                    }
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(if (theirs) "Other side: agreed" else "Other side: waiting") },
                    )
                }
                Text(
                    "Make sure your phone is set in Profile so it can be shared.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun Bubble(m: ChatMessage, isMe: Boolean) {
    val align = if (isMe) Alignment.End else Alignment.Start
    val color = when {
        m.type == ChatMessageType.SYSTEM -> MaterialTheme.colorScheme.surfaceVariant
        isMe -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val onColor = if (isMe && m.type != ChatMessageType.SYSTEM) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurface

    Column(Modifier.fillMaxWidth(), horizontalAlignment = align) {
        Surface(
            color = color,
            shape = RoundedCornerShape(
                topStart = 14.dp, topEnd = 14.dp,
                bottomStart = if (isMe) 14.dp else 2.dp,
                bottomEnd = if (isMe) 2.dp else 14.dp,
            ),
            modifier = Modifier.padding(horizontal = 2.dp).clip(RoundedCornerShape(14.dp)),
        ) {
            Text(
                m.text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                color = onColor,
            )
        }
    }
}

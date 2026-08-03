package com.ticketseller.app.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
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
import com.ticketseller.app.ui.common.AppBackground
import com.ticketseller.app.ui.common.GlassIconButton
import com.ticketseller.app.ui.common.LoadingBox
import com.ticketseller.app.ui.common.glassBox
import com.ticketseller.app.ui.theme.LocalGlass
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
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

    // Optimistic: flips true the moment you tap, so the UI updates instantly even before realtime.
    private val _consentGiven = MutableStateFlow(false)
    val consentGiven: StateFlow<Boolean> = _consentGiven

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast
    fun clearToast() { _toast.value = null }

    fun send(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch { runCatching { chatRepository.send(threadId, uid, text) } }
    }

    fun consentReveal() {
        val t = thread.value ?: return
        val asBuyer = uid == t.buyerId
        _consentGiven.value = true
        viewModelScope.launch {
            val phone = runCatching { userRepository.get(uid)?.phone }.getOrNull().orEmpty()
            if (phone.isBlank()) _toast.value = "Add your phone number in Profile so it can be shared."
            runCatching {
                chatRepository.consentReveal(threadId, asBuyer, phone)
                chatRepository.send(
                    threadId, uid,
                    if (asBuyer) "Buyer agreed to share phone number" else "Seller agreed to share phone number",
                    type = ChatMessageType.SYSTEM,
                )
            }
        }
    }
}

@Composable
fun ChatScreen(
    threadId: String,
    vm: ChatViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val g = LocalGlass.current
    val thread by vm.thread.collectAsState()
    val messages by vm.messages.collectAsState()
    val consentGiven by vm.consentGiven.collectAsState()
    val toast by vm.toast.collectAsState()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages?.size) {
        val n = messages?.size ?: 0
        if (n > 0) listState.animateScrollToItem(n - 1)
    }
    LaunchedEffect(toast) {
        toast?.let { android.widget.Toast.makeText(ctx, it, android.widget.Toast.LENGTH_LONG).show(); vm.clearToast() }
    }

    AppBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            // Header
            Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(thread?.listingTitle?.ifBlank { "Chat" } ?: "Chat", style = MaterialTheme.typography.titleMedium, color = g.ink, fontWeight = FontWeight.ExtraBold)
                    thread?.let {
                        val other = if (vm.uid == it.buyerId) it.sellerName else it.buyerName
                        Text(other.ifBlank { "Ticket chat" }, style = MaterialTheme.typography.labelMedium, color = g.ink3)
                    }
                }
            }

            thread?.let { ContactCard(it, vm.uid, myConsent = consentGiven, onConsent = vm::consentReveal) }

            val ms = messages
            if (ms == null) {
                Box(Modifier.weight(1f).fillMaxWidth()) { LoadingBox() }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(ms, key = { it.id }) { Bubble(it, isMe = it.senderId == vm.uid) }
                }
            }

            // Input bar — rises above the keyboard (IME) and clears the nav bar.
            Row(
                Modifier.fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier.weight(1f).glassBox(RoundedCornerShape(22.dp), strong = true).padding(horizontal = 16.dp, vertical = 13.dp),
                ) {
                    if (draft.isEmpty()) Text("Type a message…", style = MaterialTheme.typography.bodyLarge, color = g.ink3)
                    BasicTextField(
                        value = draft, onValueChange = { draft = it },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = g.ink),
                        cursorBrush = SolidColor(g.deep),
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(Brush.linearGradient(listOf(g.brand, g.deep)))
                        .clickable { vm.send(draft); draft = "" },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = g.onBrandStrong, modifier = Modifier.size(20.dp)) }
            }
        }
    }
}

@Composable
private fun ContactCard(thread: ChatThread, uid: String, myConsent: Boolean, onConsent: () -> Unit) {
    val g = LocalGlass.current
    val asBuyer = uid == thread.buyerId
    val mine = (if (asBuyer) thread.buyerConsentReveal else thread.sellerConsentReveal) || myConsent
    val theirs = if (asBuyer) thread.sellerConsentReveal else thread.buyerConsentReveal
    val bothAgreed = thread.buyerConsentReveal && thread.sellerConsentReveal

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(20.dp)).background(g.tint).padding(14.dp)) {
        Text(
            if (bothAgreed) "Contacts shared 🎉" else "Share phone number when you both agree",
            style = MaterialTheme.typography.titleSmall, color = g.tintInk, fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(8.dp))
        if (bothAgreed) {
            val phone = if (asBuyer) thread.sellerPhone else thread.buyerPhone
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Phone, null, tint = g.tintInk, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(phone.ifBlank { "Will appear once the other side adds their phone in Profile." }, style = MaterialTheme.typography.bodyMedium, color = g.ink2)
            }
        } else {
            Box(
                Modifier.clip(RoundedCornerShape(14.dp))
                    .background(if (mine) g.glass else g.deep)
                    .clickable(enabled = !mine) { onConsent() }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(if (mine) "You agreed ✓" else "I agree to share my number", style = MaterialTheme.typography.labelLarge, color = if (mine) g.ink2 else g.onDeep)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                (if (theirs) "Other side agreed. " else "Waiting for the other side. ") + "Nothing is shared until you both tap it. Set your phone in Profile first.",
                style = MaterialTheme.typography.labelMedium, color = g.ink3,
            )
        }
    }
}

@Composable
private fun Bubble(m: ChatMessage, isMe: Boolean) {
    val g = LocalGlass.current
    if (m.type == ChatMessageType.SYSTEM) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(m.text, style = MaterialTheme.typography.labelMedium, color = g.ink3, modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(g.glass).padding(horizontal = 12.dp, vertical = 6.dp))
        }
        return
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
        Box(
            Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp, topEnd = 18.dp,
                        bottomStart = if (isMe) 18.dp else 6.dp,
                        bottomEnd = if (isMe) 6.dp else 18.dp,
                    ),
                )
                .then(if (isMe) Modifier.background(Brush.linearGradient(listOf(g.brand, g.deep))) else Modifier.background(g.glassStrong).border(1.dp, g.glassBorder, RoundedCornerShape(18.dp)))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(m.text, color = if (isMe) g.onBrandStrong else g.ink, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

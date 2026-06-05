package com.ticketseller.app.data.repo

import com.ticketseller.app.data.model.ChatMessage
import com.ticketseller.app.data.model.ChatMessageType
import com.ticketseller.app.data.model.ChatThread
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val supabase: SupabaseClient,
) {
    fun threadId(listingId: String, buyerId: String) = "${listingId}_$buyerId"

    suspend fun openThread(thread: ChatThread): ChatThread {
        val id = thread.id.ifBlank { threadId(thread.listingId, thread.buyerId) }
        val withId = thread.copy(id = id)
        // Insert only if the thread doesn't already exist — keep prior consent
        // flags & last-message untouched on re-open.
        val existing = getThread(id)
        if (existing == null) {
            runCatching { supabase.from("chat_threads").insert(withId) }
        }
        return existing ?: withId
    }

    private suspend fun getThread(threadId: String): ChatThread? = runCatching {
        supabase.from("chat_threads").select {
            filter { eq("id", threadId) }; limit(1)
        }.decodeList<ChatThread>().firstOrNull()
    }.getOrNull()

    fun observeThread(threadId: String): Flow<ChatThread?> = callbackFlow {
        trySend(getThread(threadId))
        val channel = supabase.channel("thread-$threadId")
        val changes = channel.postgresChangeFlow<PostgresAction>("public") { table = "chat_threads" }
        val job = launch { changes.onEach { trySend(getThread(threadId)) }.collect { } }
        channel.subscribe()
        awaitClose {
            job.cancel()
            launch { supabase.realtime.removeChannel(channel) }
        }
    }

    private suspend fun fetchMessages(threadId: String): List<ChatMessage> = runCatching {
        supabase.from("chat_messages").select {
            filter { eq("thread_id", threadId) }
            limit(500)
        }.decodeList<ChatMessage>().sortedBy { it.sentAt }
    }.getOrDefault(emptyList())

    fun observeMessages(threadId: String): Flow<List<ChatMessage>> = callbackFlow {
        trySend(fetchMessages(threadId))
        val channel = supabase.channel("messages-$threadId")
        val changes = channel.postgresChangeFlow<PostgresAction>("public") { table = "chat_messages" }
        val job = launch { changes.onEach { trySend(fetchMessages(threadId)) }.collect { } }
        channel.subscribe()
        awaitClose {
            job.cancel()
            launch { supabase.realtime.removeChannel(channel) }
        }
    }

    private suspend fun fetchMyThreads(uid: String, asBuyer: Boolean): List<ChatThread> = runCatching {
        val field = if (asBuyer) "buyer_id" else "seller_id"
        supabase.from("chat_threads").select {
            filter { eq(field, uid) }
        }.decodeList<ChatThread>().sortedByDescending { it.updatedAt }
    }.getOrDefault(emptyList())

    fun observeMyThreads(uid: String, asBuyer: Boolean): Flow<List<ChatThread>> = callbackFlow {
        trySend(fetchMyThreads(uid, asBuyer))
        val channel = supabase.channel("my-threads-$uid-$asBuyer")
        val changes = channel.postgresChangeFlow<PostgresAction>("public") { table = "chat_threads" }
        val job = launch { changes.onEach { trySend(fetchMyThreads(uid, asBuyer)) }.collect { } }
        channel.subscribe()
        awaitClose {
            job.cancel()
            launch { supabase.realtime.removeChannel(channel) }
        }
    }

    suspend fun send(
        threadId: String,
        senderId: String,
        text: String,
        type: ChatMessageType = ChatMessageType.TEXT,
    ) {
        val m = ChatMessage(
            id = UUID.randomUUID().toString(),
            threadId = threadId,
            senderId = senderId,
            text = text,
            type = type,
        )
        supabase.from("chat_messages").insert(m)
        supabase.from("chat_threads").update(
            mapOf(
                "last_message" to text,
                "last_sender_id" to senderId,
                "updated_at" to System.currentTimeMillis(),
            )
        ) { filter { eq("id", threadId) } }
    }

    suspend fun consentReveal(threadId: String, asBuyer: Boolean, myPhone: String) {
        val consentField = if (asBuyer) "buyer_consent_reveal" else "seller_consent_reveal"
        val phoneField = if (asBuyer) "buyer_phone" else "seller_phone"
        supabase.from("chat_threads").update(
            mapOf(
                consentField to true,
                phoneField to myPhone,
                "updated_at" to System.currentTimeMillis(),
            )
        ) { filter { eq("id", threadId) } }
    }
}

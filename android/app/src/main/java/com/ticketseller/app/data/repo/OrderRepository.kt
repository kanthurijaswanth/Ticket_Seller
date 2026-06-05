package com.ticketseller.app.data.repo

import com.ticketseller.app.data.model.Order
import com.ticketseller.app.data.model.OrderStatus
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
class OrderRepository @Inject constructor(
    private val supabase: SupabaseClient,
) {
    private val table = "orders"

    suspend fun create(order: Order): String {
        val id = order.id.ifBlank { UUID.randomUUID().toString() }
        val withId = order.copy(id = id)
        supabase.from(table).insert(withId)
        return id
    }

    suspend fun get(id: String): Order? = runCatching {
        supabase.from(table).select { filter { eq("id", id) }; limit(1) }
            .decodeList<Order>().firstOrNull()
    }.getOrNull()

    fun observe(id: String): Flow<Order?> = callbackFlow {
        trySend(get(id))
        val channel = supabase.channel("order-$id")
        val changes = channel.postgresChangeFlow<PostgresAction>("public") { table = "orders" }
        val job = launch { changes.onEach { trySend(get(id)) }.collect { } }
        channel.subscribe()
        awaitClose {
            job.cancel()
            launch { supabase.realtime.removeChannel(channel) }
        }
    }

    suspend fun setStatus(id: String, status: OrderStatus) {
        supabase.from(table).update(
            mapOf("status" to status.name, "updated_at" to System.currentTimeMillis())
        ) { filter { eq("id", id) } }
    }

    private suspend fun fetchMine(uid: String, asBuyer: Boolean): List<Order> = runCatching {
        val field = if (asBuyer) "buyer_id" else "seller_id"
        supabase.from(table).select {
            filter { eq(field, uid) }
        }.decodeList<Order>().sortedByDescending { it.updatedAt }
    }.getOrDefault(emptyList())

    fun observeMine(uid: String, asBuyer: Boolean): Flow<List<Order>> = callbackFlow {
        trySend(fetchMine(uid, asBuyer))
        val channel = supabase.channel("my-orders-$uid-$asBuyer")
        val changes = channel.postgresChangeFlow<PostgresAction>("public") { table = "orders" }
        val job = launch { changes.onEach { trySend(fetchMine(uid, asBuyer)) }.collect { } }
        channel.subscribe()
        awaitClose {
            job.cancel()
            launch { supabase.realtime.removeChannel(channel) }
        }
    }
}

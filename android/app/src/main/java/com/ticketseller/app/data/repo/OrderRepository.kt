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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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
        // Insert explicit columns only — omit payment_ref so an order can be created even
        // before that column is migrated into the DB.
        supabase.from(table).insert(
            buildJsonObject {
                put("id", id)
                put("listing_id", order.listingId)
                put("buyer_id", order.buyerId)
                put("seller_id", order.sellerId)
                put("amount", order.amount)
                put("status", order.status.name)
                put("created_at", order.createdAt)
                put("updated_at", order.updatedAt)
            },
        )
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

    /** Mark an order paid and stash the UPI reference. Falls back to status-only if the
     *  payment_ref column hasn't been migrated in yet. */
    suspend fun markPaid(id: String, paymentRef: String) {
        val now = System.currentTimeMillis()
        runCatching {
            supabase.from(table).update(
                mapOf(
                    "status" to OrderStatus.PAID_OFFLINE.name,
                    "payment_ref" to paymentRef,
                    "updated_at" to now,
                )
            ) { filter { eq("id", id) } }
        }.onFailure {
            supabase.from(table).update(
                mapOf("status" to OrderStatus.PAID_OFFLINE.name, "updated_at" to now)
            ) { filter { eq("id", id) } }
        }
    }

    /** Is there already a live (non-cancelled) order for this listing? Prevents double-buying. */
    suspend fun activeOrderForListing(listingId: String): Order? = runCatching {
        supabase.from(table).select { filter { eq("listing_id", listingId) } }
            .decodeList<Order>()
            .filter { it.status != OrderStatus.CANCELLED }
            .maxByOrNull { it.updatedAt }
    }.getOrNull()

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

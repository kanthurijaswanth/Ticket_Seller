package com.ticketseller.app.data.repo

import com.ticketseller.app.data.model.Listing
import com.ticketseller.app.data.model.ListingStatus
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
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
class ListingRepository @Inject constructor(
    private val supabase: SupabaseClient,
) {
    private val table = "listings"

    // Everything except the heavy base64 image — used for list screens so they load fast.
    private val listColumns = Columns.list(
        "id", "seller_id", "seller_name", "seller_rating", "ticket", "address",
        "asking_price", "ticket_image_hash", "verification_notes", "verification_flags",
        "status", "created_at", "updated_at",
    )

    suspend fun create(listing: Listing): String {
        val id = listing.id.ifBlank { UUID.randomUUID().toString() }
        val withId = listing.copy(id = id)
        supabase.from(table).insert(withId)
        return id
    }

    suspend fun update(id: String, fields: Map<String, Any?>) {
        supabase.from(table).update(fields.mapValues { it.value ?: "" }) {
            filter { eq("id", id) }
        }
    }

    /** Convenience for lifecycle transitions (RESERVED / SOLD / ACTIVE …). */
    suspend fun setStatus(id: String, status: ListingStatus) {
        supabase.from(table).update(
            mapOf("status" to status.name, "updated_at" to System.currentTimeMillis())
        ) { filter { eq("id", id) } }
    }

    suspend fun get(id: String): Listing? = runCatching {
        supabase.from(table).select { filter { eq("id", id) }; limit(1) }
            .decodeList<Listing>().firstOrNull()
    }.getOrNull()

    fun observe(id: String): Flow<Listing?> = callbackFlow {
        trySend(get(id))
        val channel = supabase.channel("listing-$id")
        val changes = channel.postgresChangeFlow<PostgresAction>("public") { table = "listings" }
        val job = launch { changes.onEach { trySend(get(id)) }.collect { } }
        channel.subscribe()
        awaitClose {
            job.cancel()
            launch { supabase.realtime.removeChannel(channel) }
        }
    }

    private suspend fun fetchActive(): List<Listing> = runCatching {
        supabase.from(table).select(listColumns) {
            filter { eq("status", ListingStatus.ACTIVE.name) }
            limit(100)
        }.decodeList<Listing>().sortedBy { it.ticket.journeyDateMillis }
    }.getOrDefault(emptyList())

    fun observeActive(): Flow<List<Listing>> = callbackFlow {
        trySend(fetchActive())
        val channel = supabase.channel("listings-active")
        val changes = channel.postgresChangeFlow<PostgresAction>("public") { table = "listings" }
        val job = launch { changes.onEach { trySend(fetchActive()) }.collect { } }
        channel.subscribe()
        awaitClose {
            job.cancel()
            launch { supabase.realtime.removeChannel(channel) }
        }
    }

    private suspend fun fetchBySeller(sellerId: String): List<Listing> = runCatching {
        supabase.from(table).select(listColumns) {
            filter { eq("seller_id", sellerId) }
        }.decodeList<Listing>().sortedByDescending { it.createdAt }
    }.getOrDefault(emptyList())

    fun observeBySeller(sellerId: String): Flow<List<Listing>> = callbackFlow {
        trySend(fetchBySeller(sellerId))
        val channel = supabase.channel("listings-seller-$sellerId")
        val changes = channel.postgresChangeFlow<PostgresAction>("public") { table = "listings" }
        val job = launch { changes.onEach { trySend(fetchBySeller(sellerId)) }.collect { } }
        channel.subscribe()
        awaitClose {
            job.cancel()
            launch { supabase.realtime.removeChannel(channel) }
        }
    }

    suspend fun anyWithImageHash(hash: String): Boolean {
        if (hash.isBlank()) return false
        return runCatching {
            supabase.from(table).select {
                filter { eq("ticket_image_hash", hash) }
                limit(1)
            }.decodeList<Listing>().isNotEmpty()
        }.getOrDefault(false)
    }
}

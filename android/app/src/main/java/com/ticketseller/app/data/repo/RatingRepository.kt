package com.ticketseller.app.data.repo

import com.ticketseller.app.data.model.Rating
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ratings left after a completed order. Each side (buyer / seller) can rate the
 * other once per order. Submitting a rating also recomputes the ratee's average
 * and count on their user row so it shows up everywhere their profile appears.
 */
@Singleton
class RatingRepository @Inject constructor(
    private val supabase: SupabaseClient,
) {
    private val table = "ratings"

    /** True if [raterId] has already rated [orderId] — used to hide the form after rating. */
    suspend fun hasRated(orderId: String, raterId: String): Boolean = runCatching {
        supabase.from(table).select {
            filter { eq("order_id", orderId); eq("rater_id", raterId) }
            limit(1)
        }.decodeList<Rating>().isNotEmpty()
    }.getOrDefault(false)

    /** Insert a rating, then recompute the ratee's average + count. */
    suspend fun submit(rating: Rating) {
        val id = rating.id.ifBlank { UUID.randomUUID().toString() }
        supabase.from(table).insert(rating.copy(id = id))
        recompute(rating.rateeId)
    }

    suspend fun ratingsFor(rateeId: String): List<Rating> = runCatching {
        supabase.from(table).select { filter { eq("ratee_id", rateeId) } }
            .decodeList<Rating>()
            .sortedByDescending { it.createdAt }
    }.getOrDefault(emptyList())

    private suspend fun recompute(rateeId: String) {
        val all = runCatching {
            supabase.from(table).select { filter { eq("ratee_id", rateeId) } }
                .decodeList<Rating>()
        }.getOrDefault(emptyList())
        if (all.isEmpty()) return
        val count = all.size
        val avg = all.sumOf { it.stars.toDouble() } / count
        runCatching {
            supabase.from("users").update(
                mapOf("rating_avg" to avg, "rating_count" to count)
            ) { filter { eq("uid", rateeId) } }
        }
    }
}

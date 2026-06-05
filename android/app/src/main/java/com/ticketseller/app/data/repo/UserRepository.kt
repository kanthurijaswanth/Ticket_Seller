package com.ticketseller.app.data.repo

import com.ticketseller.app.data.model.User
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val supabase: SupabaseClient,
) {
    suspend fun get(uid: String): User? = runCatching {
        supabase.from("users").select { filter { eq("uid", uid) }; limit(1) }
            .decodeList<User>().firstOrNull()
    }.getOrNull()

    /** Live user row — emits initial value, then re-fetches on any /users change. */
    fun observe(uid: String): Flow<User?> = callbackFlow {
        trySend(get(uid))
        val channel = supabase.channel("users-$uid")
        val changeFlow = channel.postgresChangeFlow<PostgresAction>("public") { table = "users" }
        val job = launch {
            changeFlow.onEach { trySend(get(uid)) }.collect { }
        }
        channel.subscribe()
        awaitClose {
            job.cancel()
            launch { supabase.realtime.removeChannel(channel) }
        }
    }

    suspend fun update(uid: String, fields: Map<String, Any?>) {
        supabase.from("users").update(fields.mapValues { it.value ?: "" }) {
            filter { eq("uid", uid) }
        }
    }
}

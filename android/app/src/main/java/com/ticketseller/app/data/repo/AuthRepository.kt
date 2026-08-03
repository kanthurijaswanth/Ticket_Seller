package com.ticketseller.app.data.repo

import android.content.Context
import com.ticketseller.app.data.model.User
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Auth wrapper on Supabase. Supports two modes:
 *   1. Real Supabase auth (email/password)
 *   2. Local dev bypass — a UUID stamped in SharedPreferences. Lets the rest of
 *      the app run without going through Supabase auth. Remove the bypass surface
 *      before release.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val supabase: SupabaseClient,
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("ts_session", Context.MODE_PRIVATE)
    private val devUidFlow = MutableStateFlow(prefs.getString(KEY_DEV_UID, null))

    /** Best-effort current user id: real Supabase UID > dev bypass UID > null. */
    fun effectiveUid(): String? = supabase.auth.currentUserOrNull()?.id ?: devUidFlow.value

    /** Streams the effective UID — emits whenever auth state or dev bypass changes. */
    fun effectiveUidFlow(): Flow<String?> {
        val authFlow = supabase.auth.sessionStatus.map { _ ->
            supabase.auth.currentUserOrNull()?.id
        }
        return combine(authFlow, devUidFlow) { authUid, devUid -> authUid ?: devUid }
    }

    suspend fun signUpEmail(email: String, password: String, name: String): String {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        // If the Supabase project requires email confirmation, no session is created
        // until the user clicks the link — that's a success state, not a failure.
        val uid = supabase.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("CONFIRM_EMAIL")
        ensureUserDoc(uid, email, name, "")
        return uid
    }

    /** Send a password-reset email. */
    suspend fun sendPasswordReset(email: String) {
        supabase.auth.resetPasswordForEmail(email)
    }

    suspend fun signInEmail(email: String, password: String): String {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        val uid = supabase.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("Sign-in succeeded but no session returned.")
        ensureUserDoc(uid, email, "", "")
        return uid
    }

    /**
     * DEV ONLY. Skips Supabase auth and signs in as a ready-made demo account with a
     * filled-out profile, so the app looks populated for testing/demos.
     */
    suspend fun devBypassSignIn(): String {
        val uid = DEMO_UID
        prefs.edit().putString(KEY_DEV_UID, uid).apply()
        devUidFlow.value = uid
        runCatching { ensureDemoUser(uid) }
        return uid
    }

    private suspend fun ensureDemoUser(uid: String) {
        val existing = runCatching {
            supabase.from("users").select(Columns.list("uid")) {
                filter { eq("uid", uid) }; limit(1)
            }.decodeList<UidOnly>()
        }.getOrDefault(emptyList())
        if (existing.isEmpty()) {
            runCatching {
                supabase.from("users").insert(
                    buildJsonObject {
                        put("uid", uid)
                        put("email", "demo@ticketresell.app")
                        put("name", "Aditya Sharma")
                        put("phone", "9876543210")
                        put("photo_url", "")
                        put("rating_avg", 4.8)
                        put("rating_count", 23)
                    },
                )
            }
        }
    }

    suspend fun signOut() {
        runCatching { supabase.auth.signOut() }
        prefs.edit().remove(KEY_DEV_UID).apply()
        devUidFlow.value = null
    }

    private suspend fun ensureUserDoc(uid: String, email: String, name: String, photoUrl: String) {
        val existing = runCatching {
            supabase.from("users").select(Columns.list("uid")) {
                filter { eq("uid", uid) }
                limit(1)
            }.decodeList<UidOnly>()
        }.getOrDefault(emptyList())

        if (existing.isEmpty()) {
            // Explicit columns (omit upi_id) so the row is created even before that column
            // is migrated in.
            runCatching {
                supabase.from("users").insert(
                    buildJsonObject {
                        put("uid", uid)
                        put("email", email)
                        put("name", name.ifBlank { email.substringBefore('@') })
                        put("phone", "")
                        put("photo_url", photoUrl)
                        put("rating_avg", 0.0)
                        put("rating_count", 0)
                    },
                )
            }
        }
    }

    @kotlinx.serialization.Serializable
    private data class UidOnly(val uid: String)

    companion object {
        private const val KEY_DEV_UID = "dev_bypass_uid"
        private const val DEMO_UID = "demo_user_01"
    }
}

package com.ticketseller.app.data

/**
 * Credentials for the project's Supabase backend.
 *
 *   Supabase Dashboard → Project Settings → API
 *     • Project URL                       ⇒ SUPABASE_URL
 *     • Project API keys → "anon public"  ⇒ SUPABASE_ANON_KEY
 *
 * The anon key is safe to ship in the app — it only grants the row-level-security
 * policies you configure on the database.
 */
object SupabaseConfig {
    const val SUPABASE_URL = "https://cqfxkqrvlgopnfbxlpfl.supabase.co"
    const val SUPABASE_ANON_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImNxZnhrcXJ2bGdvcG5mYnhscGZsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzkyNzQ2NDcsImV4cCI6MjA5NDg1MDY0N30.ai5QO0SvMAbvuvkGQoItdm638TTgWUSPoDnitKBns-E"
}

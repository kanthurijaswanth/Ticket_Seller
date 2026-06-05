package com.ticketseller.app.di

import com.ticketseller.app.data.SupabaseConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import javax.inject.Singleton

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient = createSupabaseClient(
        supabaseUrl = SupabaseConfig.SUPABASE_URL,
        supabaseKey = SupabaseConfig.SUPABASE_ANON_KEY,
    ) {
        defaultSerializer = KotlinXSerializer(
            Json {
                namingStrategy = JsonNamingStrategy.SnakeCase
                ignoreUnknownKeys = true
                encodeDefaults = true
                explicitNulls = false
            }
        )
        install(Auth)
        install(Postgrest)
        install(Realtime)
        install(Storage)
    }
}

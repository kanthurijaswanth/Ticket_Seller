package com.ticketseller.app.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.ticketseller.app.BuildConfig
import com.ticketseller.app.data.api.ApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

  @Provides @Singleton
  fun okHttp(): OkHttpClient =
    OkHttpClient.Builder()
      .addInterceptor(HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
      })
      .build()

  @Provides @Singleton
  fun moshi(): Moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

  @Provides @Singleton
  fun retrofit(client: OkHttpClient, moshi: Moshi): Retrofit =
    Retrofit.Builder()
      .baseUrl(BuildConfig.BASE_URL)
      .client(client)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()

  @Provides @Singleton
  fun api(retrofit: Retrofit): ApiService = retrofit.create(ApiService::class.java)
}

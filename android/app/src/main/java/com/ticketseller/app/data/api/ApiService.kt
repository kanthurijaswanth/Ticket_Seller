package com.ticketseller.app.data.api

import retrofit2.http.GET

data class Health(val status: String)
data class ListingDto(val id: String, val title: String, val priceMinor: Long)

interface ApiService {
  @GET("health") suspend fun health(): Health
  @GET("listings") suspend fun listings(): List<ListingDto>
}

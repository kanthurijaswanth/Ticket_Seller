package com.ticketseller.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ListingEntity(
  @PrimaryKey val id: String,
  val title: String,
  val priceMinor: Long
)

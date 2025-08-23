package com.ticketseller.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ListingDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(items: List<ListingEntity>)

  @Query("SELECT * FROM ListingEntity")
  suspend fun all(): List<ListingEntity>
}

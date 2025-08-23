package com.ticketseller.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ListingEntity::class], version = 1)
abstract class AppDb : RoomDatabase() {
  abstract fun listingDao(): ListingDao
}

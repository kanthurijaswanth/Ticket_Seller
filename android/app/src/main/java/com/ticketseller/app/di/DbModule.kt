package com.ticketseller.app.di

import android.content.Context
import androidx.room.Room
import com.ticketseller.app.data.db.AppDb
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DbModule {
  @Provides @Singleton
  fun db(@ApplicationContext ctx: Context): AppDb =
    Room.databaseBuilder(ctx, AppDb::class.java, "ticket-seller.db").build()
}

package com.ticketseller.app.data.repo

import com.ticketseller.app.data.api.ApiService
import com.ticketseller.app.data.db.AppDb
import com.ticketseller.app.data.db.ListingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ListingRepository @Inject constructor(
  private val api: ApiService,
  private val db: AppDb
){
  suspend fun refresh() = withContext(Dispatchers.IO) {
    val remote = api.listings()
    db.listingDao().upsertAll(remote.map { ListingEntity(it.id, it.title, it.priceMinor) })
  }

  suspend fun local() = withContext(Dispatchers.IO) { db.listingDao().all() }
}

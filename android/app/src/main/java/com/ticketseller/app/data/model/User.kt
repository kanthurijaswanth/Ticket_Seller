package com.ticketseller.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val phone: String = "",
    val upiId: String = "",
    val photoUrl: String = "",
    val ratingAvg: Double = 0.0,
    val ratingCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

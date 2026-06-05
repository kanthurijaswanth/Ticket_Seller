package com.ticketseller.app.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class OrderStatus {
    INITIATED,
    CONTACT_SHARED,
    AWAITING_PAYMENT,
    PAID_OFFLINE,
    COMPLETED,
    CANCELLED,
    DISPUTED,
}

@Serializable
data class Order(
    val id: String = "",
    val listingId: String = "",
    val buyerId: String = "",
    val sellerId: String = "",
    val amount: Long = 0L,
    val status: OrderStatus = OrderStatus.INITIATED,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Serializable
data class Rating(
    val id: String = "",
    val orderId: String = "",
    val raterId: String = "",
    val rateeId: String = "",
    val stars: Int = 5,
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

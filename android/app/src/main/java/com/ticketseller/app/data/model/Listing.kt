package com.ticketseller.app.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class ListingStatus { PENDING_REVIEW, ACTIVE, RESERVED, SOLD, REJECTED, EXPIRED }

@Serializable
data class PickupDropAddress(
    val pickupLine1: String = "",
    val pickupCity: String = "",
    val pickupPincode: String = "",
    val pickupLandmark: String = "",
    val dropLine1: String = "",
    val dropCity: String = "",
    val dropPincode: String = "",
    val notes: String = "",
)

@Serializable
data class Listing(
    val id: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val sellerRating: Double = 0.0,
    val ticket: TicketDetails = TicketDetails(),
    val address: PickupDropAddress = PickupDropAddress(),
    val askingPrice: Long = 0L,
    val ticketImageBase64: String = "",
    val ticketImageHash: String = "",
    val verificationNotes: String = "",
    val verificationFlags: List<String> = emptyList(),
    val status: ListingStatus = ListingStatus.PENDING_REVIEW,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

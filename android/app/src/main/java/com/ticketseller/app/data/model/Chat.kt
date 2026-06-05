package com.ticketseller.app.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class ChatMessageType { TEXT, SYSTEM, CONTACT_REVEAL_REQUEST, CONTACT_REVEALED }

@Serializable
data class ChatMessage(
    val id: String = "",
    val threadId: String = "",
    val senderId: String = "",
    val text: String = "",
    val type: ChatMessageType = ChatMessageType.TEXT,
    val sentAt: Long = System.currentTimeMillis(),
)

@Serializable
data class ChatThread(
    val id: String = "",
    val listingId: String = "",
    val listingTitle: String = "",
    val buyerId: String = "",
    val sellerId: String = "",
    val buyerName: String = "",
    val sellerName: String = "",
    val lastMessage: String = "",
    val lastSenderId: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val buyerConsentReveal: Boolean = false,
    val sellerConsentReveal: Boolean = false,
    val buyerPhone: String = "",
    val sellerPhone: String = "",
)

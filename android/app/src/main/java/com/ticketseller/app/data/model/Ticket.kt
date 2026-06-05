package com.ticketseller.app.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class TicketType { BUS, TRAIN }

@Serializable
enum class Berth { LOWER, MIDDLE, UPPER, SIDE_LOWER, SIDE_UPPER, UNSPECIFIED }

@Serializable
data class TicketDetails(
    val type: TicketType = TicketType.BUS,
    val pnrOrBookingId: String = "",
    val operator: String = "",
    val trainOrBusNumber: String = "",
    val source: String = "",
    val destination: String = "",
    val journeyDateMillis: Long = 0L,
    val departureTime: String = "",
    val seatOrCoach: String = "",
    val berth: Berth = Berth.UNSPECIFIED,
    val passengerName: String = "",
    val originalFare: Long = 0L,
)

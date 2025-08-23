package com.ticketseller.server

import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable data class Health(val status: String = "ok")
@Serializable data class ListingDto(val id: String, val title: String, val priceMinor: Long)

fun main() {
  embeddedServer(Netty, port = System.getenv("PORT")?.toInt() ?: 8080) { module() }.start(wait = true)
}

fun Application.module() {
  install(ContentNegotiation) {
    json(Json {
      prettyPrint = true
      ignoreUnknownKeys = true
    })
  }
  routing {
    get("/") { call.respondText("Ticket Seller API") }
    get("/health") { call.respond(Health()) }
    get("/listings") {
      call.respond(
        listOf(
          ListingDto("l1", "Bengaluru → Mysuru (2 seats) 18:30", 14900),
          ListingDto("l2", "Pune → Mumbai (1 seat) 07:00", 9900)
        )
      )
    }
  }
}

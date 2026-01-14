package com.yodgorbek.trendoraai.backend.routes

import com.yodgorbek.trendoraai.backend.service.CoinService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.coinRoutes() {

    route("/api") {

        get("/coins") {
            call.respond(CoinService.getCoins())
        }

        get("/coins/{id}") {
            val id = call.parameters["id"]!!
            call.respond(CoinService.getCoinDetail(id) ?: mapOf("error" to "Not found"))
        }

        get("/coins/{id}/history") {
            val id = call.parameters["id"]!!
            val days = call.request.queryParameters["days"] ?: "30"
            call.respond(CoinService.getHistory(id, days))
        }

        get("/coins/{id}/predict") {
            val id = call.parameters["id"]!!
            val days = call.request.queryParameters["days"] ?: "30"
            call.respond(CoinService.getAiPrediction(id, days))
        }

        get("/coins/{id}/full") {
            val id = call.parameters["id"]!!
            val days = call.request.queryParameters["days"] ?: "30"
            try {
                call.respond(CoinService.getCoinFullData(id, days))
            } catch (e: Exception) {
                call.respond(io.ktor.http.HttpStatusCode.NotFound, mapOf("error" to e.message))
            }
        }
    }
}

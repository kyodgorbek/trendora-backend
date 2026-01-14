package com.yodgorbek.trendoraai.backend.plugins

import com.yodgorbek.trendoraai.backend.routes.coinRoutes
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.swagger.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("TrendoraAI Backend Running")
        }

        // IMPORTANT
        // IMPORTANT
        this.coinRoutes()

        // Swagger UI
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
    }
}



package no.nav.tsm.mottak.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.*

fun Application.configureMonitoring() {
    val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    install(MicrometerMetrics) {
        registry = appMicrometerRegistry
    }

    routing {
        get("/internal/prometheus") {
            call.respond(appMicrometerRegistry.scrape())
        }
        get("/internal/is_alive") {
            call.respond(HttpStatusCode.OK)
        }
        get("/internal/is_ready") {
            call.respond(HttpStatusCode.OK)
        }
    }
}

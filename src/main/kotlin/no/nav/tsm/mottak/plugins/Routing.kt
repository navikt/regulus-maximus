package no.nav.tsm.mottak.plugins

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.tsm.mottak.example.ExposedExample
import no.nav.tsm.mottak.example.ExampleService

fun Application.configureRouting() {
    routing {
        get("/hello-world") {
            ExampleService.create(ExposedExample(
                text = "Hello",
                someNumber = 42
            ))

            call.respondText("Hello World!")
        }
    }
}

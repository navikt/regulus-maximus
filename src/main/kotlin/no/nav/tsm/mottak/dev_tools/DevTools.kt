package no.nav.tsm.mottak.dev_tools

import indexPageRoute
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.module() {
  log.warn("Configuring development plugins, if you see this in production, something is wrong")

  configureDevRoutes()
  // Configure dev kafka producer
}

fun Application.configureDevRoutes() {
  routing { indexPageRoute() }
}

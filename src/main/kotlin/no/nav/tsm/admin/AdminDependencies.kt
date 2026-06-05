package no.nav.tsm.admin

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*

fun Application.configureAdminDependencies() {
    dependencies { provide(AdminSykmeldingRepo::class) }
}

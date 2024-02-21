package no.nav.tsm.mottak.plugins

import io.ktor.server.application.*
import no.nav.tsm.mottak.example.ExampleService
import no.nav.tsm.mottak.example.ExampleTransitiveDependency
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureDependencyInjection() {
  install(Koin) {
    slf4jLogger()

    modules(exampleModule)
  }
}

val exampleModule = module {
  singleOf(::ExampleService)
  singleOf(::ExampleTransitiveDependency)
}

// Alternative syntax:
val appModule = module {
  single { ExampleTransitiveDependency() }
  single { ExampleService(get()) }
}

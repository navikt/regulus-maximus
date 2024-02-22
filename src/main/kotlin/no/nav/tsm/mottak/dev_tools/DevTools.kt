package no.nav.tsm.mottak.dev_tools

import indexPageRoute
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import no.nav.tsm.mottak.plugins.Environment
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.koin.dsl.module
import org.koin.ktor.ext.get
import org.koin.ktor.ext.getKoin
import kotlin.collections.listOf
import kotlin.collections.set


fun Application.devTools() {
  getKoin().loadModules(listOf(module {
    single {
      val props = get<Environment>().kafkaConfig
      props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
      props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
      KafkaProducer<String, String>(props)
    }
  }))
  configureDevRoutes()
}

fun Application.configureDevRoutes() {
  routing { indexPageRoute(get(), get()) }
}

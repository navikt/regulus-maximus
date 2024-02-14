package no.nav.tsm.mottak.dev_tools

import indexPageRoute
import io.ktor.server.application.*
import io.ktor.server.routing.*
import no.nav.tsm.mottak.env
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer


fun Application.module() {
  log.warn("Configuring development plugins, if you see this in production, something is wrong")
  val props = environment.env.kafkaConfig
  props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
  props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
  val kafkaProducer = KafkaProducer<String, String>(props)
  configureDevRoutes(kafkaProducer)
  // Configure dev kafka producer
}

fun Application.configureDevRoutes(kafkaProducer: KafkaProducer<String, String>) {
  routing { indexPageRoute(kafkaProducer) }
}

package no.nav.tsm.mottak.dev_tools

import indexPageRoute
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.nav.tsm.mottak.plugins.Environment
import no.nav.tsm.mottak.sykmelding.kafka.model.SykmeldingInput
import no.nav.tsm.mottak.sykmelding.kafka.model.SykmeldingMedUtfall
import no.nav.tsm.mottak.sykmelding.kafka.util.SykmeldingDeserializer
import no.nav.tsm.mottak.sykmelding.kafka.util.SykmeldingMedUtfallSerializer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.cache.Cache
import org.apache.kafka.common.cache.LRUCache
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.ktor.ext.get
import org.koin.ktor.ext.getKoin
import org.koin.ktor.plugin.Koin
import kotlin.collections.listOf
import kotlin.collections.set
import kotlin.math.sin

val sykmeldingMedUtfallConsumerModule = module {
  single { KafkaProducer<String, SykmeldingInput>(get<Environment>().kafkaConfig.apply {
    this[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = SykmeldingMedUtfallSerializer::class.java.name
    this[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
  }) }
  single {
    KafkaConsumer(get<Environment>().kafkaConfig.apply {
      this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = SykmeldingDeserializer::class.java.name
      this[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
      this[ConsumerConfig.GROUP_ID_CONFIG] = "regulus-maximus"
    }, StringDeserializer(), SykmeldingDeserializer(SykmeldingMedUtfall::class))
  }
  single { HashMap<String, SykmeldingMedUtfall>() as Map<String, SykmeldingMedUtfall> }
  single {
    SykmeldingUtfallConsumer(get(), get(), get<Environment>().sykmeldingMedUtfall)
  }
}

fun Application.devTools() {
  getKoin().loadModules(
    listOf(
      sykmeldingMedUtfallConsumerModule)
  )
  configureDevRoutes()
  configureKafkaConsumer(get())
}

fun Application.configureKafkaConsumer(consumer: SykmeldingUtfallConsumer) {
  launch(Dispatchers.IO) { consumer.consumeSykmelding() }
}


fun Application.configureDevRoutes() {
  routing { indexPageRoute(get(), get(), get()) }
}


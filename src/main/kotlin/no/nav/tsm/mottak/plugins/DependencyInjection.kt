package no.nav.tsm.mottak.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.tsm.mottak.example.ExampleService
import no.nav.tsm.mottak.example.ExampleTransitiveDependency
import no.nav.tsm.mottak.sykmelding.kafka.SykmeldingConsumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureDependencyInjection() {
    install(Koin) {
        slf4jLogger()

        modules(
            environmentModule(),
            exampleModule,
            kafkaModule,
        )
    }
}

fun Application.environmentModule() = module {
    single<Environment> { createEnvironment() }
}

val exampleModule = module {
    singleOf(::ExampleService)
    singleOf(::ExampleTransitiveDependency)
}

val kafkaModule = module {
    single {
        KafkaConsumer<String, String>(get<Environment>().kafkaConfig.apply {
            this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
            this[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
            this[ConsumerConfig.GROUP_ID_CONFIG] = "regulus-maximus"
        })
    }
    single { get<Environment>().mottattSykmeldingTopic }
    singleOf(::SykmeldingConsumer)
}

package no.nav.tsm.mottak.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.tsm.mottak.example.ExampleService
import no.nav.tsm.mottak.example.ExampleTransitiveDependency
import no.nav.tsm.mottak.sykmelding.kafka.SykmeldingConsumer
import no.nav.tsm.mottak.sykmelding.kafka.model.SykmeldingInput
import no.nav.tsm.mottak.sykmelding.kafka.model.SykmeldingMedUtfall
import no.nav.tsm.mottak.sykmelding.kafka.util.SykmeldingDeserializer
import no.nav.tsm.mottak.sykmelding.kafka.util.SykmeldingMedUtfallSerializer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
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
            kafkaModule,
            exampleModule,
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
        KafkaConsumer(get<Environment>().kafkaConfig.apply {
            this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = SykmeldingDeserializer::class.java.name
            this[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
            this[ConsumerConfig.GROUP_ID_CONFIG] = "regulus-maximus"
        }, StringDeserializer(), SykmeldingDeserializer(SykmeldingInput::class))
    }
    single { get<Environment>().mottattSykmeldingTopic }
    single {
        val props = get<Environment>().kafkaConfig
        props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = SykmeldingMedUtfallSerializer::class.java.name
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
        KafkaProducer<String, SykmeldingMedUtfall>(props)
    }
    single {SykmeldingConsumer(get(), get<Environment>().mottattSykmeldingTopic, get<Environment>().sykmeldingMedUtfall, get())}

    //Declare a Module.single definition by resolving a constructor reference for the dependency. The resolution is done at compile time by leveraging inline functions, no reflection is required.

}

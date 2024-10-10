package no.nav.tsm.mottak.config

import no.nav.tsm.mottak.sykmelding.kafka.model.SykmeldingMedBehandlingsutfall
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.support.serializer.JsonDeserializer.TRUSTED_PACKAGES
import java.util.*

@Configuration
class KafkaConsumerConfig {

    @Bean
    fun containerFactory(p: KafkaProperties) =
        ConcurrentKafkaListenerContainerFactory<UUID, SykmeldingMedBehandlingsutfall>().apply {
            containerProperties.isObservationEnabled = true
            consumerFactory = DefaultKafkaConsumerFactory(p.buildConsumerProperties(null).apply {
                put(TRUSTED_PACKAGES, "no.nav.tsm.mottak.sykmelding.kafka.model")
            })
        }
}
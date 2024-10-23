package no.nav.tsm.mottak.config

import no.nav.tsm.mottak.sykmelding.kafka.model.SykmeldingMedBehandlingsutfall
import no.nav.tsm.mottak.sykmelding.kafka.util.SykmeldingDeserializer
import no.nav.tsm.mottak.sykmelding.kafka.util.SykmeldingMedUtfallSerializer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory


@Configuration
class KafkaConsumerConfig()  {


    @Bean
    fun containerFactory(props: KafkaProperties): ConcurrentKafkaListenerContainerFactory<String, SykmeldingMedBehandlingsutfall> {
        val consumerFactory = DefaultKafkaConsumerFactory<String, SykmeldingMedBehandlingsutfall>(props.buildConsumerProperties(null), StringDeserializer(), SykmeldingDeserializer(SykmeldingMedBehandlingsutfall::class))

        val factory = ConcurrentKafkaListenerContainerFactory<String,SykmeldingMedBehandlingsutfall>()
        factory.consumerFactory = consumerFactory
        return factory
    }

    @Bean
    fun producerFactory(props: KafkaProperties): KafkaProducer<String, SykmeldingMedBehandlingsutfall> {
        val producer = KafkaProducer<String, SykmeldingMedBehandlingsutfall>(props.buildProducerProperties(null), StringSerializer(), SykmeldingMedUtfallSerializer())
        return producer
    }

}
package no.nav.tsm.mottak.config

import no.nav.tsm.mottak.sykmelding.kafka.model.SykmeldingMedBehandlingsutfall
import no.nav.tsm.mottak.sykmelding.kafka.util.SykmeldingDeserializer
import no.nav.tsm.mottak.sykmelding.kafka.util.SykmeldingMedUtfallSerializer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory


@Configuration
class KafkaConsumerConfig {

    @Bean
    fun containerFactory(props: KafkaProperties): ConcurrentKafkaListenerContainerFactory<String, SykmeldingMedBehandlingsutfall> {
        val consumerFactory = DefaultKafkaConsumerFactory(
            props.buildConsumerProperties(null).apply {
                put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
                put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1)
                put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true)
            }, StringDeserializer(), SykmeldingDeserializer(SykmeldingMedBehandlingsutfall::class)
        )

        val factory = ConcurrentKafkaListenerContainerFactory<String, SykmeldingMedBehandlingsutfall>()
        factory.consumerFactory = consumerFactory
        return factory
    }

    @Bean
    fun producerFactory(props: KafkaProperties): KafkaProducer<String, SykmeldingMedBehandlingsutfall> {
        val producer =
            KafkaProducer(props.buildProducerProperties(null).apply{
                put(ProducerConfig.ACKS_CONFIG, "all")
                put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE)
            }, StringSerializer(), SykmeldingMedUtfallSerializer())
        return producer
    }

}

package no.nav.tsm.mottak.config

import no.nav.tsm.mottak.sykmelding.kafka.util.SykmeldingRecordSerializer
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory

@Configuration
@EnableConfigurationProperties
class KafkaConsumerConfig {

    @Bean
    fun containerFactory(
        props: KafkaProperties,
        errorHandler: ConsumerErrorHandler
    ): ConcurrentKafkaListenerContainerFactory<String, ByteArray?> {
        val consumerFactory = DefaultKafkaConsumerFactory(
            props.buildConsumerProperties(null).apply {
                put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
                put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1)
                put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true)
            }, StringDeserializer(), ByteArrayDeserializer()
        )

        val factory = ConcurrentKafkaListenerContainerFactory<String, ByteArray?>()
        factory.consumerFactory = consumerFactory
        factory.setCommonErrorHandler(errorHandler)
        return factory
    }

    @Bean
    fun kafkaProducer(props: KafkaProperties): KafkaProducer<String, SykmeldingRecord> {
        val producer =
            KafkaProducer(props.buildProducerProperties(null).apply {
                put(ProducerConfig.ACKS_CONFIG, "all")
                put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip")
                put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE)
            }, StringSerializer(), SykmeldingRecordSerializer())
        return producer
    }

}

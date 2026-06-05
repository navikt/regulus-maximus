package no.nav.tsm.mottak.sykmelding.kafka

import java.util.Properties
import kotlin.collections.set
import no.nav.tsm.core.Environment
import no.nav.tsm.core.logger
import no.nav.tsm.mottak.sykmelding.kafka.util.SykmeldingRecordSerializer
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.serialization.StringSerializer

class SykmeldingProducer(environment: Environment) {
    private val logger = logger()
    private val topicName = "tsm.sykmeldinger"

    private val kafkaProducer: KafkaProducer<String, SykmeldingRecord>

    init {
        val kafkaProperties = Properties(environment.kafka.config)

        kafkaProperties.apply {
            this[ProducerConfig.CLIENT_ID_CONFIG] =
                "${environment.runtime.name}-sykmelding-producer"
            this[ProducerConfig.ACKS_CONFIG] = "all"
            this[ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG] = "true"
            this[ProducerConfig.COMPRESSION_TYPE_CONFIG] = "gzip"
        }

        kafkaProducer =
            KafkaProducer(kafkaProperties, StringSerializer(), SykmeldingRecordSerializer())
    }

    fun send(sykmelding: SykmeldingRecord, headers: Headers) {
        val record = ProducerRecord(topicName, null, sykmelding.sykmelding.id, sykmelding, headers)
        val result = kafkaProducer.send(record).get()
        logger.debug(
            "Sent record with with id ${sykmelding.sykmelding.id} to topic '$topicName' on partition ${result.partition()} offset ${result.offset()}"
        )
    }

    fun tombstone(id: String, headers: Headers) {
        val record = ProducerRecord(topicName, null, id, null, headers)
        logger.debug("tombstone record with id $id on partition ${record.partition()}")
    }
}

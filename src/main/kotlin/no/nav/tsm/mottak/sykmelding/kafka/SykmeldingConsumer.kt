package no.nav.tsm.mottak.sykmelding.kafka

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import no.nav.tsm.mottak.config.KafkaConfigProperties
import no.nav.tsm.mottak.sykmelding.kafka.model.SykmeldingInput
import no.nav.tsm.mottak.sykmelding.kafka.model.SykmeldingMedUtfall
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@Service
class SykmeldingConsumer(
    private val kafkaConfigProperties: KafkaConfigProperties
) {


    private val logger = LoggerFactory.getLogger(SykmeldingConsumer::class.java)

    @KafkaListener(topics = ["\${spring.kafka.topics.mottatt-sykmelding}"], groupId = "\${spring.kafka.group-id}")
    fun consume(cr: ConsumerRecord<String, SykmeldingMedUtfall>?) {
        logger.info("Received message from topic: ${kafkaConfigProperties.topics.mottattSykmelding}")
        // mer kode
    }
}

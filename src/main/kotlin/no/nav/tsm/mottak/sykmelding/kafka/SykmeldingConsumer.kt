package no.nav.tsm.mottak.sykmelding.kafka

import no.nav.tsm.mottak.sykmelding.service.SykmeldingService
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.sykmeldingObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
class SykmeldingConsumer(
    private val sykmeldingService: SykmeldingService,
) {
  @KafkaListener(
        topics = ["\${spring.kafka.topics.sykmeldinger-input}"],
        groupId = "regulus-maximus-consumer",
        containerFactory = "containerFactory",
        batch = "false"
    )
    fun consume(record: ConsumerRecord<String, ByteArray?>) {
        val sykmelding = record.value()?.let { sykmeldingObjectMapper.readValue(it, SykmeldingRecord::class.java) }
        sykmeldingService.updateSykmelding(record.key(), sykmelding, record.headers())
    }
}

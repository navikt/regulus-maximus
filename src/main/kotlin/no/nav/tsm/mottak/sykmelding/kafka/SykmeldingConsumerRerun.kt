package no.nav.tsm.mottak.sykmelding.kafka

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.tsm.mottak.db.earliestFom
import no.nav.tsm.mottak.db.latestTom
import no.nav.tsm.mottak.sykmelding.service.SykmeldingService
import no.nav.tsm.sykmelding.input.core.model.SykmeldingModule
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class SykmeldingConsumerRerun(
    private val sykmeldingService: SykmeldingService,
) {

    private val log = LoggerFactory.getLogger(SykmeldingConsumerRerun::class.java)

    val objectMapper =
        jacksonObjectMapper().apply {
            registerModule(SykmeldingModule())
            registerModule(JavaTimeModule())
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        }

    @KafkaListener(
        topics = [$$"${spring.kafka.topics.sykmeldinger-input}"],
        groupId = "regulus-maximus-consumer-fix-fom-tom",
        containerFactory = "containerFactoryRerun",
        batch = "true"
    )
    fun consume(records: List<ConsumerRecord<String, ByteArray?>>) {

        val sykmeldinger: List<SykmeldingRecord> = records.mapNotNull { record ->
            try {
                record.value()?.let { objectMapper.readValue(it, SykmeldingRecord::class.java) }
            } catch (ex: Exception) {
                log.error("could not reprocess sykmelding ${record.key()}", ex)
                null
            }
        }.filter { it.sykmelding.aktivitet.size > 1 }

        if (sykmeldinger.isEmpty()) {
            return
        }

        log.info("Found ${sykmeldinger.size} with more than 1 period in batch! Tihi")
        sykmeldinger.forEach { sykmelding ->
            val correctFom: LocalDate = sykmelding.sykmelding.aktivitet.earliestFom()
            val correctTom: LocalDate = sykmelding.sykmelding.aktivitet.latestTom()
            try {
                sykmeldingService.fixFomTom(sykmelding.sykmelding.id, correctFom, correctTom)
            } catch (ex: Exception) {
                log.error("failed to update sykmelding fom/tom for ${sykmelding.sykmelding.id}", ex)
            }
        }
    }
}

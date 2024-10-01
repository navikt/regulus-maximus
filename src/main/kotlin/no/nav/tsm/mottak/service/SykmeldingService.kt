package no.nav.tsm.mottak.service

import no.nav.tsm.mottak.controller.SykmeldingController
import no.nav.tsm.mottak.db.SykmeldingEntity
import no.nav.tsm.mottak.db.SykmeldingRepository
import no.nav.tsm.mottak.sykmelding.kafka.model.SykmeldingInput
import no.nav.tsm.mottak.sykmelding.kafka.model.SykmeldingMedUtfall
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class SykmeldingService(
    private val sykmeldingRepository: SykmeldingRepository
) {
    private val logger = LoggerFactory.getLogger(SykmeldingService::class.java)

    @Transactional
    fun saveSykmelding(sykmelding: SykmeldingMedUtfall): Mono<SykmeldingEntity> {
        logger.info("lagrer sykmelding")
        val entity = SykmeldingEntity(
            sykmeldingId = sykmelding.sykmeldingInput.sykmeldingId,
            utfall = sykmelding.utfall
        )
        logger.info("sykmeldingEntity $entity")
        return sykmeldingRepository.save(entity)
    }

    fun getLatestSykmeldinger(): Flux<SykmeldingMedUtfall> {
        return sykmeldingRepository.findTop10ByOrderByIdDesc()
            .map { SykmeldingMedUtfall(SykmeldingInput(it.sykmeldingId), it.utfall) }
    }
}
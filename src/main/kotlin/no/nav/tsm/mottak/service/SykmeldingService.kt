package no.nav.tsm.mottak.service

import kotlinx.coroutines.flow.toList
import no.nav.tsm.mottak.db.SykmeldingBehandlingsutfall
import no.nav.tsm.mottak.db.SykmeldingMapper
import no.nav.tsm.mottak.db.SykmeldingRepository
import no.nav.tsm.mottak.sykmelding.kafka.model.SykmeldingMedBehandlingsutfall
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SykmeldingService(
    private val sykmeldingRepository: SykmeldingRepository,
    private val sykmeldingMapper: SykmeldingMapper,
) {
    private val logger = LoggerFactory.getLogger(SykmeldingService::class.java)

    @Transactional
    suspend fun saveSykmelding(sykmelding: SykmeldingMedBehandlingsutfall) : SykmeldingBehandlingsutfall {
        return sykmeldingRepository.save(sykmeldingMapper.toSykmeldingBehandlingsutfall(sykmelding))
    }

    suspend fun getLatestSykmeldinger(): List<SykmeldingBehandlingsutfall> {
        return sykmeldingRepository.findTop10ByOrderBySykmeldingIdDesc().toList()

    }
}

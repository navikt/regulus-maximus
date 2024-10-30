package no.nav.tsm.mottak.service

import kotlinx.coroutines.flow.toList
import no.nav.tsm.mottak.db.SykmeldingBehandlingsutfall
import no.nav.tsm.mottak.db.SykmeldingMapper
import no.nav.tsm.mottak.db.SykmeldingRepository
import no.nav.tsm.mottak.sykmelding.kafka.model.SykmeldingMedBehandlingsutfall
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SykmeldingService(
    private val sykmeldingRepository: SykmeldingRepository,
    private val sykmeldingMapper: SykmeldingMapper,
) {

    @Transactional
    suspend fun saveSykmelding(sykmelding: SykmeldingMedBehandlingsutfall) {

        val existingSykmelding = sykmeldingRepository.findById(sykmelding.sykmelding.id)
        sykmeldingRepository.upsertSykmelding(sykmeldingMapper.toSykmeldingBehandlingsutfall(sykmelding))
    }

    suspend fun getLatestSykmeldinger(): List<SykmeldingBehandlingsutfall> {
        return sykmeldingRepository.findTop10ByOrderByGeneratedDateDesc().toList()
    }

    suspend fun delete(sykmeldingId: String) {
        sykmeldingRepository.deleteById(sykmeldingId)
    }
}

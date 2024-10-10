package no.nav.tsm.mottak.db

import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository


@Repository
interface SykmeldingRepository : CoroutineCrudRepository<SykmeldingBehandlingsutfall, String> {

    fun findTop10ByOrderBySykmeldingIdDesc(): Flow<SykmeldingBehandlingsutfall>

    fun findTop10ByOrderByGeneratedDateDesc(): Flow<SykmeldingBehandlingsutfall>

}

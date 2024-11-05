package no.nav.tsm.mottak.db

import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface SykmeldingRepository : CrudRepository<SykmeldingBehandlingsutfall, String> {

    suspend fun findTop10ByOrderBySykmeldingIdDesc(): Flow<SykmeldingBehandlingsutfall>

    suspend fun findTop10ByOrderByGeneratedDateDesc(): Flow<SykmeldingBehandlingsutfall>

}

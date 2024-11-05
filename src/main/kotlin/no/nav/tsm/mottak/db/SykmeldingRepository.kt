package no.nav.tsm.mottak.db

import kotlinx.coroutines.flow.Flow
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface SykmeldingRepository : CrudRepository<SykmeldingBehandlingsutfall, String> {

    suspend fun findTop10ByOrderBySykmeldingIdDesc(): Flow<SykmeldingBehandlingsutfall>

    suspend fun findTop10ByOrderByGeneratedDateDesc(): Flow<SykmeldingBehandlingsutfall>

    @Query("""INSERT INTO sykmelding_behandlingsutfall (
            sykmelding_id, pasient_ident, fom, tom, generated_date, sykmelding, metadata, validation, meldingsinformasjon
        ) VALUES (
            :#{#sykmelding.sykmeldingId}, :#{#sykmelding.pasientIdent}, :#{#sykmelding.fom}, 
            :#{#sykmelding.tom}, :#{#sykmelding.generatedDate}, :#{#sykmelding.sykmelding}, 
            :#{#sykmelding.metadata}, :#{#sykmelding.validation}, :#{#sykmelding.meldingsinformasjon}
        )
        ON CONFLICT (sykmelding_id) DO UPDATE SET 
            pasient_ident = EXCLUDED.pasient_ident,
            fom = EXCLUDED.fom,
            tom = EXCLUDED.tom,
            generated_date = EXCLUDED.generated_date,
            sykmelding = EXCLUDED.sykmelding,
            metadata = EXCLUDED.metadata,
            validation = EXCLUDED.validation,
            meldingsinformasjon = EXCLUDED.meldingsinformasjon
    """)
    fun upsertSykmelding(@Param("sykmelding") sykmelding: SykmeldingBehandlingsutfall)
}

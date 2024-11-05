package no.nav.tsm.mottak.db

import kotlinx.coroutines.flow.Flow
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface SykmeldingRepository : CrudRepository<SykmeldingBehandlingsutfall, String> {

    @Query("SELECT e FROM SykmeldingBehandlingsutfall e ORDER BY e.generatedDate DESC")
    suspend fun findTop10ByOrderByGeneratedDateDesc(): List<SykmeldingBehandlingsutfall>

    @Modifying
    @Query(
        """
        INSERT INTO sykmelding_behandlingsutfall (
            sykmelding_id, pasient_ident, fom, tom, generated_date, sykmelding, metadata, validation, meldingsinformasjon
        ) VALUES (
            :#{#sykmelding.sykmeldingId}, to_jsonb(:#{#sykmelding.pasientIdent}), :#{#sykmelding.fom}, 
            :#{#sykmelding.tom}, :#{#sykmelding.generatedDate},to_jsonb(:#{#sykmelding.sykmelding}), 
            to_jsonb(:#{#sykmelding.metadata}), to_jsonb(:#{#sykmelding.validation}), :#{#sykmelding.meldingsinformasjon}
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
        """,
        nativeQuery = true
    )
    fun upsertSykmelding(@Param("sykmelding") sykmelding: SykmeldingBehandlingsutfall)
}

package no.nav.tsm.mottak.db

import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface SykmeldingRepository : CrudRepository<SykmeldingBehandlingsutfall, String> {

    fun findTop10ByOrderByGeneratedDateDesc(): List<SykmeldingBehandlingsutfall>

    @Modifying
    @Query(
        """
    INSERT INTO sykmelding (
        sykmelding_id, pasient_ident, fom, tom, generated_date, sykmelding, metadata, validation
    ) VALUES (
        :#{#sykmelding.sykmeldingId}, :#{#sykmelding.pasientIdent}, :#{#sykmelding.fom}, 
        :#{#sykmelding.tom}, :#{#sykmelding.generatedDate}, :#{#sykmelding.sykmelding},
        :#{#sykmelding.metadata}, :#{#sykmelding.validation}
    )
    ON CONFLICT (sykmelding_id) DO UPDATE SET 
        pasient_ident = EXCLUDED.pasient_ident,
        fom = EXCLUDED.fom,
        tom = EXCLUDED.tom,
        generated_date = EXCLUDED.generated_date,
        sykmelding = EXCLUDED.sykmelding,
        metadata = EXCLUDED.metadata,
        validation = EXCLUDED.validation
    """
    )
    fun upsertSykmelding(@Param("sykmelding") sykmelding: SykmeldingBehandlingsutfall)
}

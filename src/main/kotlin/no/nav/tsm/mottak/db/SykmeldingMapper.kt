package no.nav.tsm.mottak.db

import no.nav.tsm.mottak.sykmelding.kafka.model.SykmeldingMedBehandlingsutfall
import no.nav.tsm.mottak.sykmelding.kafka.objectMapper
import org.postgresql.util.PGobject
import org.springframework.stereotype.Component

@Component
class SykmeldingMapper {

    fun toSykmeldingBehandlingsutfall(
        sykmeldingMedBehandlingsutfall: SykmeldingMedBehandlingsutfall
    ): SykmeldingBehandlingsutfall {
        return SykmeldingBehandlingsutfall(
            sykmeldingId = sykmeldingMedBehandlingsutfall.sykmelding.id,
            pasientIdent = sykmeldingMedBehandlingsutfall.sykmelding.pasient.fnr,
            fom = sykmeldingMedBehandlingsutfall.sykmelding.aktivitet.first().fom,
            tom = sykmeldingMedBehandlingsutfall.sykmelding.aktivitet.last().tom,
            generatedDate = sykmeldingMedBehandlingsutfall.sykmelding.metadata.genDate,
            sykmelding =  sykmeldingMedBehandlingsutfall.sykmelding.toPGobject(),
            validation = sykmeldingMedBehandlingsutfall.validation.toPGobject(),
            metadata = sykmeldingMedBehandlingsutfall.metadata.toPGobject(),
        )
    }
}

fun Any.toPGobject() : PGobject {
    return PGobject().also {
        it.value = objectMapper.writeValueAsString(this)
        it.type = "json"
    }
}

package no.nav.tsm.mottak.db

import com.fasterxml.jackson.module.kotlin.readValue
import io.r2dbc.postgresql.codec.Json
import no.nav.tsm.mottak.sykmelding.kafka.model.SykmeldingKilde
import no.nav.tsm.mottak.sykmelding.kafka.model.SykmeldingMedBehandlingsutfall
import no.nav.tsm.mottak.sykmelding.kafka.objectMapper
import org.postgresql.util.PGobject
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component


@Component
class SykmeldingMapper {
   /* fun toSykmeldingMedBehandlingsutfall(
        sykmeldingBehandlingsutfall: SykmeldingBehandlingsutfall
    ): SykmeldingMedBehandlingsutfall {
        return SykmeldingMedBehandlingsutfall(
            sykmelding = objectMapper.readValue(sykmeldingBehandlingsutfall.sykmelding.value),
            validation = objectMapper.readValue(sykmeldingBehandlingsutfall.validation.toString()),
            kilde = enumValueOf<SykmeldingKilde>(sykmeldingBehandlingsutfall.kilde)
        )
    }*/
    fun toSykmeldingBehandlingsutfall(
        sykmeldingMedBehandlingsutfall: SykmeldingMedBehandlingsutfall
    ): SykmeldingBehandlingsutfall {
        return SykmeldingBehandlingsutfall(
            sykmeldingId = sykmeldingMedBehandlingsutfall.sykmelding.id,
            pasientIdent = sykmeldingMedBehandlingsutfall.sykmelding.pasient.ident,
            fom = sykmeldingMedBehandlingsutfall.sykmelding.aktivitet.first().fom,
            tom = sykmeldingMedBehandlingsutfall.sykmelding.aktivitet.last().tom,
            sykmelding = Json.of(objectMapper.writeValueAsString(sykmeldingMedBehandlingsutfall.sykmelding)),
            metadata = Json.of(objectMapper.writeValueAsString(sykmeldingMedBehandlingsutfall.sykmelding.metadata)),
            kilde = sykmeldingMedBehandlingsutfall.kilde.name,
            validation = Json.of(objectMapper.writeValueAsString(sykmeldingMedBehandlingsutfall.validation)),
        )
    }
}

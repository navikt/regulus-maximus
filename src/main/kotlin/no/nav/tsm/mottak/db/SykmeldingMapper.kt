package no.nav.tsm.mottak.db

import io.r2dbc.postgresql.codec.Json
import no.nav.tsm.mottak.sykmelding.kafka.model.SykmeldingMedBehandlingsutfall
import no.nav.tsm.mottak.sykmelding.kafka.objectMapper
import org.springframework.stereotype.Component


@Component
class SykmeldingMapper {

    fun toSykmeldingBehandlingsutfall(
        sykmeldingMedBehandlingsutfall: SykmeldingMedBehandlingsutfall
    ): SykmeldingBehandlingsutfall {
        return SykmeldingBehandlingsutfall(
            sykmeldingId = sykmeldingMedBehandlingsutfall.sykmelding.id,
            pasientIdent = Json.of(objectMapper.writeValueAsString(sykmeldingMedBehandlingsutfall.sykmelding.pasient)),
            fom = sykmeldingMedBehandlingsutfall.sykmelding.aktivitet.first().fom,
            tom = sykmeldingMedBehandlingsutfall.sykmelding.aktivitet.last().tom,
            generatedDate = sykmeldingMedBehandlingsutfall.sykmelding.metadata.genDate,
            sykmelding = Json.of(objectMapper.writeValueAsString(sykmeldingMedBehandlingsutfall.sykmelding)),
            metadata = Json.of(objectMapper.writeValueAsString(sykmeldingMedBehandlingsutfall.sykmelding.metadata)),
            validation = Json.of(objectMapper.writeValueAsString(sykmeldingMedBehandlingsutfall.validation)),
            meldingsinformasjon = Json.of(objectMapper.writeValueAsString(sykmeldingMedBehandlingsutfall.metadata)),
        )
    }
}

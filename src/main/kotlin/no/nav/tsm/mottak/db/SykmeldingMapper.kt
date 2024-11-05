package no.nav.tsm.mottak.db

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
            pasientIdent = objectMapper.writeValueAsString(sykmeldingMedBehandlingsutfall.sykmelding.pasient),
            fom = sykmeldingMedBehandlingsutfall.sykmelding.aktivitet.first().fom,
            tom = sykmeldingMedBehandlingsutfall.sykmelding.aktivitet.last().tom,
            generatedDate = sykmeldingMedBehandlingsutfall.sykmelding.metadata.genDate,
            sykmelding = objectMapper.writeValueAsString(sykmeldingMedBehandlingsutfall.sykmelding),
            metadata = objectMapper.writeValueAsString(sykmeldingMedBehandlingsutfall.sykmelding.metadata),
            validation = objectMapper.writeValueAsString(sykmeldingMedBehandlingsutfall.validation),
            meldingsinformasjon = objectMapper.writeValueAsString(sykmeldingMedBehandlingsutfall.metadata),
        )
    }
}

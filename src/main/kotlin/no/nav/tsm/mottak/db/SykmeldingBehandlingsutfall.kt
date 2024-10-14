package no.nav.tsm.mottak.db
import io.r2dbc.postgresql.codec.Json
import no.nav.tsm.mottak.sykmelding.kafka.model.metadata.PersonId
import org.springframework.data.relational.core.mapping.Table


import java.time.LocalDate
import java.time.OffsetDateTime

@Table("sykmelding_behandlingsutfall")
data class SykmeldingBehandlingsutfall (
    val sykmeldingId: String,
    val pasientIdent: List<PersonId>,
    val fom: LocalDate,
    val tom: LocalDate,
    val generatedDate: OffsetDateTime?,
    val sykmelding: Json,
    val metadata: Json,
    val validation: Json
)

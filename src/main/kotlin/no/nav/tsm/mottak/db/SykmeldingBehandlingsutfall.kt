package no.nav.tsm.mottak.db
import io.r2dbc.postgresql.codec.Json
import org.springframework.data.relational.core.mapping.Table


import java.time.LocalDate

@Table("sykmelding_behandlingsutfall")
data class SykmeldingBehandlingsutfall (
    val sykmeldingId: String,
    val pasientIdent: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val sykmelding: Json,
    val metadata: Json,
    val kilde: String,
    val validation: Json
)

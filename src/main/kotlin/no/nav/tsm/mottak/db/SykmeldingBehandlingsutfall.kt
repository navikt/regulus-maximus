package no.nav.tsm.mottak.db

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Id
import java.time.LocalDate
import java.time.OffsetDateTime


@Table(name = "sykmelding_behandlingsutfall")
@Entity
data class SykmeldingBehandlingsutfall(
    @Id val sykmeldingId: String,
    @Column(columnDefinition = "jsonb")
    val pasientIdent: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val generatedDate: OffsetDateTime?,
    @Column(columnDefinition = "jsonb")
    val sykmelding: String,
    @Column(columnDefinition = "jsonb")
    val metadata: String,
    @Column(columnDefinition = "jsonb")
    val validation: String,
    @Column(columnDefinition = "jsonb")
    val meldingsinformasjon: String
) {
    constructor() : this(
        sykmeldingId = "",
        pasientIdent = "",
        fom = LocalDate.now(),
        tom = LocalDate.now(),
        generatedDate = null,
        sykmelding = "",
        metadata = "",
        validation = "",
        meldingsinformasjon = ""
    )
}
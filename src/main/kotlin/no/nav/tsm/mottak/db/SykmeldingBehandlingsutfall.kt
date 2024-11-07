package no.nav.tsm.mottak.db

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Id
import java.time.LocalDate
import java.time.OffsetDateTime


@Table(name = "sykmelding_behandlingsutfall")
@Entity
data class SykmeldingBehandlingsutfall(
    @Column(name = "sykmelding_id")
    @Id val sykmeldingId: String,
    @Column(name = "pasient_ident")
    val pasientIdent: String,
    val fom: LocalDate,
    val tom: LocalDate,
    @Column(name = "generated_date")
    val generatedDate: OffsetDateTime?,
    @Column(columnDefinition = "jsonb")
    val sykmelding: String,
    @Column(columnDefinition = "jsonb")
    val metadata: String,
    @Column(columnDefinition = "jsonb")
    @Convert(converter = JpaConverter::class)
    val validation: String,
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
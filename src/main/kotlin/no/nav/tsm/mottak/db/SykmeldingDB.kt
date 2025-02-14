package no.nav.tsm.mottak.db

import org.postgresql.util.PGobject
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.time.OffsetDateTime


@Table(name = "sykmelding")
data class SykmeldingDB(
    val sykmeldingId: String,
    val pasientIdent: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val generatedDate: OffsetDateTime?,
    val sykmelding: PGobject,
    val validation: PGobject,
    val metadata: PGobject
)

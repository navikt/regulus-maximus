package no.nav.tsm.mottak.db

import com.fasterxml.jackson.databind.util.JSONPObject
import io.r2dbc.postgresql.codec.Json
import no.nav.tsm.mottak.sykmelding.kafka.model.Sykmelding
import no.nav.tsm.mottak.sykmelding.kafka.model.SykmeldingMetadata
import no.nav.tsm.mottak.sykmelding.kafka.model.ValidationResult
import org.postgresql.util.PGobject
import org.springframework.data.annotation.Id
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import org.springframework.expression.spel.ast.OpGT


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

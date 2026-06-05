package no.nav.tsm.mottak.db

import no.nav.tsm.sykmelding.input.core.model.Sykmelding
import no.nav.tsm.sykmelding.input.core.model.ValidationResult
import no.nav.tsm.sykmelding.input.core.model.metadata.MessageMetadata
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object SykmeldingTable : Table("sykmelding") {
    val sykmeldingId = text("sykmelding_id")
    val pasientIdent = text("pasient_ident")
    val fom = date("fom")
    val tom = date("tom")
    val generatedDate = timestampWithTimeZone("generated_date")
    val sykmelding = jacksonJsonb<Sykmelding>("sykmelding")
    val validation = jacksonJsonb<ValidationResult>("validation")
    val metadata = jacksonJsonb<MessageMetadata>("metadata")

    override val primaryKey = PrimaryKey(sykmeldingId)
}

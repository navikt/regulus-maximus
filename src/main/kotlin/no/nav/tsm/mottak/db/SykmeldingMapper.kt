package no.nav.tsm.mottak.db

import no.nav.tsm.sykmelding.input.core.model.Sykmelding
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.ValidationResult
import no.nav.tsm.sykmelding.input.core.model.metadata.MessageMetadata

private inline fun <reified T : MessageMetadata> MessageMetadata.requireType(): T =
    when (this) {
        is T -> this
        else -> throw IllegalStateException("Invalid metadata type: $type")
    }

fun toSpecificSykmeldingRecord(
    sykmelding: Sykmelding,
    metadata: MessageMetadata,
    validation: ValidationResult,
): SykmeldingRecord {
    return when (sykmelding) {
        is Sykmelding.Digital ->
            SykmeldingRecord.Digital(
                metadata = metadata.requireType(),
                sykmelding = sykmelding,
                validation = validation,
            )

        is Sykmelding.Papir ->
            SykmeldingRecord.Papir(
                metadata = metadata.requireType(),
                sykmelding = sykmelding,
                validation = validation,
            )

        is Sykmelding.Xml ->
            SykmeldingRecord.Xml(
                metadata = metadata.requireType(),
                sykmelding = sykmelding,
                validation = validation,
            )

        is Sykmelding.Utenlandsk ->
            SykmeldingRecord.Utenlandsk(
                metadata = metadata.requireType(),
                sykmelding = sykmelding,
                validation = validation,
            )
    }
}

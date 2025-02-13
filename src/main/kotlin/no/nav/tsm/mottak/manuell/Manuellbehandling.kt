package no.nav.tsm.mottak.manuell

import com.google.cloud.storage.Storage
import no.nav.tsm.mottak.sykmelding.exceptions.SykmeldingMergeValidationException
import no.nav.tsm.mottak.sykmelding.model.InvalidRule
import no.nav.tsm.mottak.sykmelding.model.OKRule
import no.nav.tsm.mottak.sykmelding.model.PendingRule
import no.nav.tsm.mottak.sykmelding.model.SykmeldingRecord
import no.nav.tsm.mottak.sykmelding.model.TilbakedatertMerknad
import no.nav.tsm.mottak.sykmelding.model.ValidationResult
import no.nav.tsm.mottak.sykmelding.model.ValidationType
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class ManuellbehandlingService(
    private val storage: Storage,
    @Value("\${manuellbehandling.bucket}") private val bucket: String,
    @Value("\${manuellbehandling.filename}") private val filename: String
) {

    private lateinit var manuellBehandlinger: Map<String, OffsetDateTime>

    init {
        manuellBehandlinger = getFromBucket()
    }

    private fun getFromBucket(): Map<String, OffsetDateTime> {
        val csvContent = storage.get(bucket, filename)
        val lines = csvContent.getContent().toString(Charsets.UTF_8).split("\n")
        return lines.filter {
            it.isNotBlank()
        }.associate {
            val (fnr, timestamp) = it.split(",")
            fnr to OffsetDateTime.parse(timestamp.replaceFirst(" ", "T").replace(" ", ""))
        }
    }

    fun getManuellBehnaldinger(): Map<String, OffsetDateTime> {
        return manuellBehandlinger
    }

    fun getManuellBehandlingTimestamp(sykmeldingId: String): OffsetDateTime? {
        return manuellBehandlinger[sykmeldingId]
    }

}

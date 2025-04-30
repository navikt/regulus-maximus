package no.nav.tsm.mottak.manuell

import com.google.cloud.storage.Storage
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
            val (sykmeldingId, timestamp) = it.split(",")
            sykmeldingId to OffsetDateTime.parse(timestamp.replaceFirst(" ", "T").replace(" ", ""))
        }
    }

    fun getManuellBehandlingTimestamp(sykmeldingId: String): OffsetDateTime? {
        return manuellBehandlinger[sykmeldingId]
    }

}

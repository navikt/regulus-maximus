package no.nav.tsm.mottak.sykmelding.kafka

import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import no.nav.tsm.mottak.sykmelding.service.SykmeldingService
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import org.apache.kafka.common.header.Headers

class SykmeldingInputConsumerService(
    private val consumer: SykmeldingInputConsumer,
    private val service: SykmeldingService,
) {
    suspend fun consume() =
        withContext(Dispatchers.IO) {
            consumer.subscribe()
            try {
                while (isActive) {
                    val records = consumer.poll()
                    handleRecords(records)
                }
            } finally {
                withContext(NonCancellable) { consumer.unsubscribe() }
            }
        }

    @WithSpan(kind = SpanKind.CONSUMER, inheritContext = false)
    private suspend fun handleRecords(records: List<Triple<String, SykmeldingRecord?, Headers>>) {
        records.forEach { (key, record, headers) -> service.updateSykmelding(key, record, headers) }
    }
}

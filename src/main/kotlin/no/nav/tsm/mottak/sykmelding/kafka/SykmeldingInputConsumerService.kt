package no.nav.tsm.mottak.sykmelding.kafka

import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import no.nav.tsm.core.logger
import no.nav.tsm.mottak.sykmelding.service.SykmeldingService
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import org.apache.kafka.common.header.Headers

class SykmeldingInputConsumerService(
    private val consumer: SykmeldingInputConsumer,
    private val service: SykmeldingService,
) {
    private val logger = logger()

    suspend fun consumeWithRetry() =
        withContext(Dispatchers.IO) {
            while (isActive) {
                consumer.subscribe()
                try {
                    while (isActive) {
                        val records = consumer.poll()
                        handleRecords(records)
                    }
                } catch (_: CancellationException) {
                    logger.info("Sykmelding consumer job cancelled, shutting down gracefully")
                    withContext(NonCancellable) { consumer.unsubscribe() }
                } catch (ex: Exception) {
                    logger.error("Error while consuming records", ex)
                    consumer.unsubscribe()

                    delay(60.seconds)
                    logger.warn("Retrying after 60 seconds...")
                }
            }
        }

    @WithSpan(kind = SpanKind.CONSUMER, inheritContext = false)
    private suspend fun handleRecords(records: List<Triple<String, SykmeldingRecord?, Headers>>) {
        records.forEach { (key, record, headers) -> service.updateSykmelding(key, record, headers) }
    }
}

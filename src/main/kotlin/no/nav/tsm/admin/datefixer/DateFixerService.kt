package no.nav.tsm.admin.datefixer

import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import java.time.LocalDate
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import no.nav.tsm.core.logger
import no.nav.tsm.sykmelding.input.core.model.Aktivitet
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord

class DateFixerService(
    private val consumer: SykmeldingInputDateFixerConsumer,
    private val repo: DateFixerRepo,
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
                    logger.info("Sykmelding fixer job cancelled, shutting down gracefully")
                    withContext(NonCancellable) { consumer.unsubscribe() }
                } catch (ex: Exception) {
                    logger.error("Error while consuming fixer records", ex)
                    consumer.unsubscribe()

                    delay(60.seconds)
                    logger.warn("Retrying after 60 seconds... (fixer)")
                }
            }
        }

    @WithSpan(kind = SpanKind.CONSUMER, inheritContext = false)
    private suspend fun handleRecords(records: List<Pair<String, SykmeldingRecord?>>) {
        val sykmeldingerToFix: List<Pair<String, SykmeldingRecord>> =
            records
                .filter { (_, record) -> record != null && record.sykmelding.aktivitet.size > 1 }
                .map { (key, record) -> key to record!! }

        if (sykmeldingerToFix.isEmpty()) return

        sykmeldingerToFix.forEach { (key, record) -> fixSykmeldingIfNeeded(key, record) }
    }

    private suspend fun fixSykmeldingIfNeeded(key: String, record: SykmeldingRecord) {
        val actualEarliest = record.sykmelding.aktivitet.earliestFom()
        val actualLatest = record.sykmelding.aktivitet.latestTom()

        val isWrong = repo.isDateDiff(key, actualEarliest, actualLatest)
        if (!isWrong) {
            // Its fine lets not log
            return
        } else {
            logger.info(
                "Fixing sykmelding with key $key, it has ${record.sykmelding.aktivitet.size} perioder"
            )
            repo.upDate(key, actualEarliest, actualLatest)
        }
    }
}

private fun List<Aktivitet>.earliestFom(): LocalDate = maxOf { it.fom }

private fun List<Aktivitet>.latestTom(): LocalDate = maxOf { it.tom }

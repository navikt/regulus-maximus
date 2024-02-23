package no.nav.tsm.mottak.dev_tools

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import no.nav.tsm.mottak.sykmelding.kafka.model.SykmeldingMedUtfall
import org.apache.kafka.clients.consumer.KafkaConsumer
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class SykmeldingUtfallConsumer(private val kafkaCOnsumer: KafkaConsumer<String, SykmeldingMedUtfall>,
                               private val cache: MutableMap<String, SykmeldingMedUtfall>,
                               private val topic: String) {
    suspend fun consumeSykmelding() = withContext(Dispatchers.IO) {
        kafkaCOnsumer.subscribe(listOf(topic))
        while (isActive) {
            val records = kafkaCOnsumer.poll(1.seconds.toJavaDuration())
            records.forEach { record ->
                cache[record.key()] = record.value()
            }
        }
    }
}

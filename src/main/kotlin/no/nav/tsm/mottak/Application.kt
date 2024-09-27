package no.nav.tsm.mottak

import no.nav.tsm.mottak.config.KafkaConfigProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.kafka.annotation.EnableKafka

@SpringBootApplication
@EnableCaching
@EnableKafka
@EnableConfigurationProperties(KafkaConfigProperties::class)
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}

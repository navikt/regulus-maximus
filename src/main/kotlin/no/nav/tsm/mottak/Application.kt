package no.nav.tsm.mottak

import no.nav.tsm.mottak.config.KafkaConfigProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories
import no.nav.boot.conditionals.Cluster.Companion.profiler

@SpringBootApplication
@EnableCaching
@EnableKafka
@EnableR2dbcRepositories
@EnableR2dbcAuditing
@EnableConfigurationProperties(KafkaConfigProperties::class)
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args) {
        setAdditionalProfiles(*profiler())
    }
}

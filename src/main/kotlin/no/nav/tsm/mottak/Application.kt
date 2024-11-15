package no.nav.tsm.mottak

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories
import org.springframework.kafka.annotation.EnableKafka

@SpringBootApplication
@EnableKafka
@EnableJdbcRepositories
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args) {}
}

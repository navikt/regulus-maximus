package no.nav.tsm.mottak.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "spring.kafka")
class KafkaConfigProperties {
    lateinit var bootstrapServers: String
    lateinit var groupId: String
    lateinit var topics: Topics

    class Topics {
        lateinit var mottattSykmelding: String
        lateinit var sykmeldingMedUtfall: String
    }
}
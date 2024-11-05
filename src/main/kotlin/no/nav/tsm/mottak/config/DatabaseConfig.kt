package no.nav.tsm.mottak.config

import org.flywaydb.core.Flyway
import org.springframework.boot.autoconfigure.flyway.FlywayProperties
import org.springframework.boot.autoconfigure.jdbc.JdbcProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(JdbcProperties::class, FlywayProperties::class)
class DatabaseConfig {

    @Bean
    fun flyway(flywayProperties: FlywayProperties, jdbcProperties: JdbcProperties): Flyway {
        val flyway = Flyway.configure()
            .dataSource(
                flywayProperties.url,
                jdbcProperties.toString(),
                jdbcProperties.toString()
            )
            .locations(*flywayProperties.locations.toTypedArray())
            .baselineOnMigrate(true)
            .load()

        flyway.migrate()
        return flyway
    }
}

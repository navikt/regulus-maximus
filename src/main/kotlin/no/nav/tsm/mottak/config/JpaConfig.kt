package no.nav.tsm.mottak.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import jakarta.persistence.EntityManagerFactory
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@EnableJpaRepositories
@EnableTransactionManagement
class JpaConfig {

    @Autowired
    lateinit var dataSourceProperties: DataSourceProperties

    @Bean
    fun entityManagerFactory(builder: EntityManagerFactoryBuilder): LocalContainerEntityManagerFactoryBean {
        return builder
            .dataSource(dataSourceProperties.initializeDataSourceBuilder().build())
            .packages("no.nav.tsm.mottak.db")
            .persistenceUnit("yourPersistenceUnit")
            .build()
    }

    @Bean
    fun transactionManager(entityManagerFactory: EntityManagerFactory): PlatformTransactionManager {
        return JpaTransactionManager(entityManagerFactory)
    }
}

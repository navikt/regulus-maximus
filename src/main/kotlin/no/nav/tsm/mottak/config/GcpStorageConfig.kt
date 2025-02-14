package no.nav.tsm.mottak.config

import com.google.cloud.storage.StorageOptions
import com.google.cloud.storage.Storage
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GcpStorageConfig {

    @Bean
    fun storage(): Storage {
        return StorageOptions.getDefaultInstance().service
    }
}

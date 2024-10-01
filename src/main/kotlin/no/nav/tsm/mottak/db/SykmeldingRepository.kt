package no.nav.tsm.mottak.db

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

@Repository
interface SykmeldingRepository : ReactiveCrudRepository<SykmeldingEntity, Long> {

    fun findTop10ByOrderByIdDesc(): Flux<SykmeldingEntity>

    fun findTop10ByOrderByIdDesc(sykmeldingEntity: SykmeldingEntity)
}
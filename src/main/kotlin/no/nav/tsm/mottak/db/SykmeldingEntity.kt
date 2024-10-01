package no.nav.tsm.mottak.db

import jakarta.persistence.GeneratedValue
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("sykmelding_entity")
data class SykmeldingEntity (
    @Id
    @GeneratedValue
    val id: Long? = null,
    val sykmeldingId: String,
    val utfall: String
)
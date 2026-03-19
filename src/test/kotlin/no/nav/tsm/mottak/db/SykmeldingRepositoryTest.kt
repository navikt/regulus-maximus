package no.nav.tsm.mottak.db

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.postgresql.util.PGobject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.postgresql.PostgreSQLContainer
import java.time.LocalDate
import java.time.OffsetDateTime

@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration::class)
class SykmeldingRepositoryTest {

    companion object {
        val postgres = PostgreSQLContainer("postgres:16-alpine")

        init {
            postgres.start()
        }


        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.flyway.url", postgres::getJdbcUrl)
            registry.add("spring.flyway.user", postgres::getUsername)
            registry.add("spring.flyway.password", postgres::getPassword)
            registry.add("spring.flyway.target") { "1" }
        }
    }

    @Autowired
    lateinit var sykmeldingRepository: SykmeldingRepository

    private fun jsonb(value: String): PGobject = PGobject().apply {
        type = "jsonb"
        this.value = value
    }

    private fun createSykmelding(
        sykmeldingId: String = "test-id-1",
        pasientIdent: String = "12345678901",
        fom: LocalDate = LocalDate.of(2024, 1, 1),
        tom: LocalDate = LocalDate.of(2024, 1, 15),
    ) = SykmeldingDB(
        sykmeldingId = sykmeldingId,
        pasientIdent = pasientIdent,
        fom = fom,
        tom = tom,
        generatedDate = OffsetDateTime.now(),
        sykmelding = jsonb("""{"type": "test"}"""),
        validation = jsonb("""{"status": "OK"}"""),
        metadata = jsonb("""{"source": "test"}"""),
    )

    @Test
    fun `upsert and find by sykmeldingId`() {
        val sykmelding = createSykmelding()
        sykmeldingRepository.upsertSykmelding(sykmelding)

        val found = sykmeldingRepository.findBySykmeldingId("test-id-1")
        assertNotNull(found)
        assertEquals("test-id-1", found!!.sykmeldingId)
        assertEquals("12345678901", found.pasientIdent)
        assertEquals(LocalDate.of(2024, 1, 1), found.fom)
        assertEquals(LocalDate.of(2024, 1, 15), found.tom)
    }

    @Test
    fun `upsert updates existing record on conflict`() {
        val original = createSykmelding(pasientIdent = "11111111111")
        sykmeldingRepository.upsertSykmelding(original)

        val updated = createSykmelding(pasientIdent = "22222222222")
        sykmeldingRepository.upsertSykmelding(updated)

        val found = sykmeldingRepository.findBySykmeldingId("test-id-1")
        assertNotNull(found)
        assertEquals("22222222222", found!!.pasientIdent)
    }

    @Test
    fun `findBySykmeldingId returns null when not found`() {
        val found = sykmeldingRepository.findBySykmeldingId("nonexistent")
        assertNull(found)
    }

    @Test
    fun `fixFomTom updates fom and tom for sykmeldingId`() {
        val sykmelding = createSykmelding(
            sykmeldingId = "fix-fom-tom-id",
            fom = LocalDate.of(2024, 1, 1),
            tom = LocalDate.of(2024, 1, 15),
        )
        sykmeldingRepository.upsertSykmelding(sykmelding)

        val newFom = LocalDate.of(2024, 3, 1)
        val newTom = LocalDate.of(2024, 3, 31)
        sykmeldingRepository.fixFomTom("fix-fom-tom-id", newFom, newTom)

        val found = sykmeldingRepository.findBySykmeldingId("fix-fom-tom-id")
        assertNotNull(found)
        assertEquals(newFom, found!!.fom)
        assertEquals(newTom, found.tom)
    }

    @Test
    fun `deleteBySykmeldingId deletes existing record`() {
        val sykmelding = createSykmelding(sykmeldingId = "to-delete")
        sykmeldingRepository.upsertSykmelding(sykmelding)

        val deleted = sykmeldingRepository.deleteBySykmeldingId("to-delete")
        assertTrue(deleted)

        val found = sykmeldingRepository.findBySykmeldingId("to-delete")
        assertNull(found)
    }
}

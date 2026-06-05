package no.nav.tsm.mottak.db

import io.kotest.matchers.equals.shouldEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlinx.coroutines.test.runTest
import no.nav.tsm.sykmelding.input.core.model.*
import no.nav.tsm.sykmelding.input.core.model.metadata.HelsepersonellKategori
import no.nav.tsm.sykmelding.input.core.model.metadata.MessageMetadata
import no.nav.tsm.sykmelding.input.core.model.metadata.Navn
import no.nav.tsm.utils.WithPostgresql
import org.junit.Test

class SykmeldingRepositoryTest : WithPostgresql() {
    companion object {
        init {
            runMigrations(true)
            connect()
        }
    }

    private val sykmeldingRepository = SykmeldingRepository()

    @Test
    fun `upsert and find by sykmeldingId`() = runTest {
        val sykmelding = createSykmelding()
        sykmeldingRepository.upsertSykmelding(sykmelding)

        val found = sykmeldingRepository.findBySykmeldingId("test-id-1")

        found.shouldNotBeNull()
        found.sykmelding.id shouldEqual "test-id-1"
        found.sykmelding.pasient.fnr shouldEqual "12345678901"
        found.sykmelding.aktivitet.first().fom shouldEqual LocalDate.of(2024, 1, 1)
        found.sykmelding.aktivitet.first().tom shouldEqual LocalDate.of(2024, 1, 15)
    }

    @Test
    fun `upsert updates existing record on conflict`() = runTest {
        val original = createSykmelding(pasientIdent = "11111111111")
        sykmeldingRepository.upsertSykmelding(original)

        val updated = createSykmelding(pasientIdent = "22222222222")
        sykmeldingRepository.upsertSykmelding(updated)

        val found = sykmeldingRepository.findBySykmeldingId("test-id-1")
        found.shouldNotBeNull()
        found.sykmelding.pasient.fnr shouldEqual "22222222222"
    }

    @Test
    fun `findBySykmeldingId returns null when not found`() = runTest {
        val found = sykmeldingRepository.findBySykmeldingId("nonexistent")

        found.shouldBeNull()
    }

    @Test
    fun `deleteBySykmeldingId deletes existing record`() = runTest {
        val sykmelding = createSykmelding(sykmeldingId = "to-delete")
        sykmeldingRepository.upsertSykmelding(sykmelding)

        val deleted = sykmeldingRepository.deleteBySykmeldingId("to-delete")
        deleted shouldBe 1

        val found = sykmeldingRepository.findBySykmeldingId("to-delete")
        found.shouldBeNull()
    }
}

private fun createSykmelding(
    sykmeldingId: String = "test-id-1",
    pasientIdent: String = "12345678901",
    fom: LocalDate = LocalDate.of(2024, 1, 1),
    tom: LocalDate = LocalDate.of(2024, 1, 15),
) =
    SykmeldingRecord.Digital(
        metadata = MessageMetadata.Digital("12312321"),
        sykmelding =
            Sykmelding.Digital(
                id = sykmeldingId,
                metadata =
                    SykmeldingMeta.Digital(
                        mottattDato = OffsetDateTime.now(),
                        genDate = OffsetDateTime.now(),
                        avsenderSystem = AvsenderSystem(navn = "TestySystemmy", "1.0.0"),
                    ),
                pasient =
                    Pasient(
                        navn = Navn(fornavn = "Forri", mellomnavn = null, etternavn = "Navni"),
                        navKontor = null,
                        navnFastlege = null,
                        fnr = pasientIdent,
                        kontaktinfo = emptyList(),
                    ),
                medisinskVurdering =
                    MedisinskVurdering.Digital(
                        hovedDiagnose = null,
                        biDiagnoser = null,
                        svangerskap = false,
                        yrkesskade = null,
                        skjermetForPasient = false,
                        annenFravarsgrunn = null,
                    ),
                aktivitet =
                    listOf(
                        Aktivitet.Gradert(fom = fom, tom = tom, grad = 90, reisetilskudd = false)
                    ),
                behandler =
                    Behandler(
                        navn = Navn(fornavn = "Beh", mellomnavn = null, etternavn = "Handler"),
                        adresse = null,
                        ids = emptyList(),
                        kontaktinfo = emptyList(),
                    ),
                sykmelder =
                    Sykmelder(
                        ids = emptyList(),
                        helsepersonellKategori = HelsepersonellKategori.HJELPEPLEIER,
                    ),
                arbeidsgiver = ArbeidsgiverInfo.Ingen(),
                tilbakedatering = null,
                bistandNav = null,
                utdypendeSporsmal = null,
            ),
        validation =
            ValidationResult(
                status = RuleType.OK,
                timestamp = OffsetDateTime.now(),
                rules = emptyList(),
            ),
    )

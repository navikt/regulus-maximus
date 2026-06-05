package no.nav.tsm.mottak.sykmelding.service

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.BeforeTest
import kotlinx.coroutines.test.runTest
import no.nav.tsm.mottak.db.*
import no.nav.tsm.mottak.sykmelding.exceptions.SykmeldingMergeValidationException
import no.nav.tsm.mottak.sykmelding.kafka.SykmeldingProducerService
import no.nav.tsm.sykmelding.input.core.model.*
import no.nav.tsm.sykmelding.input.core.model.Pasient
import no.nav.tsm.sykmelding.input.core.model.metadata.*
import no.nav.tsm.sykmelding.input.core.model.metadata.MessageMetadata.Xml.Emottak
import org.apache.kafka.common.header.internals.RecordHeaders
import org.junit.Test

fun SykmeldingRecord.copy(validation: ValidationResult): SykmeldingRecord {
    return toSpecificSykmeldingRecord(sykmelding, metadata, validation)
}

class SykmeldingServiceTest {

    private val sykmeldingRepository: SykmeldingRepository = mockk()
    private val sykmeldingProducer: SykmeldingProducerService = mockk()
    private val sykmeldingService =
        SykmeldingService(
            sykmeldingRepository = sykmeldingRepository,
            sykmeldingProducerService = sykmeldingProducer,
        )

    @BeforeTest
    fun setup() {
        coEvery { sykmeldingRepository.findBySykmeldingId(any()) } returns null
        coEvery { sykmeldingRepository.upsertSykmelding(any()) } returns Unit
        coEvery { sykmeldingProducer.sendToTsmSykmelding(any(), any()) } returns Unit
    }

    @Test
    fun `test only ok sykmelding`() = runTest {
        val sykmeldingRecord =
            getSykmeldingRecord(
                ValidationResult(
                    status = RuleType.OK,
                    timestamp = OffsetDateTime.now(),
                    rules = emptyList(),
                )
            )

        sykmeldingService.updateSykmelding("1", sykmeldingRecord, RecordHeaders())

        coVerify {
            sykmeldingProducer.sendToTsmSykmelding(
                match { it.validation.status == RuleType.OK && it.validation.rules.isEmpty() },
                any(),
            )
        }
    }

    @Test
    fun `test invalid sykmelding`() = runTest {
        val sykmeldingRecord =
            getSykmeldingRecord(
                ValidationResult(
                    status = RuleType.INVALID,
                    timestamp = OffsetDateTime.now(),
                    rules = listOf(invalid()),
                )
            )

        sykmeldingService.updateSykmelding("1", sykmeldingRecord, RecordHeaders())
        coVerify {
            sykmeldingProducer.sendToTsmSykmelding(
                match { record ->
                    record.validation.status == RuleType.INVALID &&
                        record.validation.rules.any { it.type == RuleType.INVALID }
                },
                any(),
            )
        }
    }

    @Test
    fun `test pending sykmelding`() = runTest {
        val sykmeldingRecord =
            getSykmeldingRecord(
                ValidationResult(
                    status = RuleType.PENDING,
                    timestamp = OffsetDateTime.now(),
                    rules = listOf(pending()),
                )
            )

        sykmeldingService.updateSykmelding("1", sykmeldingRecord, RecordHeaders())
        coVerify {
            sykmeldingProducer.sendToTsmSykmelding(
                match { it.validation.status == RuleType.PENDING && it.validation.rules.size == 1 },
                any(),
            )
        }
    }

    @Test
    fun `test sykmelding ok from manuell`() = runTest {
        val okTimestamp = OffsetDateTime.now().plusHours(5)
        val sykmeldingRecord: SykmeldingRecord =
            getSykmeldingRecord(
                ValidationResult(status = RuleType.OK, timestamp = okTimestamp, rules = emptyList())
            )
        val pendingTimeStamp = sykmeldingRecord.sykmelding.metadata.mottattDato

        coEvery { sykmeldingRepository.findBySykmeldingId("1") } returns
            sykmeldingRecord.copy(
                validation =
                    ValidationResult(
                        status = RuleType.PENDING,
                        timestamp = pendingTimeStamp,
                        rules = listOf(pending(timestamp = pendingTimeStamp)),
                    )
            )

        sykmeldingService.updateSykmelding("1", sykmeldingRecord, RecordHeaders())
        coVerify {
            sykmeldingProducer.sendToTsmSykmelding(
                match { record ->
                    record.validation.status == RuleType.OK &&
                        record.validation.rules.size == 2 &&
                        record.validation.rules.singleOrNull {
                            it.type == RuleType.PENDING && it.timestamp.isEqual(pendingTimeStamp)
                        } != null &&
                        record.validation.rules.singleOrNull {
                            it.type == RuleType.OK && it.timestamp.isEqual(okTimestamp)
                        } != null
                },
                any(),
            )
        }
    }

    @Test
    fun `test sykmelding invalid from manuell`() = runTest {
        val invalidTimesamp = OffsetDateTime.now().plusHours(5)
        val sykmeldingRecord =
            getSykmeldingRecord(
                ValidationResult(
                    status = RuleType.INVALID,
                    timestamp = invalidTimesamp,
                    rules =
                        listOf(
                            invalid(
                                validationType = ValidationType.MANUAL,
                                name =
                                    TilbakedatertMerknad.TILBAKEDATERING_UGYLDIG_TILBAKEDATERING
                                        .name,
                                timestamp = invalidTimesamp,
                            )
                        ),
                )
            )

        coEvery { sykmeldingRepository.findBySykmeldingId("1") } returns
            sykmeldingRecord.copy(
                validation =
                    ValidationResult(
                        status = RuleType.PENDING,
                        timestamp = sykmeldingRecord.sykmelding.metadata.mottattDato,
                        rules =
                            listOf(
                                pending(
                                    timestamp = sykmeldingRecord.sykmelding.metadata.mottattDato
                                )
                            ),
                    )
            )

        sykmeldingService.updateSykmelding("1", sykmeldingRecord, RecordHeaders())
        coVerify {
            sykmeldingProducer.sendToTsmSykmelding(
                match { record ->
                    record.validation.status == RuleType.INVALID &&
                        record.validation.rules.size == 2 &&
                        record.validation.rules.any {
                            it.type == RuleType.PENDING &&
                                it.timestamp.isEqual(
                                    sykmeldingRecord.sykmelding.metadata.mottattDato
                                )
                        } &&
                        record.validation.rules.any {
                            it.type == RuleType.INVALID &&
                                it.timestamp.isEqual(invalidTimesamp) &&
                                it.name ==
                                    TilbakedatertMerknad.TILBAKEDATERING_UGYLDIG_TILBAKEDATERING
                                        .name
                        }
                },
                any(),
            )
        }
    }

    @Test
    fun `test both ok and invalid should throw exception`() = runTest {
        val sykmeldingRecord =
            getSykmeldingRecord(
                ValidationResult(
                    status = RuleType.OK,
                    timestamp = OffsetDateTime.now(),
                    rules = listOf(ok(), invalid()),
                )
            )

        shouldThrow<SykmeldingMergeValidationException> {
            sykmeldingService.updateSykmelding("1", sykmeldingRecord, RecordHeaders())
        }
    }

    @Test
    fun `test pending, ok, ok (bug in syfosmmanuell)`() = runTest {
        val pendingTimestamp = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1)
        val firstOkTimestamp = OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(20)
        val secondOkTimestamp = OffsetDateTime.now(ZoneOffset.UTC)
        val sykmeldingRecord =
            getSykmeldingRecord(
                ValidationResult(
                    status = RuleType.OK,
                    timestamp = secondOkTimestamp,
                    rules = emptyList(),
                )
            )

        coEvery { sykmeldingRepository.findBySykmeldingId("1") } returns
            sykmeldingRecord.copy(
                validation =
                    ValidationResult(
                        status = RuleType.OK,
                        timestamp = firstOkTimestamp,
                        rules =
                            listOf(
                                ok(timestamp = firstOkTimestamp),
                                pending(timestamp = pendingTimestamp),
                            ),
                    )
            )

        shouldNotThrowAny {
            sykmeldingService.updateSykmelding("1", sykmeldingRecord, RecordHeaders())
        }
    }
}

private fun getSykmeldingRecord(validation: ValidationResult): SykmeldingRecord {
    return SykmeldingRecord.Xml(
        metadata =
            Emottak.Legacy(
                msgInfo =
                    MessageInfo(
                        Meldingstype.SYKMELDING,
                        genDate = OffsetDateTime.now(),
                        msgId = "1",
                        migVersjon = "1",
                    ),
                sender =
                    Organisasjon(
                        null,
                        OrganisasjonsType.IKKE_OPPGITT,
                        emptyList(),
                        null,
                        null,
                        null,
                        null,
                    ),
                vedlegg = emptyList(),
                receiver =
                    Organisasjon(
                        null,
                        OrganisasjonsType.IKKE_OPPGITT,
                        emptyList(),
                        null,
                        null,
                        null,
                        null,
                    ),
            ),
        sykmelding =
            Sykmelding.Xml(
                id = "1",
                metadata =
                    SykmeldingMeta.Legacy(
                        genDate = OffsetDateTime.now(),
                        mottattDato = OffsetDateTime.now(),
                        behandletTidspunkt = OffsetDateTime.now(),
                        regelsettVersjon = "3",
                        avsenderSystem = AvsenderSystem("TSM", "1.0"),
                        strekkode = "123123123123",
                    ),
                medisinskVurdering =
                    MedisinskVurdering.Legacy(
                        hovedDiagnose = DiagnoseInfo(DiagnoseSystem.ICD10, "T123", "tekst"),
                        biDiagnoser = emptyList(),
                        annenFraversArsak = null,
                        skjermetForPasient = false,
                        yrkesskade = null,
                        syketilfelletStartDato = LocalDate.now(),
                        svangerskap = false,
                    ),
                pasient =
                    Pasient(
                        fnr = "123",
                        navn = null,
                        navKontor = null,
                        navnFastlege = null,
                        kontaktinfo = emptyList(),
                    ),
                aktivitet =
                    listOf(
                        Aktivitet.IkkeMulig(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusDays(7),
                            medisinskArsak = null,
                            arbeidsrelatertArsak = null,
                        ),
                        // This is before the first period on purpose
                        Aktivitet.IkkeMulig(
                            fom = LocalDate.now().minusDays(7),
                            tom = LocalDate.now().minusDays(1),
                            medisinskArsak = null,
                            arbeidsrelatertArsak = null,
                        ),
                    ),
                behandler =
                    Behandler(
                        navn = Navn("Ola", null, "Nordmann"),
                        adresse = null,
                        ids = emptyList(),
                        kontaktinfo = emptyList(),
                    ),
                prognose = null,
                tiltak = null,
                bistandNav = null,
                arbeidsgiver = ArbeidsgiverInfo.Ingen(),
                sykmelder =
                    Sykmelder(
                        ids = emptyList(),
                        helsepersonellKategori = HelsepersonellKategori.LEGE,
                    ),
                tilbakedatering = null,
                utdypendeOpplysninger = null,
            ),
        validation = validation,
    )
}

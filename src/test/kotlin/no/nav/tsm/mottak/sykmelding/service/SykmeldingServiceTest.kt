package no.nav.tsm.mottak.sykmelding.service

import arrow.core.right
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.collections.emptyList
import kotlin.test.BeforeTest
import kotlinx.coroutines.test.runTest
import no.nav.tsm.core.Environment
import no.nav.tsm.ktor.core.SimpleNavn
import no.nav.tsm.mottak.db.*
import no.nav.tsm.mottak.pdl.PdlArrowed
import no.nav.tsm.mottak.sykmelder.Sykmelder
import no.nav.tsm.mottak.sykmelder.SykmelderService
import no.nav.tsm.mottak.sykmelding.exceptions.SykmeldingMergeValidationException
import no.nav.tsm.pdl.Person
import no.nav.tsm.regulus.regula.RegulaJuridiskVurdering
import no.nav.tsm.regulus.regula.RegulaResult
import no.nav.tsm.regulus.regula.RegulaStatus
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
    private val ruleService = mockk<RuleService>()
    private val sykmelderService = mockk<SykmelderService>()
    private val pdlClient = mockk<PdlArrowed>()

    private val env: Environment = mockk(relaxed = true)

    private val sykmeldingService =
        SykmeldingService(
            sykmeldingRepository = sykmeldingRepository,
            sykmeldingProducerService = sykmeldingProducer,
            env = env,
            ruleService = ruleService,
            sykmelderService = sykmelderService,
            pdlClient = pdlClient,
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

    @Test
    fun `test verifyRegulaRules happy path`() = runTest {
        val timestamp = OffsetDateTime.parse("2026-09-30T10:39:08.326777Z")
        val sykmeldingRecord =
            getSykmeldingDigitalRecord(
                ValidationResult(status = RuleType.OK, timestamp = timestamp, rules = emptyList())
            )

        val pasient = mockk<Person>(relaxed = true)
        mockVerifyRegulaMethods(pasient)
        val result = sykmeldingService.verifyRegulaRules(sykmeldingRecord)

        result.shouldNotBeNull()
        result.status shouldBeEqual RegulaStatus.OK
    }

    @Test
    fun `test verifyRegulaRules with invalid message that has been validated wrong in syk-inn`() =
        runTest {
            val timestamp = OffsetDateTime.parse("2026-09-30T10:39:08.326777Z")
            val sykmeldingRecord =
                getSykmeldingDigitalRecord(
                    ValidationResult(
                        status = RuleType.INVALID,
                        timestamp = timestamp,
                        rules = emptyList(),
                    )
                )
            val pasient = mockk<Person>(relaxed = true)
            mockVerifyRegulaMethods(pasient)
            val result = sykmeldingService.verifyRegulaRules(sykmeldingRecord)

            result.shouldNotBeNull()
            result.status shouldBeEqual RegulaStatus.OK
        }

    @Test
    fun `test byIdents with one ident`() = runTest {
        val timestamp = OffsetDateTime.parse("2026-09-30T10:39:08.326777Z")
        val sykmeldingRecord =
            getSykmeldingDigitalRecord(
                ValidationResult(
                    status = RuleType.INVALID,
                    timestamp = timestamp,
                    rules = emptyList(),
                )
            )

        coEvery { sykmeldingRepository.allSykmeldingerLastThreeYearsForIdent(any()) } returns
            listOf(sykmeldingRecord)

        val idents = listOf("21914897936")

        val result = sykmeldingService.byIdents(idents)
        result.fold({}, { result -> result shouldBeEqual listOf(sykmeldingRecord) })
    }

    @Test
    fun `test byIdents with two idents and two sykmeldinger`() = runTest {
        val timestamp = OffsetDateTime.parse("2026-09-30T10:39:08.326777Z")
        val sykmeldingRecord =
            getSykmeldingDigitalRecord(
                ValidationResult(
                    status = RuleType.INVALID,
                    timestamp = timestamp,
                    rules = emptyList(),
                )
            )
        val sykmeldingRecord2 =
            getSykmeldingDigitalRecord(
                ValidationResult(
                    status = RuleType.INVALID,
                    timestamp = timestamp,
                    rules = emptyList(),
                ),
                Pasient(
                    navn = Navn(fornavn = "MATEMATISK", mellomnavn = null, etternavn = "APE"),
                    navKontor = null,
                    navnFastlege = null,
                    fnr = "11111111111",
                    kontaktinfo = emptyList(),
                ),
            )

        coEvery {
            sykmeldingRepository.allSykmeldingerLastThreeYearsForIdent(
                listOf("21914897936", "11111111111")
            )
        } returns listOf(sykmeldingRecord, sykmeldingRecord2)

        val idents = listOf("21914897936", "11111111111")

        val result = sykmeldingService.byIdents(idents)
        result.fold(
            {},
            { result -> result shouldBeEqual listOf(sykmeldingRecord, sykmeldingRecord2) },
        )
    }

    @Test
    fun `test byIdents with two idents and only one sykmelding`() = runTest {
        val timestamp = OffsetDateTime.parse("2026-09-30T10:39:08.326777Z")
        val sykmeldingRecord =
            getSykmeldingDigitalRecord(
                ValidationResult(
                    status = RuleType.INVALID,
                    timestamp = timestamp,
                    rules = emptyList(),
                )
            )

        coEvery {
            sykmeldingRepository.allSykmeldingerLastThreeYearsForIdent(
                listOf("21914897936", "11111111111")
            )
        } returns listOf(sykmeldingRecord)

        val idents = listOf("21914897936", "11111111111")

        val result = sykmeldingService.byIdents(idents)
        result.fold({}, { result -> result shouldBeEqual listOf(sykmeldingRecord) })
    }

    private fun mockVerifyRegulaMethods(pasient: Person) {
        val regulaResult = mockk<RegulaResult>()
        coEvery { pdlClient.getPerson("21914897936") } returns pasient.right()
        coEvery { sykmelderService.byHpr("565501872") } returns
            Sykmelder.MedSuspensjon(
                    hpr = "565501872",
                    navn = SimpleNavn("GRØNN", null, "VITS"),
                    godkjenninger = emptyList(),
                    ident = "05898597468",
                    suspendert = false,
                )
                .right()
        coEvery { sykmeldingRepository.allSykmeldingerLastThreeYearsForIdent(any()) } returns
            emptyList()
        every { regulaResult.status } returns RegulaStatus.OK
        every { ruleService.verify(any(), any(), any(), any()) } returns okRuleResultPair.right()
    }
}

val okRuleResultPair: Pair<RegulaResult, List<RegulaJuridiskVurdering>> =
    RegulaResult.Ok(emptyList()) to emptyList()

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

// TODO: create sykmeldingBuilder
private fun getSykmeldingDigitalRecord(
    validation: ValidationResult,
    pasient: Pasient? = null,
): SykmeldingRecord {
    val timestamp = OffsetDateTime.parse("2026-09-30T10:39:08.326777Z")
    return SykmeldingRecord.Digital(
        metadata = MessageMetadata.Digital("864425208"),
        sykmelding =
            Sykmelding.Digital(
                id = "eb0a9800-b1b2-49e1-b7c8-ccef6ca3ab75",
                metadata =
                    SykmeldingMeta.Digital(
                        mottattDato = timestamp,
                        genDate = timestamp,
                        avsenderSystem = AvsenderSystem(navn = "nav-epj (FHIR)", versjon = "1"),
                    ),
                pasient =
                    pasient
                        ?: Pasient(
                            navn =
                                Navn(fornavn = "MATEMATISK", mellomnavn = null, etternavn = "APE"),
                            navKontor = null,
                            navnFastlege = null,
                            fnr = "21914897936",
                            kontaktinfo = emptyList(),
                        ),
                medisinskVurdering =
                    MedisinskVurdering.Digital(
                        hovedDiagnose = DiagnoseInfo(DiagnoseSystem.ICPC2, "A02", "Frysninger"),
                        biDiagnoser =
                            listOf(
                                DiagnoseInfo(DiagnoseSystem.ICPC2, "A03", "Feber"),
                                DiagnoseInfo(
                                    DiagnoseSystem.ICPC2,
                                    "A01",
                                    "Smerte generell/flere steder",
                                ),
                            ),
                        svangerskap = true,
                        yrkesskade = null,
                        skjermetForPasient = false,
                        annenFravarsgrunn = null,
                    ),
                aktivitet =
                    listOf(
                        Aktivitet.IkkeMulig(
                            fom = LocalDate.parse("2026-09-30"),
                            tom = LocalDate.parse("2026-10-07"),
                            medisinskArsak = null,
                            arbeidsrelatertArsak = null,
                        )
                    ),
                behandler =
                    Behandler(
                        navn = Navn(fornavn = "GRØNN", mellomnavn = null, etternavn = "VITS"),
                        adresse = null,
                        ids =
                            listOf(
                                PersonId("565501872", type = PersonIdType.HPR),
                                PersonId("05898597468", type = PersonIdType.FNR),
                            ),
                        kontaktinfo = emptyList(),
                    ),
                sykmelder =
                    Sykmelder(
                        ids = listOf(PersonId("565501872", type = PersonIdType.HPR)),
                        helsepersonellKategori = HelsepersonellKategori.LEGE,
                    ),
                arbeidsgiver = ArbeidsgiverInfo.Ingen(),
                tilbakedatering = null,
                bistandNav = null,
                utdypendeSporsmal = null,
                prognose = null,
            ),
        validation = validation,
    )
}

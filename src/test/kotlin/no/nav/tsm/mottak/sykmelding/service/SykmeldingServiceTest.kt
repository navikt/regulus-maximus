package no.nav.tsm.mottak.sykmelding.service

import no.nav.tsm.mottak.db.SykmeldingDB
import no.nav.tsm.mottak.db.SykmeldingMapper
import no.nav.tsm.mottak.db.SykmeldingRepository
import no.nav.tsm.mottak.db.invalid
import no.nav.tsm.mottak.db.ok
import no.nav.tsm.mottak.db.pending
import no.nav.tsm.mottak.manuell.ManuellbehandlingService
import no.nav.tsm.mottak.pdl.IDENT_GRUPPE
import no.nav.tsm.mottak.pdl.Ident
import no.nav.tsm.mottak.pdl.PdlClient
import no.nav.tsm.mottak.pdl.Person
import no.nav.tsm.mottak.sykmelding.exceptions.SykmeldingMergeValidationException
import no.nav.tsm.mottak.sykmelding.kafka.objectMapper
import no.nav.tsm.mottak.sykmelding.model.AktivitetIkkeMulig
import no.nav.tsm.mottak.sykmelding.model.AvsenderSystem
import no.nav.tsm.mottak.sykmelding.model.Behandler
import no.nav.tsm.mottak.sykmelding.model.DiagnoseInfo
import no.nav.tsm.mottak.sykmelding.model.DiagnoseSystem
import no.nav.tsm.mottak.sykmelding.model.IngenArbeidsgiver
import no.nav.tsm.mottak.sykmelding.model.MedisinskVurdering
import no.nav.tsm.mottak.sykmelding.model.OKRule
import no.nav.tsm.mottak.sykmelding.model.Pasient
import no.nav.tsm.mottak.sykmelding.model.RuleType
import no.nav.tsm.mottak.sykmelding.model.Sykmelder
import no.nav.tsm.mottak.sykmelding.model.SykmeldingMetadata
import no.nav.tsm.mottak.sykmelding.model.SykmeldingRecord
import no.nav.tsm.mottak.sykmelding.model.TilbakedatertMerknad
import no.nav.tsm.mottak.sykmelding.model.ValidationResult
import no.nav.tsm.mottak.sykmelding.model.ValidationType
import no.nav.tsm.mottak.sykmelding.model.XmlSykmelding
import no.nav.tsm.mottak.sykmelding.model.metadata.EmottakEnkel
import no.nav.tsm.mottak.sykmelding.model.metadata.HelsepersonellKategori
import no.nav.tsm.mottak.sykmelding.model.metadata.Meldingstype
import no.nav.tsm.mottak.sykmelding.model.metadata.MessageInfo
import no.nav.tsm.mottak.sykmelding.model.metadata.Navn
import no.nav.tsm.mottak.sykmelding.model.metadata.Organisasjon
import no.nav.tsm.mottak.sykmelding.model.metadata.OrganisasjonsType
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.RecordMetadata
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.internal.verification.Times
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.postgresql.util.PGobject
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.CompletableFuture

class SykmeldingServiceTest {

    private val pdlClient = mock(PdlClient::class.java)
    private val sykmeldingRepository: SykmeldingRepository = mock()
    private val kafkaProducer : KafkaProducer<String, SykmeldingRecord> = mock()
    private val manuellbehandlingService: ManuellbehandlingService = mock()
    private val sykmeldingService = SykmeldingService(
        sykmeldingRepository, kafkaProducer, pdlClient, manuellbehandlingService, "tsmSykmeldingTopic"
    )
    init {
        Mockito.`when`(pdlClient.getPerson(Mockito.anyString()))
            .thenReturn(Person(null, null, listOf(
                Ident("123", IDENT_GRUPPE.FOLKEREGISTERIDENT, false),
                Ident("123", IDENT_GRUPPE.AKTORID, false)
            )))
        val future: CompletableFuture<RecordMetadata> = mock()
        Mockito.`when`(future.get()).thenReturn(RecordMetadata(null, 0, 0, 0, 0, 0))
        Mockito.`when`(kafkaProducer.send(any())).thenReturn(future)
        Mockito.`when`(manuellbehandlingService.getManuellBehandlingTimestamp(any())).thenReturn(null)
        Mockito.`when`(sykmeldingRepository.findBySykmeldingId(any())).thenReturn(null)
    }
    @Test
    fun `test only ok sykmelding`() {
        val sykmeldingRecord = getSykmeldingRecord(
            ValidationResult(
                status = RuleType.OK,
                timestamp = OffsetDateTime.now(),
                rules = emptyList()
            )
        )



        sykmeldingService.updateSykmelding("1", sykmeldingRecord, null)
        Mockito.verify(kafkaProducer).send(argThat {
            val validation = value().validation
            validation.status == RuleType.OK &&
                    validation.rules.isEmpty()
        })
    }

        @Test
        fun `test invalid sykmelding`() {
            val sykmeldingRecord = getSykmeldingRecord(ValidationResult(
                status = RuleType.INVALID,
                timestamp = OffsetDateTime.now(),
                rules = listOf(
                    invalid(),
                )
            ))
            sykmeldingService.updateSykmelding("1", sykmeldingRecord, null)
            Mockito.verify(kafkaProducer).send(argThat {
                val validation = value().validation
                validation.status == RuleType.INVALID &&
                        validation.rules.singleOrNull { it.type == RuleType.INVALID } != null
            })
        }

        @Test
        fun `test pending sykmelding`() {
            val sykmeldingRecord = getSykmeldingRecord(ValidationResult(
                status = RuleType.PENDING,
                timestamp = OffsetDateTime.now(),
                rules = listOf(pending())
            ))

            sykmeldingService.updateSykmelding("1", sykmeldingRecord, null)
            Mockito.verify(kafkaProducer).send(argThat {
                val validation = value().validation
                validation.status == RuleType.PENDING &&
                        validation.rules.size == 1
            })
        }

        @Test
        fun `test sykmeling ok from manuell`() {
            val sykmeldingRecord = getSykmeldingRecord(
                ValidationResult(
                    status = RuleType.OK,
                    timestamp = OffsetDateTime.now(),
                    rules = emptyList()
                )
            )
            val pendingTimeStamp = sykmeldingRecord.sykmelding.metadata.mottattDato
            val okTimestamp = OffsetDateTime.now()
            Mockito.`when`(manuellbehandlingService.getManuellBehandlingTimestamp(any())).thenReturn(okTimestamp)
            sykmeldingService.updateSykmelding("1", sykmeldingRecord, null)

            Mockito.verify(kafkaProducer).send(argThat {
                val validation = value().validation
                validation.status == RuleType.OK &&
                        validation.rules.size == 2 &&
                        validation.rules.singleOrNull { it.type == RuleType.PENDING && it.timestamp == pendingTimeStamp} != null &&
                        validation.rules.singleOrNull { it.type == RuleType.OK && it.timestamp == okTimestamp} != null
            })
        }

    @Test
    fun `test sykmeling invalid from manuell`() {
        val sykmeldingRecord = getSykmeldingRecord(
            ValidationResult(
                status = RuleType.INVALID,
                timestamp = OffsetDateTime.now(),
                rules = listOf(
                    invalid(validationType = ValidationType.MANUAL, name = TilbakedatertMerknad.TILBAKEDATERING_UGYLDIG_TILBAKEDATERING.name),
                )
            )
        )
        val invalidTimesamp = OffsetDateTime.now()
        Mockito.`when`(manuellbehandlingService.getManuellBehandlingTimestamp(any())).thenReturn(invalidTimesamp)

        sykmeldingService.updateSykmelding("1", sykmeldingRecord, null)

        Mockito.verify(kafkaProducer).send(argThat {
            val validation = value().validation
            validation.status == RuleType.INVALID &&
                    validation.rules.size == 2 &&
                    validation.rules.singleOrNull { it.type == RuleType.PENDING && it.timestamp == sykmeldingRecord.sykmelding.metadata.mottattDato} != null &&
                    validation.rules.singleOrNull { it.type == RuleType.INVALID && it.timestamp == invalidTimesamp && it.name == TilbakedatertMerknad.TILBAKEDATERING_UGYLDIG_TILBAKEDATERING.name } != null
        })
    }

    @Test
    fun `test sykmeling ok from manuell, but also in database`() {
        val sykmeldingRecord = getSykmeldingRecord(
            ValidationResult(
                status = RuleType.OK,
                timestamp = OffsetDateTime.now(),
                rules = emptyList()
            )
        )
        val pendingTimeStamp = sykmeldingRecord.sykmelding.metadata.mottattDato
        val okTimestamp = OffsetDateTime.now()
        Mockito.`when`(manuellbehandlingService.getManuellBehandlingTimestamp(any())).thenReturn(okTimestamp)

        val dbTimestamp = OffsetDateTime.now()
        Mockito.`when`(sykmeldingRepository.findBySykmeldingId("1")).thenReturn(SykmeldingMapper.toSykmeldingDB(sykmeldingRecord.copy(validation = ValidationResult(
            status = RuleType.OK,
            timestamp = dbTimestamp,
            rules = emptyList()
        ))) )

        sykmeldingService.updateSykmelding("1", sykmeldingRecord, null)
        Mockito.verify(sykmeldingRepository, Times(0)).findBySykmeldingId("1")
        Mockito.verify(kafkaProducer).send(argThat {
            val validation = value().validation
            validation.status == RuleType.OK &&
                    validation.rules.size == 2 &&
                    validation.rules.singleOrNull { it.type == RuleType.PENDING && it.timestamp == pendingTimeStamp} != null &&
                    validation.rules.singleOrNull { it.type == RuleType.OK && it.timestamp == okTimestamp} != null
        })
    }

    @Test
    fun `test manuell delvis godkjent`() {
        val sykmeldingRecord = getSykmeldingRecord(
            ValidationResult(
                status = RuleType.OK,
                timestamp = OffsetDateTime.now(),
                rules = listOf(OKRule(
                    timestamp = OffsetDateTime.now(),
                    name = TilbakedatertMerknad.TILBAKEDATERING_DELVIS_GODKJENT.name,
                    validationType = ValidationType.MANUAL
                ))
            )
        )
        val pendingTimeStamp = sykmeldingRecord.sykmelding.metadata.mottattDato
        val okTimestamp = OffsetDateTime.now()
        Mockito.`when`(manuellbehandlingService.getManuellBehandlingTimestamp(any())).thenReturn(okTimestamp)

        Mockito.`when`(sykmeldingRepository.findBySykmeldingId("1")).thenReturn(null)

        sykmeldingService.updateSykmelding("1", sykmeldingRecord, null)
        Mockito.verify(sykmeldingRepository, Times(0)).findBySykmeldingId("1")
        Mockito.verify(kafkaProducer).send(argThat {
            val validation = value().validation
            validation.status == RuleType.OK &&
                    validation.rules.size == 2 &&
                    validation.rules.singleOrNull { it.type == RuleType.PENDING && it.timestamp == pendingTimeStamp} != null &&
                    validation.rules.singleOrNull { it.type == RuleType.OK && it.timestamp == okTimestamp && it.name == TilbakedatertMerknad.TILBAKEDATERING_DELVIS_GODKJENT.name } != null
        })
    }


    @Test
    fun `test both ok and invalid should throw exception`() {
        val sykmeldingRecord = getSykmeldingRecord(
            ValidationResult(
                status = RuleType.OK,
                timestamp = OffsetDateTime.now(),
                rules = listOf(
                    ok(),
                    invalid()
                )
            )
        )

        assertThrows<SykmeldingMergeValidationException> {
            sykmeldingService.updateSykmelding("1", sykmeldingRecord, null)
        }
    }

    @Test
    fun `test pending, ok, ok (bug in syfosmmanuell)`() {
        val pendingTimestamp =  OffsetDateTime.now(ZoneOffset.UTC).minusDays(1)
        val firstOkTimestamp = OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(20)
        val secondOkTimestamp = OffsetDateTime.now(ZoneOffset.UTC)
        val sykmeldingRecord = getSykmeldingRecord(
            ValidationResult(
                status = RuleType.OK,
                timestamp = secondOkTimestamp,
                rules = emptyList()
            )
        )
        Mockito.`when`(sykmeldingRepository.findBySykmeldingId("1")).thenReturn(SykmeldingDB(
            sykmeldingId = "1",
            pasientIdent = "123",
            fom = LocalDate.now(),
            tom = LocalDate.now(),
            generatedDate = OffsetDateTime.now(),
            sykmelding = PGobject().apply { value = "" },
            validation = PGobject().apply {
                value = objectMapper.writeValueAsString(ValidationResult(
                    status = RuleType.OK,
                    timestamp = firstOkTimestamp,
                    rules = listOf(
                        ok(timestamp = firstOkTimestamp),
                        pending(timestamp = pendingTimestamp)
                    )
                ))
            },
            metadata = PGobject().apply { value = "" },
        ))

        assertThrows<SykmeldingMergeValidationException> { sykmeldingService.updateSykmelding("1", sykmeldingRecord, null) }
    }
}



fun getSykmeldingRecord(validation: ValidationResult) : SykmeldingRecord {
    return SykmeldingRecord(
        metadata = EmottakEnkel(
            msgInfo = MessageInfo(
                Meldingstype.SYKMELDING,
                genDate = OffsetDateTime.now(),
                msgId = "1",
                migVersjon = "1",
            ),
            sender = Organisasjon(null, OrganisasjonsType.IKKE_OPPGITT, emptyList(), null, null, null, null),
            vedlegg = emptyList(),
            receiver = Organisasjon(null, OrganisasjonsType.IKKE_OPPGITT, emptyList(), null, null, null, null),
        ),
        sykmelding = XmlSykmelding(
            id = "1",
            metadata = SykmeldingMetadata(
                genDate = OffsetDateTime.now(),
                mottattDato = OffsetDateTime.now(),
                behandletTidspunkt = OffsetDateTime.now(),
                regelsettVersjon = "3",
                avsenderSystem = AvsenderSystem("TSM", "1.0"),
                strekkode = "123123123123",
            ),
            medisinskVurdering = MedisinskVurdering(
                hovedDiagnose = DiagnoseInfo(DiagnoseSystem.ICD10, "T123", "tekst"),
                biDiagnoser = emptyList(),
                annenFraversArsak = null,
                skjermetForPasient = false,
                yrkesskade = null,
                syketilfelletStartDato = LocalDate.now(),
                svangerskap = false,
            ),
            pasient = Pasient(
                fnr = "123",
                navn = null,
                navKontor = null,
                navnFastlege = null,
                kontaktinfo = emptyList()
            ),
            aktivitet = listOf(
                AktivitetIkkeMulig(
                    fom = LocalDate.now(),
                    tom = LocalDate.now(),
                    medisinskArsak = null,
                    arbeidsrelatertArsak = null
                )
            ),
            behandler = Behandler(
                navn = Navn("Ola", null, "Nordmann"),
                adresse = null,
                ids = emptyList(),
                kontaktinfo = emptyList()
            ),
            prognose = null,
            tiltak = null,
            bistandNav = null,
            arbeidsgiver = IngenArbeidsgiver(),
            sykmelder = Sykmelder(
                ids = emptyList(),
                helsepersonellKategori = HelsepersonellKategori.LEGE
            ),
            tilbakedatering = null,
            utdypendeOpplysninger = null

        ),
        validation = validation
    )
}

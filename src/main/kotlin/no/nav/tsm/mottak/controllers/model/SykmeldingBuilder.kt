package no.nav.tsm.mottak.controllers.model

import no.nav.tsm.mottak.sykmelding.kafka.model.*
import no.nav.tsm.mottak.sykmelding.kafka.model.metadata.*
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

fun createNewSykmelding() : SykmeldingMedBehandlingsutfall
{
    return SykmeldingMedBehandlingsutfall(Sykmelding(
        id = UUID.randomUUID().toString(),
        metadata = SykmeldingMetadata(
            msgId = null,
            regelsettVersjon = "1",
            partnerreferanse = null,
            avsenderSystem = AvsenderSystem("", ""),
            mottattDato = OffsetDateTime.now(),
            behandletTidspunkt = OffsetDateTime.now(),
        ),
        pasient = Pasient(
            listOf(PersonId("12345678901", type = PersonIdType.FNR)), null, null, null, null, null, listOf(Kontaktinfo(type = KontaktinfoType.TLF, value = "333333"))),
        behandler = Behandler(

            kontaktInfo = listOf(Kontaktinfo(KontaktinfoType.TLF, "3335455")),
            adresse = Adresse(AdresseType.FOLKEREGISTERADRESSE, null, null, null, null, null, null),
            navn = Navn(fornavn= "AA", etternavn = "BB", mellomnavn = null),
            ids = listOf(PersonId("1344556565", PersonIdType.FNR))
        ),
        arbeidsgiver = EnArbeidsgiver(null, null),
        medisinskVurdering = MedisinskVurdering(hovedDiagnose = null, biDiagnoser = null, svangerskap = false, yrkesskade = false, yrkesskadeDato = null, skjermetForPasient = false, syketilfelletStartDato = null, annenFraversArsak = null),
        prognose = Prognose(arbeidsforEtterPeriode = false, null, null),
        tiltak = null,
        bistandNav = null,
        tilbakedatering = null,
        aktivitet = listOf(AktivitetIkkeMulig(medisinskArsak = MedisinskArsak(null, MedisinskArsakType.ANNET), null, fom = 1.januar(2023), tom = 31.januar(2023))),
        utdypendeOpplysninger = emptyMap(),
        generatedDate = OffsetDateTime.now(),
        signerendeBehandler = SignerendeBehandler(ids = listOf(PersonId(id = "12345678901", type= PersonIdType.FNR)), helsepersonellKategori = HelsepersonellKategori.LEGE)
    ),
    validation = ValidationResult(
        status = RuleType.OK,
        timestamp = OffsetDateTime.now(),
        rules = emptyList()
    ),
    meldingsInformasjon =  Utenlandsk(msgInfo= MeldingMetadata(type = Meldingstype.SYKMELDING, genDate = OffsetDateTime.now(), msgId = "111", migVersjon = null ), sender= Organisasjon(navn = "Hallo As", OrganisasjonsType.PRIVATE_SPESIALISTER_MED_DRIFTSAVTALER, listOf(
        OrgId(id = "1", type = OrgIdType.ENH)
    ), null, null, null, null), receiver=Organisasjon(navn = "Heisann", OrganisasjonsType.IKKE_OPPGITT,  listOf(OrgId(id = "1", type = OrgIdType.ENH)), null, null, null, null), utenlandskSykmelding = UtenlandskSykmeldingInfo(land = "Sverige", folkeRegistertAdresseErBrakkeEllerTilsvarende = false, erAdresseUtland = true )
    )
    )
}

internal fun Int.januar(year: Int) = LocalDate.of(year, 1, this)

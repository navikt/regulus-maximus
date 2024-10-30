package no.nav.tsm.mottak.sykmelding.kafka.model

import java.time.LocalDate
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id
enum class Aktivitetstype {
    AKTIVITET_IKKE_MULIG, AVVENTENDE, BEHANDLINGSDAGER, GRADERT, REISETILSKUDD,
}

@JsonSubTypes(
    Type(AktivitetIkkeMulig::class, name = "AKTIVITET_IKKE_MULIG"),
    Type(Avventende::class, name = "AVVENTENDE"),
    Type(Behandlingsdager::class, name = "BEHANDLINGSDAGER"),
    Type(Gradert::class, name = "GRADERT"),
    Type(Reisetilskudd::class, name = "REISETILSKUDD"))
@JsonTypeInfo(use = Id.NAME, include = PROPERTY, property = "type")
sealed interface Aktivitet {
    val fom: LocalDate
    val tom: LocalDate
    val type: Aktivitetstype
}

data class Behandlingsdager(
    val antallBehandlingsdager: Int,
    override val fom: LocalDate,
    override val tom: LocalDate
) : Aktivitet {
    override val type = Aktivitetstype.BEHANDLINGSDAGER
}

data class Gradert(
    val grad: Int, override val fom: LocalDate, override val tom: LocalDate, val reisetilskudd: Boolean,
) : Aktivitet {
    override val type = Aktivitetstype.GRADERT
}

data class Reisetilskudd(
    override val fom: LocalDate, override val tom: LocalDate
) : Aktivitet {
    override val type = Aktivitetstype.REISETILSKUDD
}

data class Avventende(
    val innspillTilArbeidsgiver: String, override val fom: LocalDate, override val tom: LocalDate
) : Aktivitet {
    override val type = Aktivitetstype.AVVENTENDE
}

data class AktivitetIkkeMulig(
    val medisinskArsak: MedisinskArsak?,
    val arbeidsrelatertArsak: ArbeidsrelatertArsak?,
    override val fom: LocalDate,
    override val tom: LocalDate
) : Aktivitet {
    override val type = Aktivitetstype.AKTIVITET_IKKE_MULIG
}

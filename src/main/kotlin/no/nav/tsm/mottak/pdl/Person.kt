package no.nav.tsm.mottak.pdl

import java.time.LocalDate

enum class IDENT_GRUPPE {
    AKTORID,
    FOLKEREGISTERIDENT,
    NPID,
}

data class Ident(
    val ident: String,
    val gruppe: IDENT_GRUPPE,
    val historisk: Boolean,
)
data class Navn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
)

data class Person(
    val navn: Navn?,
    val foedselsdato: LocalDate?,
    val identer: List<Ident>
)

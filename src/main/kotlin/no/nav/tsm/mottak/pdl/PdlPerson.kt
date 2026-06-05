package no.nav.tsm.mottak.pdl

import java.time.LocalDate

data class PdlPerson(val navn: PdlNavn?, val foedselsdato: LocalDate?, val identer: List<PdlIdent>)

data class PdlNavn(val fornavn: String, val mellomnavn: String?, val etternavn: String)

data class PdlIdent(val ident: String, val gruppe: PdlIdentgruppe, val historisk: Boolean)

enum class PdlIdentgruppe {
    AKTORID,
    FOLKEREGISTERIDENT,
    NPID,
}

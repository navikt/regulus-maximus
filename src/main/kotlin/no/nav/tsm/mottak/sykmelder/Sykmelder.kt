package no.nav.tsm.mottak.sykmelder

import no.nav.tsm.ktor.core.Navn
import no.nav.tsm.mottak.sykmelder.tsmBehandler.SykmelderGodkjenning

sealed interface Sykmelder {
    data class MedSuspensjon(
        val hpr: String,
        val navn: Navn,
        val godkjenninger: List<SykmelderGodkjenning>,
        val ident: String,
        val suspendert: Boolean,
    ) : Sykmelder

    object FinnesIkke : Sykmelder
}

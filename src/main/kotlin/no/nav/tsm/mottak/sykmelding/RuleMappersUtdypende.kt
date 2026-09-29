package no.nav.tsm.mottak.sykmelding

import no.nav.tsm.sykmelding.input.core.model.Sporsmalstype
import no.nav.tsm.sykmelding.input.core.model.UtdypendeSporsmal

fun List<UtdypendeSporsmal>.toRegulaBesvartUtdypende(): List<String> {
    if (
        this.find { it.type.name == Sporsmalstype.MEDISINSKE_HENSYN.name } != null ||
            this.find { it.type.name == Sporsmalstype.FORVENTET_HELSETILSTAND_UTVIKLING.name } !=
                null
    ) {
        return uke39() + uke17() + uke7()
    }

    if (
        this.find { it.type.name == Sporsmalstype.UAVKLARTE_FORHOLD.name } != null ||
            this.find { it.type.name == Sporsmalstype.BEHANDLING_OG_FREMTIDIG_ARBEID.name } != null
    ) {
        return uke17() + uke7()
    }

    // Uke 7 or no values:
    return uke7()
}

fun List<UtdypendeSporsmal>.uke39(): List<String> {
    val sporsmaalId =
        this.filterNot {
                it.type == Sporsmalstype.UAVKLARTE_FORHOLD ||
                    it.type == Sporsmalstype.BEHANDLING_OG_FREMTIDIG_ARBEID ||
                    it.type == Sporsmalstype.HENSYN_PA_ARBEIDSPLASSEN ||
                    it.type == Sporsmalstype.UTFORDRINGER_MED_GRADERT_ARBEID
            }
            .map {
                if (it.type == Sporsmalstype.MEDISINSK_OPPSUMMERING) {
                    it.let { "6.5.1" }
                }
                if (it.type == Sporsmalstype.UTFORDRINGER_MED_ARBEID) {
                    it.let { "6.5.2" }
                }
                if (it.type == Sporsmalstype.FORVENTET_HELSETILSTAND_UTVIKLING) {
                    it.let { "6.5.3" }
                }
                if (it.type == Sporsmalstype.MEDISINSKE_HENSYN) {
                    it.let { "6.5.5" }
                } else {
                    it.let { "remove" }
                }
            }
            .filterNot { it == "remove" }

    return sporsmaalId
}

fun List<UtdypendeSporsmal>.uke17(): List<String> {
    val sporsmalId =
        this.filterNot {
                it.type == Sporsmalstype.MEDISINSKE_HENSYN ||
                    it.type == Sporsmalstype.FORVENTET_HELSETILSTAND_UTVIKLING ||
                    it.type == Sporsmalstype.HENSYN_PA_ARBEIDSPLASSEN ||
                    it.type == Sporsmalstype.UTFORDRINGER_MED_GRADERT_ARBEID
            }
            .map {
                if (it.type == Sporsmalstype.MEDISINSK_OPPSUMMERING) {
                    it.let { "6.4.1" }
                }
                if (it.type == Sporsmalstype.UTFORDRINGER_MED_ARBEID) {
                    it.let { "6.4.2" }
                }
                if (it.type == Sporsmalstype.BEHANDLING_OG_FREMTIDIG_ARBEID) {
                    it.let { "6.4.3" }
                }
                if (it.type == Sporsmalstype.UAVKLARTE_FORHOLD) {
                    it.let { "6.4.4" }
                } else {
                    it.let { "remove" }
                }
            }
            .filterNot { it == "remove" }
    return sporsmalId
}

fun List<UtdypendeSporsmal>.uke7(): List<String> {
    val sporsmaalId =
        this.filter {
                it.type == Sporsmalstype.UTFORDRINGER_MED_ARBEID ||
                    it.type == Sporsmalstype.BEHANDLING_OG_FREMTIDIG_ARBEID ||
                    it.type == Sporsmalstype.UAVKLARTE_FORHOLD ||
                    it.type == Sporsmalstype.FORVENTET_HELSETILSTAND_UTVIKLING ||
                    it.type == Sporsmalstype.MEDISINSKE_HENSYN
            }
            .map {
                if (it.type == Sporsmalstype.MEDISINSK_OPPSUMMERING) {
                    it.let { "6.3.1" }
                }
                if (it.type == Sporsmalstype.UTFORDRINGER_MED_GRADERT_ARBEID) {
                    it.let { "6.3.2" }
                }
                if (it.type == Sporsmalstype.HENSYN_PA_ARBEIDSPLASSEN) {
                    it.let { "6.3.3" }
                } else {
                    it.let { "remove" }
                }
            }
            .filterNot { it == "remove" }

    return sporsmaalId
}

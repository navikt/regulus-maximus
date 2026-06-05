package no.nav.tsm.admin

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import no.nav.tsm.core.db.dbQuery
import no.nav.tsm.mottak.db.SykmeldingTable
import no.nav.tsm.mottak.db.toSpecificSykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.r2dbc.selectAll

enum class SykmeldingHistoryRanges {
    LAST_1_YEARS,
    LAST_3_YEARS,
    LAST_10_YEARS,
}

class AdminSykmeldingRepo {
    suspend fun allByUser(
        userIdent: String,
        range: SykmeldingHistoryRanges,
    ): List<SykmeldingRecord> = dbQuery {
        val earliestFom =
            when (range) {
                SykmeldingHistoryRanges.LAST_1_YEARS -> java.time.LocalDate.now().minusYears(1)
                SykmeldingHistoryRanges.LAST_3_YEARS -> java.time.LocalDate.now().minusYears(3)
                SykmeldingHistoryRanges.LAST_10_YEARS -> java.time.LocalDate.now().minusYears(10)
            }

        SykmeldingTable.selectAll()
            .where {
                (SykmeldingTable.pasientIdent eq userIdent) and
                    (SykmeldingTable.fom greaterEq earliestFom)
            }
            .map {
                toSpecificSykmeldingRecord(
                    sykmelding = it[SykmeldingTable.sykmelding],
                    metadata = it[SykmeldingTable.metadata],
                    validation = it[SykmeldingTable.validation],
                )
            }
            .toList()
    }
}

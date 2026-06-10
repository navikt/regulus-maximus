package no.nav.tsm.admin.datefixer

import java.time.LocalDate
import no.nav.tsm.core.db.dbQuery
import no.nav.tsm.mottak.db.SykmeldingTable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.update

class DateFixerRepo {
    suspend fun isDateDiff(
        key: String,
        actualEarliest: LocalDate,
        actualLatest: LocalDate,
    ): Boolean = dbQuery {
        SykmeldingTable.select(SykmeldingTable.sykmeldingId)
            .where {
                (SykmeldingTable.sykmeldingId eq key) and
                    ((SykmeldingTable.fom neq actualEarliest) or
                        (SykmeldingTable.tom neq actualLatest))
            }
            .count() > 0
    }

    suspend fun upDate(key: String, actualEarliest: LocalDate, actualLatest: LocalDate) = dbQuery {
        SykmeldingTable.update({ SykmeldingTable.sykmeldingId eq key }) {
            it[SykmeldingTable.fom] = actualEarliest
            it[SykmeldingTable.tom] = actualLatest
        }
    }
}

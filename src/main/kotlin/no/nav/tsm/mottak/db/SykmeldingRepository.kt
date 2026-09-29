package no.nav.tsm.mottak.db

import java.time.LocalDate
import java.time.OffsetDateTime
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import no.nav.tsm.core.db.dbQuery
import no.nav.tsm.sykmelding.input.core.model.Aktivitet
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.upsert

class SykmeldingRepository {
    suspend fun findBySykmeldingId(sykmeldingId: String): SykmeldingRecord? = dbQuery {
        SykmeldingTable.selectAll()
            .where { SykmeldingTable.sykmeldingId eq sykmeldingId }
            .map {
                toSpecificSykmeldingRecord(
                    sykmelding = it[SykmeldingTable.sykmelding],
                    metadata = it[SykmeldingTable.metadata],
                    validation = it[SykmeldingTable.validation],
                )
            }
            .firstOrNull()
    }

    suspend fun upsertSykmelding(sykmelding: SykmeldingRecord) =
        dbQuery<Unit> {
            SykmeldingTable.upsert(
                where = { SykmeldingTable.sykmeldingId eq sykmelding.sykmelding.id },
                onUpdateExclude = listOf(SykmeldingTable.sykmeldingId),
            ) {
                it[SykmeldingTable.sykmeldingId] = sykmelding.sykmelding.id
                it[SykmeldingTable.pasientIdent] = sykmelding.sykmelding.pasient.fnr
                it[SykmeldingTable.fom] = sykmelding.sykmelding.aktivitet.earliestFom()
                it[SykmeldingTable.tom] = sykmelding.sykmelding.aktivitet.latestTom()
                it[SykmeldingTable.generatedDate] = sykmelding.sykmelding.metadata.genDate
                it[SykmeldingTable.sykmelding] = sykmelding.sykmelding
                it[SykmeldingTable.validation] = sykmelding.validation
                it[SykmeldingTable.metadata] = sykmelding.metadata
            }
        }

    suspend fun deleteBySykmeldingId(sykmeldingId: String): Int = dbQuery {
        SykmeldingTable.deleteWhere { SykmeldingTable.sykmeldingId eq sykmeldingId }
    }

    // TODO input parameter should be a list of idents, due to ident my change over time
    suspend fun allSykmeldingerLastThreeYearsForIdent(ident: String): List<SykmeldingRecord> =
        dbQuery {
            SykmeldingTable.selectAll()
                .where {
                    SykmeldingTable.pasientIdent eq ident
                    SykmeldingTable.generatedDate greaterEq OffsetDateTime.now().minusYears(3)
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

private fun List<Aktivitet>.earliestFom(): LocalDate = minBy { it.fom }.fom

private fun List<Aktivitet>.latestTom(): LocalDate = maxBy { it.tom }.tom

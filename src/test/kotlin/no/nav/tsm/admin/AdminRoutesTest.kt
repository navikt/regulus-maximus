package no.nav.tsm.admin

import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.di.*
import io.ktor.server.testing.*
import kotlin.test.Test
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.utils.WithPostgresql
import no.nav.tsm.utils.configurePostgresIntegrationTests
import no.nav.tsm.utils.testClient

class AdminRoutesTest : WithPostgresql() {

    private fun ApplicationTestBuilder.configureAdminRoutesTest() {
        client = testClient()

        application {
            configurePostgresIntegrationTests(postgres)

            // Bare minimum needed to test admin routes
            dependencies { provide(AdminSykmeldingRepo::class) }
            configureAdminRoutes()
        }

        runMigrations(true)
        connect()
    }

    @Test
    fun `bad path parameter should return bad request with reason`() = testApplication {
        configureAdminRoutesTest()

        val response =
            client.post("/internal/admin/sykmelding-history/LAST_6.9_YEARS") {
                headers { append("Content-Type", "application/json") }
                setBody("""{"userIdent": "very-identy"}""")
            }

        response.status shouldBe HttpStatusCode.BadRequest
        response.body<String>() shouldBe
            "Invalid range parameter, must be one of LAST_1_YEARS, LAST_3_YEARS, LAST_10_YEARS"
    }

    @Test
    fun `sanity check with correct param and correct body, hit the DB get empty list`() =
        testApplication {
            configureAdminRoutesTest()

            val response =
                client.post("/internal/admin/sykmelding-history/LAST_3_YEARS") {
                    headers { append("Content-Type", "application/json") }
                    setBody("""{"userIdent": "very-identy"}""")
                }

            response.status shouldBe HttpStatusCode.OK
            response.body<List<SykmeldingRecord>>() shouldBe emptyList()
        }
}

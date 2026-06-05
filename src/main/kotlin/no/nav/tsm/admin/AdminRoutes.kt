package no.nav.tsm.admin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.di.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.tsm.core.logger
import no.nav.tsm.plugins.auth.INTERNAL_SYMFONI_AUTH
import no.nav.tsm.plugins.auth.internalSymfoniUser
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord

fun Application.configureAdminRoutes() {
    val logger = logger()
    val adminSykmeldingRepo: AdminSykmeldingRepo by dependencies

    routing {
        authenticate(INTERNAL_SYMFONI_AUTH) {
            route("/internal/admin") {
                post("/sykmelding-history/{range}") {
                    val userIdent =
                        call.getUserIdentInBody()
                            ?: return@post call.respond(
                                HttpStatusCode.BadRequest,
                                "Missing userIdent in request body",
                            )
                    val range =
                        call.getRangeFromPathParam()
                            ?: return@post call.respond(
                                HttpStatusCode.BadRequest,
                                invalidRangeError,
                            )

                    val principal = internalSymfoniUser()
                    logger.info(
                        "User ${principal.userId} requested sykmelding history (${range.name}) for a user"
                    )

                    try {
                        val sykmeldinger: List<SykmeldingRecord> =
                            adminSykmeldingRepo.allByUser(userIdent, range)

                        call.respond(HttpStatusCode.OK, sykmeldinger)
                    } catch (e: Exception) {
                        logger.error("Error fetching sykmelding history for user", e)
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            "An error occurred while fetching sykmelding history",
                        )
                    }
                }
            }
        }
    }
}

private val invalidRangeError =
    "Invalid range parameter, must be one of ${
        SykmeldingHistoryRanges.entries.joinToString(
            ", "
        )
    }"

private fun RoutingCall.getRangeFromPathParam(): SykmeldingHistoryRanges? =
    try {
        this.parameters["range"]?.let { SykmeldingHistoryRanges.valueOf(it) }
    } catch (_: Exception) {
        null
    }

private suspend fun RoutingCall.getUserIdentInBody(): String? =
    this.receive<Map<String, String>>().let { it["userIdent"] }

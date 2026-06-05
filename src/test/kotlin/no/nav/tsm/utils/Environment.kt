package no.nav.tsm.utils

import io.mockk.mockk
import no.nav.tsm.core.Environment
import no.nav.tsm.core.ExternalApi
import no.nav.tsm.core.Runtime
import no.nav.tsm.core.RuntimeEnvironments
import no.nav.tsm.core.Texas

val simpleUnitTestEnvironment =
    Environment(
        runtime = Runtime(env = RuntimeEnvironments.PROD, name = "test-app"),
        texas = { Texas(tokenEndpoint = "https://test.token.endpoint") },
        kafka = mockk(relaxed = true),
        postgres = mockk(relaxed = true),
        external = { ExternalApi(tsmPdlCache = "https://test.pdlcache.endpoint") },
        auth = mockk(relaxed = true),
    )

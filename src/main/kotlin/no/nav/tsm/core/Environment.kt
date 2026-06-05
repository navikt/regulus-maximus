package no.nav.tsm.core

import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.plugins.di.*
import java.util.*
import kotlin.time.Duration

enum class RuntimeEnvironments(val nais: String) {
    LOCAL("local"),
    DEV("dev-gcp"),
    PROD("prod-gcp"),
}

class Runtime(val env: RuntimeEnvironments, val name: String)

class PostgresR2DBCConfig(val url: String, val sslCert: String?, val sslKeyPk8: String?)

class PostgresConfig(
    val jdbc: String,
    val r2: PostgresR2DBCConfig,
    val username: String,
    val password: String,
)

class Texas(val tokenEndpoint: String)

class ExternalApi(val tsmPdlCache: String)

class EntraAuth(val issuer: String, val jwksUri: String, val audience: String)

class Auth(val entra: EntraAuth)

class KafkaSykmeldingConsumer(val longPoll: Duration)

class KafkaConfig(val config: Properties, val sykmeldingInputConsumer: KafkaSykmeldingConsumer)

class Environment(
    val runtime: Runtime,
    val kafka: KafkaConfig,
    val postgres: PostgresConfig,
    val texas: () -> Texas,
    val external: () -> ExternalApi,
    val auth: () -> Auth,
)

fun initializeEnvironment(config: ApplicationConfig): Environment {
    val kafkaProperties =
        KafkaConfig(
            config =
                Properties().apply {
                    config.config("kafka.config").toMap().forEach { this[it.key] = it.value }
                },
            sykmeldingInputConsumer =
                KafkaSykmeldingConsumer(
                    longPoll = config.property("kafka.sykmeldingInputConsumer.longPoll").getAs()
                ),
        )

    return Environment(
        runtime =
            Runtime(
                env = config.inferRuntimeEnvironment(),
                name = config.property("app.name").getString(),
            ),
        kafka = kafkaProperties,
        postgres =
            PostgresConfig(
                jdbc = config.property("postgres.jdbc").getString(),
                r2 =
                    PostgresR2DBCConfig(
                        url = config.property("postgres.r2dbc.url").getString(),
                        sslCert = config.propertyOrNull("postgres.r2dbc.sslCert")?.getString(),
                        sslKeyPk8 = config.propertyOrNull("postgres.r2dbc.sslKeyPk8")?.getString(),
                    ),
                username = config.property("postgres.username").getString(),
                password = config.property("postgres.password").getString(),
            ),
        texas = {
            Texas(tokenEndpoint = config.property("external.texas.tokenEndpoint").getString())
        },
        external = {
            ExternalApi(tsmPdlCache = config.property("external.tsmPdlCache").getString())
        },
        auth = {
            Auth(
                entra =
                    EntraAuth(
                        audience = config.property("auth.entra.audience").getString(),
                        jwksUri = config.property("auth.entra.openid.jwks").getString(),
                        issuer = config.property("auth.entra.openid.issuer").getString(),
                    )
            )
        },
    )
}

fun Application.isLocal(): Boolean {
    val env: Environment by dependencies

    return env.runtime.env == RuntimeEnvironments.LOCAL
}

private fun ApplicationConfig.inferRuntimeEnvironment(): RuntimeEnvironments {
    return when (val configEnv = this.property("app.runtime").getString()) {
        "local" -> RuntimeEnvironments.LOCAL
        "prod-gcp" -> RuntimeEnvironments.PROD
        "dev-gcp" -> RuntimeEnvironments.DEV
        else -> {
            throw IllegalStateException(
                "Unexpected 'app.runtime' configuration: ${configEnv}. Should be one of 'local', 'dev-gcp' or 'prod-gcp'"
            )
        }
    }
}

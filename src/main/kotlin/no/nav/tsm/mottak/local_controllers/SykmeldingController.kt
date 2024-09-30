package no.nav.tsm.mottak.controller

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import no.nav.tsm.mottak.sykmelding.kafka.model.SykmeldingInput
import no.nav.tsm.mottak.sykmelding.kafka.model.SykmeldingMedUtfall
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.*

@RestController
class SykmeldingController(
    private val kafkaTemplate: KafkaTemplate<String, SykmeldingMedUtfall>,
    private val sykmeldingService: SykmeldingService
) {

    private val topic = "tsm.mottak-sykmelding"
    private val logger = LoggerFactory.getLogger(SykmeldingController::class.java)


    @GetMapping("/")
    fun indexPage(): Mono<String> {
        return Mono.just(createHTML().html {
            head {
                title("Regulus Maximus: Dev Tools")

                script(src = "https://unpkg.com/htmx.org@1.9.10") {}
                script(src = "https://unpkg.com/htmx.org@1.9.10/dist/ext/remove-me.js") {}

                link(rel = "stylesheet", href = "https://unpkg.com/reset-css@5.0.2/reset.css")
                link(rel = "stylesheet", href = "/styles.css")
            }

            body {
                header {
                    h1 { +"Regulus Maximus: The DevTools" }
                    img { src = "https://emoji.slack-edge.com/T5LNAMWNA/hekkanphone/6bff158c0c8c17e0.png" }
                }

                main {
                    div(classes = "message-poster") {
                        h2 { +"Post a sykmelding-raw to the Kafka Producer" }
                        button {
                            // remove it after 10 seconds
                            attributes["hx-post"] = "/htmx/post-sykmelding"
                            attributes["hx-trigger"] = "click"
                            attributes["hx-target"] = "#sykmelding-posted"
                            attributes["hx-swap"] = "beforeend"

                            +"Post sykmelding"
                        }
                        div {
                            attributes["hx-ext"] = "remove-me"
                            id = "sykmelding-posted"
                        }
                    }

                    div(classes = "message-inspecter") {
                        h2 { +"Load last 10 messages posted to Sykmelding m/ utfall topic" }

                        button {
                            attributes["hx-get"] = "/htmx/list-example"
                            attributes["hx-target"] = "#last-ten-messages"
                            attributes["hx-swap"] = "innerHTML"
                            +"Get messages"
                        }
                        div { id = "last-ten-messages" }
                    }
                }
            }
        })
    }

    @GetMapping("/styles.css")
    fun serveStyles(): Mono<String> {
        val globalCss = """
            html { font-family: sans-serif; }
            body { margin: 0; }
            main { padding: 16px; display: grid; gap: 16px; grid-template-columns: 1fr 1fr; }
            header { border-bottom: 1px solid #ccc; height: 68px; display: flex; align-items: center; justify-content: space-between; padding-left: 22px; }
            h1 { font-size: 1.5em; font-weight: bold; margin-bottom: 8px; }
            button { padding: 8px 16px; border: none; background-color: #007bff; color: white; cursor: pointer; border-radius: 4px; }
            .success-feedback { background-color: #28a745; color: white; margin: 8px; padding: 8px; border-radius: 4px; max-width: 65ch; }
            .sykmelding-item {
                margin-bottom: 10px;  /* Legger til 10px mellom hvert element */
            }
        """.trimIndent()

        return Mono.just(globalCss)
    }

    @GetMapping("/htmx/list-example")
    fun listExample(): Flux<String> {
        logger.info("Fetching sykmeldinger from cache")

        val lastTenSykmeldinger = sykmeldingService.getAllSykmeldinger()
            .takeLast(10)

        return Flux.fromIterable(lastTenSykmeldinger)
            .map { (id, sykmelding) ->
                """
            <div>
                <p>Sykmelding ID: ${id.sykmeldingId}</p>
                <p>Utfall: ${sykmelding}</p>
                <br/>
            </div>
            """.trimIndent()
            }
    }

    @PostMapping("/htmx/post-sykmelding")
    fun postSykmelding(): Mono<String> {
        logger.info("Sending sykmelding...")
        val sykmeldingId = UUID.randomUUID().toString()
        val sykmelding = SykmeldingInput(sykmeldingId)
        val sykmeldingMedUtfall = SykmeldingMedUtfall(sykmelding, utfall = "Venter på behandling")

        logger.info("Sykmelding ID: $sykmeldingId | Utfall: ${sykmeldingMedUtfall.utfall}")

        return Mono.fromCallable {
            kafkaTemplate.send(topic, sykmeldingId, sykmeldingMedUtfall).get()
        }
            .thenReturn(
                """
            <div>
                <p>Sykmelding ID: ${sykmeldingId}</p>
                <p>Utfall: ${sykmeldingMedUtfall.utfall}</p>
                <br/>
            </div>
            """.trimIndent()
            )
            .onErrorResume { error ->
                Mono.just("Failed to post sykmelding: ${error.message}")
            }
    }
}

@Service
class SykmeldingService(
) {
    private val logger = LoggerFactory.getLogger(SykmeldingService::class.java)

    private val sykmeldingCache: MutableList<SykmeldingMedUtfall> = mutableListOf()

    @KafkaListener(topics = ["\${spring.kafka.topics.mottatt-sykmelding}"], groupId = "\${spring.kafka.group-id}")
    fun consume(cr: ConsumerRecord<String, String>?) {
        if (cr != null) {
            logger.info("konsumerte. ${cr.value()}")
            logger.info("Consumed message from Kafka: ${cr.value()}")
            sykmeldingCache.add(objectMapper.readValue(cr.value()))
        }

    }

    fun getAllSykmeldinger(): MutableList<SykmeldingMedUtfall> {
        logger.info("ALLE SYKMELINDGE $sykmeldingCache")
        return sykmeldingCache
    }
}

val objectMapper: ObjectMapper =
    ObjectMapper().apply {
        registerKotlinModule()
        registerModule(JavaTimeModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }
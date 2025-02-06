package no.nav.tsm.mottak.controllers

import kotlinx.html.*
import kotlinx.html.stream.createHTML
import kotlinx.html.title
import no.nav.tsm.mottak.controllers.model.createNewSykmelding
import no.nav.tsm.mottak.sykmelding.kafka.model.SykmeldingRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@Profile("local")
@RestController
class SykmeldingController(
    private val kafkaTemplate: KafkaProducer<String, SykmeldingRecord>,
) {

    @Value("\${spring.kafka.topics.sykmeldinger-input}")
    private lateinit var topic: String
    private val logger = LoggerFactory.getLogger(SykmeldingController::class.java)


    @GetMapping("/")
    fun indexPage(): String {
        return createHTML().html {
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
        }
    }

    @GetMapping("/styles.css")
    fun serveStyles(): String {
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

        return globalCss
    }

    @PostMapping("/htmx/post-sykmelding")
    fun postSykmelding(): String {
        val sykmeldingMedBehandlingsutfall = createNewSykmelding()
        val sykmeldingId = sykmeldingMedBehandlingsutfall.sykmelding.id
        logger.info("Sending sykmelding med id... ${sykmeldingId} ")
           kafkaTemplate.send(
                ProducerRecord(topic, sykmeldingId, sykmeldingMedBehandlingsutfall)
            )
        return """
            <div>
                <p>Sykmelding ID: ${sykmeldingId}</p>
                <p>Utfall: ${sykmeldingMedBehandlingsutfall.validation.status}</p>
                <br/>
            </div>
            """.trimIndent()
    }
}

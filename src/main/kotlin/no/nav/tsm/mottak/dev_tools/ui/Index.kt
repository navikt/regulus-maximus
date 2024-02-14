import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*
import no.nav.tsm.mottak.example.ExampleService
import no.nav.tsm.mottak.example.ExposedExample
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.UUID


fun Routing.indexPageRoute(kafkaProducer: KafkaProducer<String, String>) {
    get("/") { call.respondHtml(HttpStatusCode.OK) { indexPage() } }

    get("/styles.css") { call.respondText(globalCss, ContentType.Text.CSS) }

    get("/htmx/list-example") {
        val items = ExampleService.getAll()
        call.respondHtml(HttpStatusCode.OK) { listExample(items) }
    }

    post("/htmx/post-sykmelding") {
        val someNumber = (0..100).random()
        kafkaProducer.send(
            ProducerRecord(
                "sykmelding-input",
                UUID.randomUUID().toString(),
                "Some message with random number: $someNumber"
            )
        ).get()
        ExampleService.create(
            ExposedExample(
                text = "Some text with random number: ${System.currentTimeMillis()}",
                someNumber = someNumber
            )
        )

        call.respondHtml {
            body {
                div(classes = "success-feedback") {
                    attributes["remove-me"] = "5s"
                    +"Sykmelding \"posted\" to Kafka, with random number: $someNumber"
                }
            }
        }
    }
}

fun HTML.indexPage() {
    head {
        title("Regulus Maximus: Dev Tools")

        script(src = "https://unpkg.com/htmx.org@1.9.10") {}
        script(src = "https://unpkg.com/htmx.org@1.9.10/dist/ext/remove-me.js") {}

        link(rel = "stylesheet", href = "https://unpkg.com/reset-css@5.0.2/reset.css")
        link(rel = "stylesheet", href = "/styles.css")
    }

    body {
        header {
            h1 { +"Regulus Maximus:  The DevTools" }
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
                    // also remove "aria-hidden" attribute after click
                    attributes["hx-"]
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

fun HTML.listExample(items: List<ExposedExample>) {
    body { ul { items.map { li { +"${it.text} - ${it.someNumber}" } } } }
}

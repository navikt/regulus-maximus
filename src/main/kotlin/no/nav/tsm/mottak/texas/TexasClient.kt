package no.nav.tsm.mottak.texas

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.net.URI

data class TexasResponse(
    val access_token: String,
    val expires_in: Int,
    val token_type: String,
)

data class TexasRequest(
    val identity_provider: String,
    val target: String,
)

@Component
class TexasClient(private val restTemplate: RestTemplate,
                @Value("\${nais.cluster}") private val cluster: String,
                @Value("\${nais.texas.endpoint}") private val endpoint: String) {

    fun getTexasToken(service: String): TexasResponse {
        val texasRequest = TexasRequest(
            identity_provider = "azuread",
            target = "api://$cluster.$service/.default"
        )
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }

        val requestEntity = RequestEntity
            .post(URI(endpoint)) // Endre til riktig URL
            .headers(headers)
            .body(texasRequest)

        return restTemplate.exchange(requestEntity, TexasResponse::class.java).body
            ?: throw RuntimeException("Failed to get Texas response")
    }
}

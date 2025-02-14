package no.nav.tsm.mottak.pdl

import no.nav.tsm.mottak.texas.TexasClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.RequestEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.net.URI

@Component
class PdlClient(
    private val texasClient: TexasClient,
    private val restTemplate: RestTemplate,
    @Value("\${tsm.pdl.url}") private val tsmPdlCacheUrl: String,
    @Value("\${tsm.pdl.service}") private val tsmPdlCacheService: String) {

    fun getPerson(ident: String): Person {

        val texasToken = texasClient.getTexasToken(tsmPdlCacheService)
        val headers = HttpHeaders().apply {
            setBearerAuth(texasToken.access_token)
            set("ident", ident)
        }

        val requestEntity = RequestEntity
            .get(URI("$tsmPdlCacheUrl/api/person"))
            .headers(headers)
            .build()

        try {
            return restTemplate.exchange(requestEntity, Person::class.java).body ?: throw PersonNotFoundException("Failed to get person from PDL cache")
        } catch (e: HttpClientErrorException.NotFound) {
            throw PersonNotFoundException("Could not find person in pdl cache")
        } catch (e: Exception) {
            throw RuntimeException("Failed to get person from PDL cache", e)
        }
    }
}

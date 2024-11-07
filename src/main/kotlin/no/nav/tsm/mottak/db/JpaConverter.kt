package no.nav.tsm.mottak.db

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.io.IOException


@Converter(autoApply = true)
class JpaConverter : AttributeConverter<Any?, String?> {
    override fun convertToDatabaseColumn(meta: Any?): String? {
        return try {
            objectMapper.writeValueAsString(meta)
        } catch (ex: JsonProcessingException) {
            null
        }
    }

    override fun convertToEntityAttribute(dbData: String?): Any? {
        return try {
            objectMapper.readValue(dbData, Any::class.java)
        } catch (ex: IOException) {
            null
        }
    }

    companion object {
        private val objectMapper = ObjectMapper()
    }
}
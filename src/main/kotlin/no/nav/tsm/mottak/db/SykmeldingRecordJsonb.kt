package no.nav.tsm.mottak.db

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlin.reflect.KClass
import no.nav.tsm.sykmelding.input.core.model.CustomDeserializer
import no.nav.tsm.sykmelding.input.core.model.SykmeldingModule
import no.nav.tsm.sykmelding.input.core.model.metadata.MessageMetadata
import no.nav.tsm.sykmelding.input.core.model.metadata.MetadataType
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.json.jsonb

class MessageMetadataDeserializer : CustomDeserializer<MessageMetadata>() {
    override fun getClass(type: String): KClass<out MessageMetadata> {
        return when (MetadataType.valueOf(type)) {
            MetadataType.ENKEL -> MessageMetadata.Xml.Emottak.Legacy::class
            MetadataType.EMOTTAK -> MessageMetadata.Xml.Emottak.EDI::class
            MetadataType.EGENMELDT -> MessageMetadata.Xml.Egenmeldt::class
            MetadataType.DIGITAL -> MessageMetadata.Digital::class
            MetadataType.UTENLANDSK_SYKMELDING -> MessageMetadata.Utenlandsk::class
            MetadataType.PAPIRSYKMELDING -> MessageMetadata.Papir::class
        }
    }
}

class MetadataModule : SimpleModule() {
    init {
        addDeserializer(MessageMetadata::class.java, MessageMetadataDeserializer())
    }
}

val sykmeldingRecordMapper =
    jacksonObjectMapper().apply {
        registerModule(SykmeldingModule())
        registerModule(MetadataModule())
        registerModule(JavaTimeModule())

        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }

inline fun <reified Type : Any> Table.jacksonJsonb(name: String): Column<Type> {
    return jsonb(
        name,
        { sykmeldingRecordMapper.writeValueAsString(it) },
        { sykmeldingRecordMapper.readValue<Type>(it) },
    )
}

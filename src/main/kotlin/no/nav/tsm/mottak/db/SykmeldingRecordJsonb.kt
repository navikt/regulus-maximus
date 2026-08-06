package no.nav.tsm.mottak.db

import kotlin.reflect.KClass
import no.nav.tsm.sykmelding.input.core.model.CustomDeserializer
import no.nav.tsm.sykmelding.input.core.model.SykmeldingModule
import no.nav.tsm.sykmelding.input.core.model.metadata.MessageMetadata
import no.nav.tsm.sykmelding.input.core.model.metadata.MetadataType
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.json.jsonb
import tools.jackson.databind.module.SimpleModule
import tools.jackson.module.kotlin.jacksonMapperBuilder
import tools.jackson.module.kotlin.readValue

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
    jacksonMapperBuilder().addModules(SykmeldingModule(), MetadataModule()).build()

inline fun <reified Type : Any> Table.jacksonJsonb(name: String): Column<Type> {
    return jsonb(
        name,
        { sykmeldingRecordMapper.writeValueAsString(it) },
        { sykmeldingRecordMapper.readValue<Type>(it) },
    )
}

package no.nav.tsm.mottak.sykmelding.kafka.util

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.tsm.mottak.sykmelding.kafka.model.*
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class SykmeldingModule : SimpleModule() {
    override fun setupModule(context: SetupContext)
    {
        addDeserializer(Aktivitet::class.java, AktivitetDeserializer())
        addDeserializer(ArbeidsgiverInfo::class.java, ArbeidsgiverInfoDeserializer())
        addDeserializer(IArbeid::class.java, IArbeidDeserializer())
        addDeserializer(Rule::class.java, RuleDeserializer())
     }
}


abstract class CustomDeserializer<T : Any> : JsonDeserializer<T>() {
    abstract fun getClass(type: String): KClass<out T>

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): T {
        val node: ObjectNode = p.codec.readTree(p)
        val type = node.get("type").asText()
        val clazz = getClass(type)
        return p.codec.treeToValue(node, clazz.java)
    }
}

class RuleDeserializer : CustomDeserializer<Rule>() {

    override fun getClass(type: String): KClass<out Rule> {
        return when (RuleType.valueOf(type)) {
            RuleType.INVALID -> InvalidRule::class
            RuleType.PENDING -> PendingRule::class
            RuleType.OK -> OKRule::class
        }
    }
}

class IArbeidDeserializer : CustomDeserializer<IArbeid>() {
    override fun getClass(type: String): KClass<out IArbeid> {
        return when (IArbeidType.valueOf(type)) {
            IArbeidType.ER_I_ARBEID -> ErIArbeid::class
            IArbeidType.ER_IKKE_I_ARBEID -> ErIkkeIArbeid::class
        }
    }
}


class ArbeidsgiverInfoDeserializer : CustomDeserializer<ArbeidsgiverInfo>() {
    override fun getClass(type: String): KClass<out ArbeidsgiverInfo> {
        return when (ARBEIDSGIVER_TYPE.valueOf(type)) {
            ARBEIDSGIVER_TYPE.EN_ARBEIDSGIVER -> EnArbeidsgiver::class
            ARBEIDSGIVER_TYPE.FLERE_ARBEIDSGIVERE -> FlereArbeidsgivere::class
            ARBEIDSGIVER_TYPE.INGEN_ARBEIDSGIVER -> IngenArbeidsgiver::class
        }
    }
}

class AktivitetDeserializer : CustomDeserializer<Aktivitet>() {
    override fun getClass(type: String): KClass<out Aktivitet> {
        return when (Aktivitetstype.valueOf(type)) {
            Aktivitetstype.AKTIVITET_IKKE_MULIG -> AktivitetIkkeMulig::class
            Aktivitetstype.AVVENTENDE -> Avventende::class
            Aktivitetstype.BEHANDLINGSDAGER -> Behandlingsdager::class
            Aktivitetstype.GRADERT -> Gradert::class
            Aktivitetstype.REISETILSKUDD -> Reisetilskudd::class
        }
    }
}

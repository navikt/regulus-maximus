package no.nav.tsm.mottak.sykmelding.kafka.model.validation

import java.time.OffsetDateTime
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id

data class ValidationResult(
    val status: RuleType,
    val timestamp: OffsetDateTime,
    val rules: List<Rule>
)

enum class RuleType {
    OK, PENDING, INVALID
}

enum class RuleResult {
    OK, INVALID
}

enum class ValidationType {
    AUTOMATIC, MANUAL
}

data class RuleOutcome(
    val outcome: RuleResult,
    val timestamp: OffsetDateTime
)

@JsonSubTypes(
    Type(OKRule::class, name = "OK"),
    Type(InvalidRule::class, name = "INVALID"),
    Type(PendingRule::class, name = "PENDING"),
)
@JsonTypeInfo(use = Id.NAME, include = PROPERTY, property = "type")
sealed interface Rule {
    val type: RuleType
    val name: String
    val description: String
    val timestamp: OffsetDateTime
    val validationType: ValidationType
}

data class InvalidRule(
    override val name: String,
    override val description: String,
    override val timestamp: OffsetDateTime,
    override val validationType: ValidationType = ValidationType.AUTOMATIC
) : Rule {
    override val type = RuleType.INVALID
    val outcome = RuleOutcome(RuleResult.INVALID, timestamp)
}

data class PendingRule(
    override val name: String,
    override val timestamp: OffsetDateTime,
    override val description: String,
    override val validationType: ValidationType = ValidationType.AUTOMATIC
    ) : Rule {
    override val type = RuleType.PENDING
}

data class OKRule(
    override val name: String,
    override val description: String,
    override val timestamp: OffsetDateTime,
    override val validationType: ValidationType = ValidationType.MANUAL
) : Rule {
    override val type = RuleType.OK
    val outcome: RuleOutcome = RuleOutcome(RuleResult.OK, timestamp)
}

package no.nav.tsm.mottak.sykmelding.kafka.model

import java.time.OffsetDateTime


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

sealed interface Rule {
    val type: RuleType
    val name: String
    val description: String
    val validationType: ValidationType
}

data class InvalidRule(
    override val name: String,
    override val description: String,
    val timestamp: OffsetDateTime,
    override val validationType: ValidationType = ValidationType.AUTOMATIC
) : Rule {
    override val type = RuleType.INVALID
    val outcome = RuleOutcome(RuleResult.INVALID, timestamp)
}

data class PendingRule(
    override val name: String,
    val timestamp: OffsetDateTime,
    override val description: String,
    ) : Rule {
    override val validationType = ValidationType.MANUAL
    override val type = RuleType.PENDING
}

data class OKRule(
    override val name: String,
    override val description: String,
    val timestamp: OffsetDateTime,
    override val validationType: ValidationType = ValidationType.MANUAL
) : Rule {
    override val type = RuleType.OK
    val outcome: RuleOutcome = RuleOutcome(RuleResult.OK, timestamp)
}

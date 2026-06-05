package no.nav.tsm.mottak.db

import no.nav.tsm.core.logger
import no.nav.tsm.mottak.sykmelding.exceptions.SykmeldingMergeValidationException
import no.nav.tsm.sykmelding.input.core.model.Rule
import no.nav.tsm.sykmelding.input.core.model.RuleType
import no.nav.tsm.sykmelding.input.core.model.ValidationResult
import no.nav.tsm.sykmelding.input.core.model.ValidationType

private val logger = logger()

fun mergeValidations(old: ValidationResult, new: ValidationResult): ValidationResult {
    if (old == new) {
        return new
    }

    val rule =
        when (old.status) {
            RuleType.PENDING -> {
                if (new.rules.isEmpty()) {
                    return mergePendingWithEmpty(old, new)
                }
                val allRules = old.rules + new.rules

                val latestRule = allRules.maxBy { it.timestamp }

                return ValidationResult(
                    status = latestRule.type,
                    timestamp = latestRule.timestamp,
                    rules = allRules.sortedByDescending { it.timestamp },
                )
            }

            else -> {
                if (
                    new.status != old.status ||
                        new.timestamp != old.timestamp ||
                        !old.rules.containsAll(new.rules)
                ) {
                    if (
                        new.status == RuleType.OK &&
                            old.status == RuleType.OK &&
                            old.rules.containsAll(new.rules)
                    ) {
                        logger.warn(
                            "Ignoring validation result with status OK with different timestamps"
                        )
                    } else {
                        throw SykmeldingMergeValidationException(
                            "Cannot merge from ${old.status} to ${new.status}"
                        )
                    }
                }
                old
            }
        }
    return rule
}

private fun mergePendingWithEmpty(old: ValidationResult, new: ValidationResult): ValidationResult {
    val rule = old.rules.maxBy { it.timestamp }
    val newRule =
        when (new.status) {
            RuleType.OK ->
                Rule.OK(
                    name = rule.name,
                    timestamp = new.timestamp,
                    validationType = ValidationType.MANUAL,
                )

            else ->
                throw SykmeldingMergeValidationException(
                    "Cannot merge from ${old.status} to ${new.status}"
                )
        }
    return ValidationResult(
        status = new.status,
        timestamp = new.timestamp,
        rules = (old.rules + newRule).sortedByDescending { it.timestamp },
    )
}

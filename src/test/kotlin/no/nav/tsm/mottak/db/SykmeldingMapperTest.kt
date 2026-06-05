package no.nav.tsm.mottak.db

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.equals.shouldEqual
import java.time.OffsetDateTime
import no.nav.tsm.mottak.sykmelding.exceptions.SykmeldingMergeValidationException
import no.nav.tsm.sykmelding.input.core.model.Reason
import no.nav.tsm.sykmelding.input.core.model.Rule
import no.nav.tsm.sykmelding.input.core.model.RuleType
import no.nav.tsm.sykmelding.input.core.model.TilbakedatertMerknad
import no.nav.tsm.sykmelding.input.core.model.ValidationResult
import no.nav.tsm.sykmelding.input.core.model.ValidationType
import org.junit.Test

class SykmeldingMapperTest {

    @Test
    fun testOKwithSame() {
        val old =
            ValidationResult(
                status = RuleType.OK,
                timestamp = OffsetDateTime.now().minusDays(1),
                rules = emptyList(),
            )
        val new =
            ValidationResult(status = RuleType.OK, timestamp = old.timestamp, rules = emptyList())

        val merged = mergeValidations(old, new)
        merged shouldEqual new
    }

    @Test
    fun testOKwithDifferentTimestamps() {
        val old =
            ValidationResult(
                status = RuleType.OK,
                timestamp = OffsetDateTime.now().minusDays(1),
                rules = emptyList(),
            )
        val new =
            ValidationResult(
                status = RuleType.OK,
                timestamp = OffsetDateTime.now(),
                rules = emptyList(),
            )

        shouldNotThrowAny { mergeValidations(old, new) }
    }

    @Test
    fun testPendingOKwithSame() {
        val oldTimestamp = OffsetDateTime.now().minusDays(1)
        val old =
            ValidationResult(
                status = RuleType.OK,
                timestamp = oldTimestamp,
                rules = listOf(ok(oldTimestamp), pending(oldTimestamp.minusHours(1))),
            )

        val new =
            ValidationResult(
                status = RuleType.OK,
                timestamp = old.timestamp,
                rules = listOf(ok(oldTimestamp)),
            )

        val merged = mergeValidations(old, new)
        merged shouldEqual old
    }

    @Test
    fun testPendingOKwithDifferentRule() {
        val oldTimestamp = OffsetDateTime.now().minusDays(1)
        val old =
            ValidationResult(
                status = RuleType.OK,
                timestamp = oldTimestamp,
                rules = listOf(ok(oldTimestamp), pending(oldTimestamp.minusHours(1))),
            )
        val new =
            ValidationResult(
                status = RuleType.OK,
                timestamp = old.timestamp,
                rules = listOf(ok(oldTimestamp, name = "rule1")),
            )

        shouldThrow<SykmeldingMergeValidationException> { mergeValidations(old, new) }
    }

    @Test
    fun testInvalidToSame() {
        val oldTimestamp = OffsetDateTime.now().minusDays(1)
        val old =
            ValidationResult(
                status = RuleType.INVALID,
                timestamp = oldTimestamp,
                rules = listOf(invalid(oldTimestamp)),
            )
        val new =
            ValidationResult(
                status = RuleType.INVALID,
                timestamp = old.timestamp,
                rules = listOf(invalid(oldTimestamp)),
            )

        val merged = mergeValidations(old, new)
        merged shouldEqual new
        merged shouldEqual old
    }

    @Test
    fun testInvalidWithDifferentTimestamp() {
        val oldTimestamp = OffsetDateTime.now().minusDays(1)
        val old =
            ValidationResult(
                status = RuleType.INVALID,
                timestamp = oldTimestamp,
                rules = listOf(invalid(oldTimestamp)),
            )
        val new =
            ValidationResult(
                status = RuleType.INVALID,
                timestamp = oldTimestamp.plusHours(1),
                rules = listOf(invalid(oldTimestamp.plusHours(1))),
            )

        shouldThrow<SykmeldingMergeValidationException> { mergeValidations(old, new) }
    }

    @Test
    fun testInvalidToDifferentRule() {
        val oldTimestamp = OffsetDateTime.now()
        val old =
            ValidationResult(
                status = RuleType.INVALID,
                timestamp = oldTimestamp,
                rules = listOf(invalid(oldTimestamp)),
            )
        val new =
            ValidationResult(
                status = RuleType.INVALID,
                timestamp = oldTimestamp,
                rules = listOf(invalid(oldTimestamp, name = "rule1")),
            )

        shouldThrow<SykmeldingMergeValidationException> { mergeValidations(old, new) }
    }

    @Test
    fun testPendingInvalidToSameInvalid() {
        val oldTimestamp = OffsetDateTime.now().minusDays(1)
        val old =
            ValidationResult(
                status = RuleType.INVALID,
                timestamp = oldTimestamp,
                rules = listOf(invalid(oldTimestamp), pending(oldTimestamp.minusHours(1))),
            )
        val new =
            ValidationResult(
                status = RuleType.INVALID,
                timestamp = old.timestamp,
                rules = listOf(invalid(oldTimestamp)),
            )

        val merged = mergeValidations(old, new)
        merged shouldEqual old
    }

    @Test
    fun testPendingInvalidToDifferentInvalid() {
        val oldTimestamp = OffsetDateTime.now().minusDays(1)
        val old =
            ValidationResult(
                status = RuleType.INVALID,
                timestamp = oldTimestamp,
                rules = listOf(invalid(oldTimestamp), pending(oldTimestamp.minusHours(1))),
            )
        val new =
            ValidationResult(
                status = RuleType.INVALID,
                timestamp = old.timestamp,
                rules = listOf(invalid(oldTimestamp, name = "rule1")),
            )

        shouldThrow<SykmeldingMergeValidationException> { mergeValidations(old, new) }
    }

    @Test
    fun fromPendingToEmptyOK() {
        val oldTimestamp = OffsetDateTime.now().minusDays(1)
        val old =
            ValidationResult(
                status = RuleType.PENDING,
                timestamp = oldTimestamp,
                rules =
                    listOf(
                        pending(
                            name = TilbakedatertMerknad.TILBAKEDATERING_UNDER_BEHANDLING.name,
                            timestamp = oldTimestamp,
                            description = "Tilbakedatert sykmelding til manuell behandling",
                            validationType = ValidationType.AUTOMATIC,
                        )
                    ),
            )
        val new =
            ValidationResult(
                status = RuleType.OK,
                timestamp = OffsetDateTime.now(),
                rules = emptyList(),
            )

        val expectedMerge =
            ValidationResult(
                status = RuleType.OK,
                timestamp = new.timestamp,
                rules =
                    listOf(
                            Rule.OK(
                                name = TilbakedatertMerknad.TILBAKEDATERING_UNDER_BEHANDLING.name,
                                timestamp = new.timestamp,
                                validationType = ValidationType.MANUAL,
                            ),
                            pending(oldTimestamp),
                        )
                        .sortedByDescending { it.timestamp },
            )

        val merged = mergeValidations(old, new)
        merged shouldEqual expectedMerge
    }

    @Test
    fun fromPendingToOK() {
        val oldTimestamp = OffsetDateTime.now().minusDays(1)
        val old =
            ValidationResult(
                status = RuleType.PENDING,
                timestamp = oldTimestamp,
                rules = listOf(pending(timestamp = oldTimestamp)),
            )

        val newTimestamp = OffsetDateTime.now()

        val new =
            ValidationResult(
                status = RuleType.OK,
                timestamp = newTimestamp,
                rules = listOf(ok(newTimestamp)),
            )

        val expectedMerge =
            ValidationResult(
                status = RuleType.OK,
                timestamp = new.timestamp,
                rules =
                    listOf(ok(newTimestamp), pending(oldTimestamp)).sortedByDescending {
                        it.timestamp
                    },
            )

        val merged = mergeValidations(old, new)
        merged shouldEqual expectedMerge
    }

    @Test
    fun fromPendingToPending() {
        val oldTimestamp = OffsetDateTime.now().minusDays(1)
        val old =
            ValidationResult(
                status = RuleType.PENDING,
                timestamp = oldTimestamp,
                rules = listOf(pending(timestamp = oldTimestamp)),
            )

        val newTimestamp = OffsetDateTime.now()

        val new =
            ValidationResult(
                status = RuleType.PENDING,
                timestamp = newTimestamp,
                rules = listOf(pending(timestamp = newTimestamp)),
            )

        val expectedMerge =
            ValidationResult(
                status = RuleType.PENDING,
                timestamp = new.timestamp,
                rules =
                    listOf(pending(newTimestamp), pending(oldTimestamp)).sortedByDescending {
                        it.timestamp
                    },
            )

        val merged = mergeValidations(old, new)
        merged shouldEqual expectedMerge
    }

    @Test
    fun testPendingToPendingToEmptyOk() {
        val oldTimestamp = OffsetDateTime.now().minusDays(1)
        val old =
            ValidationResult(
                status = RuleType.PENDING,
                timestamp = oldTimestamp,
                rules = listOf(pending(timestamp = oldTimestamp)),
            )

        val newTimestamp = OffsetDateTime.now()

        val new =
            ValidationResult(
                status = RuleType.PENDING,
                timestamp = newTimestamp,
                rules = listOf(pending(timestamp = newTimestamp)),
            )

        val emptyOk =
            ValidationResult(
                status = RuleType.OK,
                timestamp = OffsetDateTime.now(),
                rules = emptyList(),
            )

        val expectedMerge =
            ValidationResult(
                status = RuleType.OK,
                timestamp = emptyOk.timestamp,
                rules =
                    listOf(
                            ok(emptyOk.timestamp, ValidationType.MANUAL),
                            pending(newTimestamp),
                            pending(oldTimestamp),
                        )
                        .sortedByDescending { it.timestamp },
            )

        val merged = mergeValidations(mergeValidations(old, new), emptyOk)
        merged shouldEqual expectedMerge
    }

    @Test
    fun testPendingToPendingToInvalid() {
        val oldTimestamp = OffsetDateTime.now().minusDays(1)
        val first =
            ValidationResult(
                status = RuleType.PENDING,
                timestamp = oldTimestamp,
                rules = listOf(pending(timestamp = oldTimestamp)),
            )

        val newTimestamp = OffsetDateTime.now().minusHours(1)

        val second =
            ValidationResult(
                status = RuleType.PENDING,
                timestamp = newTimestamp,
                rules = listOf(pending(timestamp = newTimestamp)),
            )

        val invalidTimestamp = OffsetDateTime.now()
        val invalid =
            ValidationResult(
                status = RuleType.INVALID,
                timestamp = invalidTimestamp,
                rules = listOf(invalid(invalidTimestamp, validationType = ValidationType.MANUAL)),
            )

        val expectedMerge =
            ValidationResult(
                status = RuleType.INVALID,
                timestamp = invalid.timestamp,
                rules =
                    listOf(
                            invalid(invalid.timestamp, ValidationType.MANUAL),
                            pending(newTimestamp),
                            pending(oldTimestamp),
                        )
                        .sortedByDescending { it.timestamp },
            )

        val merged = mergeValidations(mergeValidations(first, second), invalid)
        merged shouldEqual expectedMerge
    }
}

fun pending(
    timestamp: OffsetDateTime = OffsetDateTime.now(),
    validationType: ValidationType = ValidationType.AUTOMATIC,
    name: String = TilbakedatertMerknad.TILBAKEDATERING_UNDER_BEHANDLING.name,
    description: String = "Tilbakedatert sykmelding til manuell behandling",
) =
    Rule.Pending(
        name = name,
        timestamp = timestamp,
        reason = Reason(description, description),
        validationType = validationType,
    )

fun ok(
    timestamp: OffsetDateTime = OffsetDateTime.now(),
    validationType: ValidationType = ValidationType.AUTOMATIC,
    name: String = TilbakedatertMerknad.TILBAKEDATERING_UNDER_BEHANDLING.name,
) = Rule.OK(name = name, timestamp = timestamp, validationType = validationType)

fun invalid(
    timestamp: OffsetDateTime = OffsetDateTime.now(),
    validationType: ValidationType = ValidationType.AUTOMATIC,
    name: String = TilbakedatertMerknad.TILBAKEDATERING_UNDER_BEHANDLING.name,
    description: String = "Tilbakedatert sykmelding til manuell behandling",
) =
    Rule.Invalid(
        name = name,
        timestamp = timestamp,
        reason = Reason(description, description),
        validationType = validationType,
    )

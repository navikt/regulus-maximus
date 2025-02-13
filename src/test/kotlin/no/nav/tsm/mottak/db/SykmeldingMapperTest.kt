package no.nav.tsm.mottak.db

import no.nav.tsm.mottak.sykmelding.exceptions.SykmeldingMergeValidationException
import no.nav.tsm.mottak.sykmelding.model.OKRule
import no.nav.tsm.mottak.sykmelding.model.PendingRule
import no.nav.tsm.mottak.sykmelding.model.InvalidRule
import no.nav.tsm.mottak.sykmelding.model.RuleType
import no.nav.tsm.mottak.sykmelding.model.TilbakedatertMerknad
import no.nav.tsm.mottak.sykmelding.model.ValidationResult
import no.nav.tsm.mottak.sykmelding.model.ValidationType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.nio.file.Files.lines
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SykmeldingMapperTest {

    @Test
    fun testOKwithSame() {
        val old = ValidationResult(
            status = RuleType.OK,
            timestamp = OffsetDateTime.now().minusDays(1),
            rules = emptyList()
        )
        val new = ValidationResult(
            status = RuleType.OK,
            timestamp = old.timestamp,
            rules = emptyList()
        )

        val merged = SykmeldingMapper.mergeValidations(old, new)
        assertEquals(new, merged)
    }

    @Test
    fun testOKwithDifferentTimestamps() {
        val old = ValidationResult(
            status = RuleType.OK,
            timestamp = OffsetDateTime.now().minusDays(1),
            rules = emptyList()
        )
        val new = ValidationResult(
            status = RuleType.OK,
            timestamp = OffsetDateTime.now(),
            rules = emptyList()
        )

        assertThrows<SykmeldingMergeValidationException> { SykmeldingMapper.mergeValidations(old, new) }
    }

    @Test
    fun testPendingOKwithSame() {
        val oldTimestamp = OffsetDateTime.now().minusDays(1)
        val old = ValidationResult(
            status = RuleType.OK,
            timestamp = oldTimestamp,
            rules = listOf(
                ok(oldTimestamp),
                pending(oldTimestamp.minusHours(1))))

        val new = ValidationResult(
            status = RuleType.OK,
            timestamp = old.timestamp,
            rules = listOf(ok(oldTimestamp))
        )

        val merged = SykmeldingMapper.mergeValidations(old, new)
        assertEquals(old, merged)
    }

    @Test
    fun testPendingOKwithDifferentRule() {
        val oldTimestamp = OffsetDateTime.now().minusDays(1)
        val old = ValidationResult(
            status = RuleType.OK,
            timestamp = oldTimestamp,
            rules = listOf(ok(oldTimestamp),
                pending(oldTimestamp.minusHours(1)))
        )
        val new = ValidationResult(
            status = RuleType.OK,
            timestamp = old.timestamp,
            rules = listOf(ok(oldTimestamp, name = "rule1"))
        )

        assertThrows<SykmeldingMergeValidationException> { SykmeldingMapper.mergeValidations(old, new) }

    }

    @Test
    fun testInvalidToSame() {
        val oldTimestamp = OffsetDateTime.now().minusDays(1)
        val old = ValidationResult(
            status = RuleType.INVALID,
            timestamp = oldTimestamp,
            rules = listOf(invalid(oldTimestamp))
        )
        val new = ValidationResult(
            status = RuleType.INVALID,
            timestamp = old.timestamp,
            rules = listOf(invalid(oldTimestamp))
        )

        val merged =  SykmeldingMapper.mergeValidations(old, new)
        assertEquals(new, merged)
        assertEquals(old, merged)
    }

    @Test
    fun testInvalidWithDifferentTimestamp() {
        val oldTimestamp = OffsetDateTime.now().minusDays(1)
        val old = ValidationResult(
            status = RuleType.INVALID,
            timestamp = oldTimestamp,
            rules = listOf(invalid(oldTimestamp))
        )
        val new = ValidationResult(
            status = RuleType.INVALID,
            timestamp = oldTimestamp.plusHours(1),
            rules = listOf(invalid(oldTimestamp.plusHours(1)))
        )

        assertThrows<SykmeldingMergeValidationException> { SykmeldingMapper.mergeValidations(old, new) }

    }

    @Test
    fun testInvalidToDifferentRule() {
        val oldTimestamp = OffsetDateTime.now()
        val old = ValidationResult(
            status = RuleType.INVALID,
            timestamp = oldTimestamp,
            rules = listOf(invalid(oldTimestamp))
        )
        val new = ValidationResult(
            status = RuleType.INVALID,
            timestamp = oldTimestamp,
            rules = listOf(invalid(oldTimestamp, name = "rule1"))
        )

        assertThrows<SykmeldingMergeValidationException> { SykmeldingMapper.mergeValidations(old, new) }
    }

    @Test
    fun testPendingInvalidToSameInvalid() {
        val oldTimestamp = OffsetDateTime.now().minusDays(1)
        val old = ValidationResult(
            status = RuleType.INVALID,
            timestamp = oldTimestamp,
            rules = listOf(invalid(oldTimestamp),
                pending(oldTimestamp.minusHours(1)))
        )
        val new = ValidationResult(
            status = RuleType.INVALID,
            timestamp = old.timestamp,
            rules = listOf(invalid(oldTimestamp))
        )

        val merged = SykmeldingMapper.mergeValidations(old, new)
        assertEquals(old, merged)
    }

    @Test
    fun testPendingInvalidToDifferentInvalid() {
        val oldTimestamp = OffsetDateTime.now().minusDays(1)
        val old = ValidationResult(
            status = RuleType.INVALID,
            timestamp = oldTimestamp,
            rules = listOf(invalid(oldTimestamp),
                pending(oldTimestamp.minusHours(1)))
        )
        val new = ValidationResult(
            status = RuleType.INVALID,
            timestamp = old.timestamp,
            rules = listOf(invalid(oldTimestamp, name = "rule1"))
        )

        assertThrows<SykmeldingMergeValidationException> { SykmeldingMapper.mergeValidations(old, new) }
    }


    @Test
    fun fromPendingToEmptyOK() {
        val oldTimestamp = OffsetDateTime.now().minusDays(1)
        val old = ValidationResult(
            status = RuleType.PENDING,
            timestamp = oldTimestamp,
            rules = listOf(
                pending(
                    name = TilbakedatertMerknad.TILBAKEDATERING_UNDER_BEHANDLING.name,
                    timestamp = oldTimestamp,
                    description = "Tilbakedatert sykmelding til manuell behandling",
                    validationType = ValidationType.AUTOMATIC
                )
            )
        )
        val new = ValidationResult(
            status = RuleType.OK,
            timestamp = OffsetDateTime.now(),
            rules = emptyList()
        )

        val expectedMerge = ValidationResult(
            status = RuleType.OK,
            timestamp = new.timestamp,
            rules = listOf(
                OKRule(
                    name = TilbakedatertMerknad.TILBAKEDATERING_UNDER_BEHANDLING.name,
                    description = "Tilbakedatert sykmelding til manuell behandling",
                    timestamp = new.timestamp,
                    validationType = ValidationType.MANUAL,
                ),
                pending(oldTimestamp)
            ).sortedByDescending { it.timestamp }
        )

        val merged = SykmeldingMapper.mergeValidations(old, new)
        assertEquals(expectedMerge, merged)
    }

    @Test
    fun fromPendingToOK() {
        val oldTimestamp = OffsetDateTime.now().minusDays(1)
        val old = ValidationResult(
            status = RuleType.PENDING,
            timestamp = oldTimestamp,
            rules = listOf(pending(timestamp = oldTimestamp))
        )

        val newTimestamp = OffsetDateTime.now()

        val new = ValidationResult(
            status = RuleType.OK,
            timestamp = newTimestamp,
            rules = listOf(ok(newTimestamp))
        )

        val expectedMerge = ValidationResult(
            status = RuleType.OK,
            timestamp = new.timestamp,
            rules = listOf(
                ok(newTimestamp),
                pending(oldTimestamp)
            ).sortedByDescending { it.timestamp }
        )

        val merged = SykmeldingMapper.mergeValidations(old, new)
        assertEquals(expectedMerge, merged)
    }

    @Test
    fun fromPendingToPending() {
        val oldTimestamp = OffsetDateTime.now().minusDays(1)
        val old = ValidationResult(
            status = RuleType.PENDING,
            timestamp = oldTimestamp,
            rules = listOf(pending(timestamp = oldTimestamp))
        )

        val newTimestamp = OffsetDateTime.now()

        val new = ValidationResult(
            status = RuleType.PENDING,
            timestamp = newTimestamp,
            rules = listOf(pending(timestamp = newTimestamp))
        )

        val expectedMerge = ValidationResult(
            status = RuleType.PENDING,
            timestamp = new.timestamp,
            rules = listOf(
                pending(newTimestamp),
                pending(oldTimestamp)
            ).sortedByDescending { it.timestamp }
        )

        val merged = SykmeldingMapper.mergeValidations(old, new)
        assertEquals(expectedMerge, merged)
    }

    @Test
    fun testPendingToPendingToEmptyOk() {
        val oldTimestamp = OffsetDateTime.now().minusDays(1)
        val old = ValidationResult(
            status = RuleType.PENDING,
            timestamp = oldTimestamp,
            rules = listOf(pending(timestamp = oldTimestamp))
        )

        val newTimestamp = OffsetDateTime.now()

        val new = ValidationResult(
            status = RuleType.PENDING,
            timestamp = newTimestamp,
            rules = listOf(pending(timestamp = newTimestamp))
        )

        val emptyOk = ValidationResult(
            status = RuleType.OK,
            timestamp = OffsetDateTime.now(),
            rules = emptyList()
        )

        val expectedMerge = ValidationResult(
            status = RuleType.OK,
            timestamp = emptyOk.timestamp,
            rules = listOf(
                ok(emptyOk.timestamp, ValidationType.MANUAL),
                pending(newTimestamp),
                pending(oldTimestamp)
            ).sortedByDescending { it.timestamp }
        )

        val merged = SykmeldingMapper.mergeValidations(SykmeldingMapper.mergeValidations(old, new), emptyOk)
        assertEquals(expectedMerge, merged)
    }

    @Test
    fun testPendingToPendingToInvalid() {
        val oldTimestamp = OffsetDateTime.now().minusDays(1)
        val first = ValidationResult(
            status = RuleType.PENDING,
            timestamp = oldTimestamp,
            rules = listOf(pending(timestamp = oldTimestamp))
        )

        val newTimestamp = OffsetDateTime.now().minusHours(1)

        val second = ValidationResult(
            status = RuleType.PENDING,
            timestamp = newTimestamp,
            rules = listOf(pending(timestamp = newTimestamp))
        )

        val invalidTimestamp = OffsetDateTime.now()
        val invalid = ValidationResult(
            status = RuleType.INVALID,
            timestamp = invalidTimestamp,
            rules = listOf(invalid(invalidTimestamp, validationType = ValidationType.MANUAL)),
        )

        val expectedMerge = ValidationResult(
            status = RuleType.INVALID,
            timestamp = invalid.timestamp,
            rules = listOf(
                invalid(invalid.timestamp, ValidationType.MANUAL),
                pending(newTimestamp),
                pending(oldTimestamp)
            ).sortedByDescending { it.timestamp }
        )

        val merged = SykmeldingMapper.mergeValidations(SykmeldingMapper.mergeValidations(first, second), invalid)
        assertEquals(expectedMerge, merged)
    }
}

fun pending(
    timestamp: OffsetDateTime = OffsetDateTime.now(),
    validationType: ValidationType = ValidationType.AUTOMATIC,
    name: String = TilbakedatertMerknad.TILBAKEDATERING_UNDER_BEHANDLING.name,
    description: String = "Tilbakedatert sykmelding til manuell behandling",
) = PendingRule(
    name = name,
    timestamp = timestamp,
    description = description,
    validationType = validationType,
)

fun ok(
    timestamp: OffsetDateTime = OffsetDateTime.now(),
    validationType: ValidationType = ValidationType.AUTOMATIC,
    name: String = TilbakedatertMerknad.TILBAKEDATERING_UNDER_BEHANDLING.name,
    description: String = "Tilbakedatert sykmelding til manuell behandling",
) = OKRule(
    name = name,
    timestamp = timestamp,
    description = description,
    validationType = validationType,
)

fun invalid(
    timestamp: OffsetDateTime = OffsetDateTime.now(),
    validationType: ValidationType = ValidationType.AUTOMATIC,
    name: String = TilbakedatertMerknad.TILBAKEDATERING_UNDER_BEHANDLING.name,
    description: String = "Tilbakedatert sykmelding til manuell behandling",
) = InvalidRule(
    name = name,
    timestamp = timestamp,
    description = description,
    validationType = validationType,
)

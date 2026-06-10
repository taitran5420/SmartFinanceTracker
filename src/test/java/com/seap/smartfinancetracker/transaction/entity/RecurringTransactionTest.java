package com.seap.smartfinancetracker.transaction.entity;

import org.instancio.Instancio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Unit tests for {@link RecurringTransaction} domain behavior — notably the deterministic
 * per-occurrence idempotency key that protects against double-charging when a due occurrence
 * is processed more than once.
 */
class RecurringTransactionTest {

    @Test
    @DisplayName("Should produce the SAME key for the same occurrence (id + nextOccurrenceDate)")
    void currentOccurrenceIdempotencyKey_ShouldBeDeterministicForSameOccurrence() {
        // A re-fire of an occurrence whose date was not yet advanced must reuse the same key
        RecurringTransaction recurring = Instancio.of(RecurringTransaction.class)
                .set(field(RecurringTransaction::getId), UUID.randomUUID())
                .set(field(RecurringTransaction::getNextOccurrenceDate), LocalDate.of(2026, 6, 15))
                .create();

        assertEquals(recurring.currentOccurrenceIdempotencyKey(), recurring.currentOccurrenceIdempotencyKey(),
                "Re-processing the same occurrence must yield an identical key so the duplicate is rejected");
    }

    @Test
    @DisplayName("Should produce a DIFFERENT key when the occurrence date advances")
    void currentOccurrenceIdempotencyKey_ShouldDifferForNextOccurrence() {
        // The next occurrence (a later date) is a legitimate new charge and must not be blocked
        RecurringTransaction current = Instancio.of(RecurringTransaction.class)
                .set(field(RecurringTransaction::getId), UUID.randomUUID())
                .set(field(RecurringTransaction::getNextOccurrenceDate), LocalDate.of(2026, 6, 15))
                .create();
        RecurringTransaction next = current.toBuilder()
                .nextOccurrenceDate(LocalDate.of(2026, 7, 15))
                .build();

        assertNotEquals(current.currentOccurrenceIdempotencyKey(), next.currentOccurrenceIdempotencyKey(),
                "A later occurrence must yield a new key so legitimate recurring charges are not blocked");
    }

    @Test
    @DisplayName("Should produce a DIFFERENT key for different schedules due on the same date")
    void currentOccurrenceIdempotencyKey_ShouldDifferAcrossSchedules() {
        // Distinct schedules due on the same date must not collide on the transaction's unique key
        LocalDate occurrence = LocalDate.of(2026, 6, 15);
        RecurringTransaction first = Instancio.of(RecurringTransaction.class)
                .set(field(RecurringTransaction::getId), UUID.randomUUID())
                .set(field(RecurringTransaction::getNextOccurrenceDate), occurrence)
                .create();
        RecurringTransaction second = Instancio.of(RecurringTransaction.class)
                .set(field(RecurringTransaction::getId), UUID.randomUUID())
                .set(field(RecurringTransaction::getNextOccurrenceDate), occurrence)
                .create();

        assertNotEquals(first.currentOccurrenceIdempotencyKey(), second.currentOccurrenceIdempotencyKey(),
                "Distinct schedules must produce distinct keys even on the same date");
    }
}

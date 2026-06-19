package com.seap.smartfinancetracker.analytics.dto;

import java.time.Instant;

/**
 * Data Transfer Object representing the optional date-range filter shared by the
 * analytics endpoints.
 * <p>
 * Both bounds are optional and bound from HTTP query parameters. When a bound is omitted,
 * that side of the range is treated as unbounded; omitting both aggregates over the user's
 * entire history. When both are supplied, {@code startDate} must not be after {@code endDate}.
 * </p>
 *
 * @param startDate the inclusive start timestamp of the analytics window, or {@code null} for no lower bound
 * @param endDate   the inclusive end timestamp of the analytics window, or {@code null} for no upper bound
 */
public record AnalyticsPeriodRequest(
        Instant startDate,
        Instant endDate
) {
}

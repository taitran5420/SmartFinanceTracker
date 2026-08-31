package com.seap.smartfinancetracker.analytics.graphql;

import com.seap.smartfinancetracker.analytics.dto.AnalyticsPeriodRequest;
import com.seap.smartfinancetracker.analytics.dto.MonthlyTrendPointResponse;
import com.seap.smartfinancetracker.analytics.dto.PeriodSummaryResponse;
import com.seap.smartfinancetracker.analytics.dto.SpendingByCategoryResponse;
import com.seap.smartfinancetracker.analytics.service.AnalyticsService;
import com.seap.smartfinancetracker.security.annotation.CurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * GraphQL entry point for the analytics domain, exposed alongside the REST
 * {@link com.seap.smartfinancetracker.analytics.controller.AnalyticsController}.
 * <p>
 * The top-level {@code analytics} query returns a lightweight {@link AnalyticsRoot} holder; the
 * three {@code @SchemaMapping} field resolvers each delegate to the matching
 * {@link AnalyticsService} method, and only run when their sub-field is selected. All caching,
 * date-range validation and {@code userId} scoping therefore live in the shared service, not here.
 * </p>
 */
@Controller
@RequiredArgsConstructor
public class AnalyticsGraphqlController {

    private final AnalyticsService analyticsService;

    /**
     * Resolves the {@code analytics} query into a holder carrying the caller and the requested
     * window. The heavy aggregation is deferred to the field resolvers below.
     *
     * @param startDate optional inclusive start of the window; {@code null} widens the lower bound
     * @param endDate   optional inclusive end of the window; {@code null} widens the upper bound
     * @param userId    the authenticated user's id, injected from the security context
     * @return a source object for the {@code Analytics} field resolvers
     */
    @QueryMapping
    public AnalyticsRoot analytics(@Argument OffsetDateTime startDate,
                                   @Argument OffsetDateTime endDate,
                                   @CurrentUserId UUID userId) {
        AnalyticsPeriodRequest period = new AnalyticsPeriodRequest(toInstant(startDate), toInstant(endDate));
        return new AnalyticsRoot(userId, period);
    }

    /**
     * Resolves {@code Analytics.summary} only when the field is selected.
     */
    @SchemaMapping(typeName = "Analytics")
    public PeriodSummaryResponse summary(AnalyticsRoot root) {
        return analyticsService.getPeriodSummary(root.userId(), root.period());
    }

    /**
     * Resolves {@code Analytics.spendingByCategory} only when the field is selected.
     */
    @SchemaMapping(typeName = "Analytics")
    public SpendingByCategoryResponse spendingByCategory(AnalyticsRoot root) {
        return analyticsService.getSpendingByCategory(root.userId(), root.period());
    }

    /**
     * Resolves {@code Analytics.incomeExpenseTrend} only when the field is selected.
     */
    @SchemaMapping(typeName = "Analytics")
    public List<MonthlyTrendPointResponse> incomeExpenseTrend(AnalyticsRoot root) {
        return analyticsService.getIncomeExpenseTrend(root.userId(), root.period());
    }

    /**
     * Converts the {@code DateTime} scalar (bound as {@link OffsetDateTime}) into the
     * {@link Instant} the service layer expects, preserving {@code null} for an unbounded edge.
     *
     * @param value the bound argument, or {@code null} when the bound was omitted
     * @return the equivalent {@link Instant}, or {@code null}
     */
    private Instant toInstant(OffsetDateTime value) {
        return value == null ? null : value.toInstant();
    }
}

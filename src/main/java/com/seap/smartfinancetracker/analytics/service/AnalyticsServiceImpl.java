package com.seap.smartfinancetracker.analytics.service;

import com.seap.smartfinancetracker.analytics.constant.AnalyticsCacheConstant;
import com.seap.smartfinancetracker.analytics.dto.AnalyticsPeriodRequest;
import com.seap.smartfinancetracker.analytics.dto.CategorySpendingResponse;
import com.seap.smartfinancetracker.analytics.dto.MonthlyTrendPointResponse;
import com.seap.smartfinancetracker.analytics.dto.PeriodSummaryResponse;
import com.seap.smartfinancetracker.analytics.dto.SpendingByCategoryResponse;
import com.seap.smartfinancetracker.analytics.exception.AnalyticsErrorCode;
import com.seap.smartfinancetracker.common.exception.BusinessException;
import com.seap.smartfinancetracker.transaction.enums.TransactionType;
import com.seap.smartfinancetracker.transaction.repository.CategorySpendingProjection;
import com.seap.smartfinancetracker.transaction.repository.MonthlyTrendProjection;
import com.seap.smartfinancetracker.transaction.repository.PeriodTotalsProjection;
import com.seap.smartfinancetracker.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Concrete implementation of {@link AnalyticsService}.
 * <p>
 * All aggregation is delegated to the database via {@link TransactionRepository}; this
 * class is responsible only for validating the requested window and shaping the projection
 * rows into client-facing DTOs. Every operation is read-only and strictly scoped to the
 * supplied {@code userId}.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final TransactionRepository transactionRepository;

    /**
     * Scale and rounding used when expressing a category's spend as a percentage share.
     */
    private static final int PERCENTAGE_SCALE = 2;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = AnalyticsCacheConstant.SPENDING_BY_CATEGORY_CACHE,
            key = "@analyticsCacheService.buildKey(#userId, #request)")
    public SpendingByCategoryResponse getSpendingByCategory(UUID userId, AnalyticsPeriodRequest request) {
        Instant startDate = resolveAndValidate(request).startDate();
        Instant endDate = request.endDate();

        // A single grouped query returns each category's id, name, and total in one round-trip.
        List<CategorySpendingProjection> rows = transactionRepository
                .findCategorySpending(userId, startDate, endDate, Pageable.unpaged());

        BigDecimal totalExpense = rows.stream()
                .map(CategorySpendingProjection::getTotalSpent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CategorySpendingResponse> categories = rows.stream()
                .map(row -> CategorySpendingResponse.builder()
                        .categoryId(row.getCategoryId())
                        .categoryName(row.getCategoryName())
                        .totalSpent(row.getTotalSpent())
                        .percentage(toPercentage(row.getTotalSpent(), totalExpense))
                        .build())
                .toList();

        return SpendingByCategoryResponse.builder()
                .totalExpense(totalExpense)
                .categories(categories)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = AnalyticsCacheConstant.SUMMARY_CACHE,
            key = "@analyticsCacheService.buildKey(#userId, #request)")
    public PeriodSummaryResponse getPeriodSummary(UUID userId, AnalyticsPeriodRequest request) {
        Instant startDate = resolveAndValidate(request).startDate();
        Instant endDate = request.endDate();

        // One query yields income total, expense total, and transaction count together.
        PeriodTotalsProjection totals = transactionRepository.calculatePeriodTotals(userId, startDate, endDate);
        BigDecimal totalIncome = totals.getTotalIncome();
        BigDecimal totalExpense = totals.getTotalExpense();

        PeriodSummaryResponse.PeriodSummaryResponseBuilder builder = PeriodSummaryResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .net(totalIncome.subtract(totalExpense))
                .transactionCount(totals.getTransactionCount())
                .topCategoryAmount(BigDecimal.ZERO);

        // A second query fetches only the single highest-spending category, name included.
        List<CategorySpendingProjection> topCategories = transactionRepository
                .findCategorySpending(userId, startDate, endDate, PageRequest.of(0, 1));
        if (!topCategories.isEmpty()) {
            CategorySpendingProjection top = topCategories.getFirst();
            builder.topCategoryId(top.getCategoryId())
                    .topCategoryName(top.getCategoryName())
                    .topCategoryAmount(top.getTotalSpent());
        }

        return builder.build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = AnalyticsCacheConstant.INCOME_EXPENSE_TREND_CACHE,
            key = "@analyticsCacheService.buildKey(#userId, #request)")
    public List<MonthlyTrendPointResponse> getIncomeExpenseTrend(UUID userId, AnalyticsPeriodRequest request) {
        Instant startDate = resolveAndValidate(request).startDate();
        Instant endDate = request.endDate();

        List<MonthlyTrendProjection> rows = transactionRepository.findMonthlyTrend(userId, startDate, endDate);

        // The query orders rows chronologically; a LinkedHashMap preserves that order while
        // merging the separate INCOME and EXPENSE rows of the same month into one point.
        Map<Integer, MonthlyAccumulator> byMonth = new LinkedHashMap<>();
        for (MonthlyTrendProjection row : rows) {
            int key = row.getYear() * 100 + row.getMonth();
            MonthlyAccumulator accumulator = byMonth.computeIfAbsent(key,
                    ignored -> new MonthlyAccumulator(row.getYear(), row.getMonth()));
            accumulator.add(row.getTransactionType(), row.getTotalAmount());
        }

        List<MonthlyTrendPointResponse> points = new ArrayList<>(byMonth.size());
        for (MonthlyAccumulator accumulator : byMonth.values()) {
            points.add(MonthlyTrendPointResponse.builder()
                    .year(accumulator.year)
                    .month(accumulator.month)
                    .totalIncome(accumulator.income)
                    .totalExpense(accumulator.expense)
                    .net(accumulator.income.subtract(accumulator.expense))
                    .build());
        }

        return points;
    }

    /**
     * Validates the requested window, rejecting it when {@code startDate} is after
     * {@code endDate}.
     *
     * @param request the requested window (never {@code null}; bounds may be {@code null})
     * @return the same request, for fluent use by callers
     * @throws BusinessException with {@link AnalyticsErrorCode#INVALID_DATE_RANGE} if the range is inverted
     */
    private AnalyticsPeriodRequest resolveAndValidate(AnalyticsPeriodRequest request) {
        if (request.startDate() != null && request.endDate() != null
                && request.startDate().isAfter(request.endDate())) {
            log.warn("Rejected analytics request with inverted range: start={}, end={}",
                    request.startDate(), request.endDate());
            throw new BusinessException(AnalyticsErrorCode.INVALID_DATE_RANGE);
        }
        return request;
    }

    /**
     * Expresses {@code part} as a percentage of {@code total}, returning {@code 0} when the
     * total is zero to avoid division by zero.
     *
     * @param part  the numerator amount
     * @param total the denominator amount
     * @return the percentage share, rounded to {@link #PERCENTAGE_SCALE} decimal places
     */
    private double toPercentage(BigDecimal part, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return 0d;
        }
        return part.divide(total, PERCENTAGE_SCALE + 2, RoundingMode.HALF_UP)
                .multiply(HUNDRED)
                .setScale(PERCENTAGE_SCALE, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * Mutable per-month tally used while merging the income and expense rows of a single
     * calendar month into one trend point.
     */
    private static final class MonthlyAccumulator {
        private final int year;
        private final int month;
        private BigDecimal income = BigDecimal.ZERO;
        private BigDecimal expense = BigDecimal.ZERO;

        private MonthlyAccumulator(int year, int month) {
            this.year = year;
            this.month = month;
        }

        private void add(TransactionType type, BigDecimal amount) {
            if (type == TransactionType.INCOME) {
                income = income.add(amount);
            } else {
                expense = expense.add(amount);
            }
        }
    }
}

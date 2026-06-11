package com.seap.smartfinancetracker.analytics.service;

import com.seap.smartfinancetracker.analytics.dto.AnalyticsPeriodRequest;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AnalyticsServiceImpl}.
 * <p>
 * Database aggregation is mocked via {@link TransactionRepository}; these tests focus on the
 * service's own responsibilities: validating the requested window, computing percentage
 * shares, selecting the top category, and merging per-type rows into monthly trend points.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AnalyticsServiceImplTest {

    //<editor-fold desc="Setup & Configurations">
    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AnalyticsServiceImpl analyticsService;

    private final UUID userId = UUID.randomUUID();

    /**
     * Builds a Mockito-backed {@link CategorySpendingProjection} returning the supplied values.
     */
    private CategorySpendingProjection categorySpending(UUID id, String name, BigDecimal total) {
        CategorySpendingProjection projection = org.mockito.Mockito.mock(CategorySpendingProjection.class);
        when(projection.getCategoryId()).thenReturn(id);
        when(projection.getCategoryName()).thenReturn(name);
        when(projection.getTotalSpent()).thenReturn(total);
        return projection;
    }

    /**
     * Builds a Mockito-backed {@link MonthlyTrendProjection} for one year/month/type bucket.
     */
    private MonthlyTrendProjection trendRow(int year, int month, TransactionType type, BigDecimal amount) {
        MonthlyTrendProjection projection = org.mockito.Mockito.mock(MonthlyTrendProjection.class);
        when(projection.getYear()).thenReturn(year);
        when(projection.getMonth()).thenReturn(month);
        when(projection.getTransactionType()).thenReturn(type);
        when(projection.getTotalAmount()).thenReturn(amount);
        return projection;
    }
    //</editor-fold>

    //<editor-fold desc="Test getSpendingByCategory">
    @Test
    @DisplayName("Should aggregate per-category totals and compute each category's percentage share")
    void getSpendingByCategory_ShouldComputeTotalsAndPercentages() {
        // Arrange: two categories, 75 and 25, totalling 100
        UUID catA = UUID.randomUUID();
        UUID catB = UUID.randomUUID();
        List<CategorySpendingProjection> rows = List.of(
                categorySpending(catA, "Rent", new BigDecimal("75.0000")),
                categorySpending(catB, "Food", new BigDecimal("25.0000"))
        );
        when(transactionRepository.findCategorySpending(eq(userId), any(), any(), any(Pageable.class)))
                .thenReturn(rows);

        // Act
        SpendingByCategoryResponse response =
                analyticsService.getSpendingByCategory(userId, new AnalyticsPeriodRequest(null, null));

        // Assert
        assertEquals(0, new BigDecimal("100.0000").compareTo(response.totalExpense()),
                "Total expense must be the sum of all category totals");
        assertEquals(2, response.categories().size());
        assertEquals(75.0, response.categories().get(0).percentage(), 0.001,
                "75 of 100 must be 75%");
        assertEquals(25.0, response.categories().get(1).percentage(), 0.001,
                "25 of 100 must be 25%");
        assertEquals("Rent", response.categories().get(0).categoryName());
    }

    @Test
    @DisplayName("Should return zero total and empty list when the user has no expenses in the window")
    void getSpendingByCategory_ShouldReturnEmpty_WhenNoExpenses() {
        // Arrange
        when(transactionRepository.findCategorySpending(eq(userId), any(), any(), any(Pageable.class)))
                .thenReturn(List.of());

        // Act
        SpendingByCategoryResponse response =
                analyticsService.getSpendingByCategory(userId, new AnalyticsPeriodRequest(null, null));

        // Assert
        assertEquals(0, BigDecimal.ZERO.compareTo(response.totalExpense()));
        assertTrue(response.categories().isEmpty());
    }

    @Test
    @DisplayName("Should report 0% (no divide-by-zero) when category totals sum to zero")
    void getSpendingByCategory_ShouldReportZeroPercent_WhenTotalIsZero() {
        // Arrange: a single zero-amount row -> total is zero
        List<CategorySpendingProjection> rows =
                List.of(categorySpending(UUID.randomUUID(), "Misc", BigDecimal.ZERO));
        when(transactionRepository.findCategorySpending(eq(userId), any(), any(), any(Pageable.class)))
                .thenReturn(rows);

        // Act
        SpendingByCategoryResponse response =
                analyticsService.getSpendingByCategory(userId, new AnalyticsPeriodRequest(null, null));

        // Assert
        assertEquals(0.0, response.categories().getFirst().percentage(), 0.001,
                "Dividing by a zero total must yield 0%, not an arithmetic error");
    }
    //</editor-fold>

    //<editor-fold desc="Test getPeriodSummary">
    @Test
    @DisplayName("Should compute net, count and top category for the period summary")
    void getPeriodSummary_ShouldComputeNetAndTopCategory() {
        // Arrange
        UUID topCat = UUID.randomUUID();
        PeriodTotalsProjection totals = org.mockito.Mockito.mock(PeriodTotalsProjection.class);
        when(totals.getTotalIncome()).thenReturn(new BigDecimal("200.0000"));
        when(totals.getTotalExpense()).thenReturn(new BigDecimal("50.0000"));
        when(totals.getTransactionCount()).thenReturn(3L);

        List<CategorySpendingProjection> topRows =
                List.of(categorySpending(topCat, "Rent", new BigDecimal("40.0000")));
        when(transactionRepository.calculatePeriodTotals(eq(userId), any(), any())).thenReturn(totals);
        when(transactionRepository.findCategorySpending(eq(userId), any(), any(), any(Pageable.class)))
                .thenReturn(topRows);

        // Act
        PeriodSummaryResponse response =
                analyticsService.getPeriodSummary(userId, new AnalyticsPeriodRequest(null, null));

        // Assert
        assertEquals(0, new BigDecimal("200.0000").compareTo(response.totalIncome()));
        assertEquals(0, new BigDecimal("50.0000").compareTo(response.totalExpense()));
        assertEquals(0, new BigDecimal("150.0000").compareTo(response.net()),
                "Net must be income minus expense");
        assertEquals(3L, response.transactionCount());
        assertEquals(topCat, response.topCategoryId());
        assertEquals("Rent", response.topCategoryName());
        assertEquals(0, new BigDecimal("40.0000").compareTo(response.topCategoryAmount()));
    }

    @Test
    @DisplayName("Should leave top category null and amount zero when there are no expenses")
    void getPeriodSummary_ShouldHaveNullTopCategory_WhenNoExpenses() {
        // Arrange
        PeriodTotalsProjection totals = org.mockito.Mockito.mock(PeriodTotalsProjection.class);
        when(totals.getTotalIncome()).thenReturn(new BigDecimal("200.0000"));
        when(totals.getTotalExpense()).thenReturn(BigDecimal.ZERO);
        when(totals.getTransactionCount()).thenReturn(1L);

        when(transactionRepository.calculatePeriodTotals(eq(userId), any(), any())).thenReturn(totals);
        when(transactionRepository.findCategorySpending(eq(userId), any(), any(), any(Pageable.class)))
                .thenReturn(List.of());

        // Act
        PeriodSummaryResponse response =
                analyticsService.getPeriodSummary(userId, new AnalyticsPeriodRequest(null, null));

        // Assert
        assertNull(response.topCategoryId(), "No expenses means no top category id");
        assertNull(response.topCategoryName());
        assertEquals(0, BigDecimal.ZERO.compareTo(response.topCategoryAmount()),
                "Top category amount must default to zero, not null");
    }
    //</editor-fold>

    //<editor-fold desc="Test getIncomeExpenseTrend">
    @Test
    @DisplayName("Should merge INCOME and EXPENSE rows of the same month into one chronological point")
    void getIncomeExpenseTrend_ShouldMergeRowsByMonth() {
        // Arrange: May has both income & expense; June has income only. Returned in order.
        List<MonthlyTrendProjection> rows = List.of(
                trendRow(2026, 5, TransactionType.INCOME, new BigDecimal("200.0000")),
                trendRow(2026, 5, TransactionType.EXPENSE, new BigDecimal("80.0000")),
                trendRow(2026, 6, TransactionType.INCOME, new BigDecimal("100.0000"))
        );
        when(transactionRepository.findMonthlyTrend(eq(userId), any(), any())).thenReturn(rows);

        // Act
        List<MonthlyTrendPointResponse> points =
                analyticsService.getIncomeExpenseTrend(userId, new AnalyticsPeriodRequest(null, null));

        // Assert
        assertEquals(2, points.size(), "Two distinct months must produce two points");

        MonthlyTrendPointResponse may = points.get(0);
        assertEquals(2026, may.year());
        assertEquals(5, may.month());
        assertEquals(0, new BigDecimal("200.0000").compareTo(may.totalIncome()));
        assertEquals(0, new BigDecimal("80.0000").compareTo(may.totalExpense()));
        assertEquals(0, new BigDecimal("120.0000").compareTo(may.net()),
                "May net must be 200 - 80");

        MonthlyTrendPointResponse june = points.get(1);
        assertEquals(6, june.month());
        assertEquals(0, new BigDecimal("100.0000").compareTo(june.totalIncome()));
        assertEquals(0, BigDecimal.ZERO.compareTo(june.totalExpense()),
                "A month with no expense rows must report zero expense");
        assertEquals(0, new BigDecimal("100.0000").compareTo(june.net()));
    }

    @Test
    @DisplayName("Should return an empty series when there are no transactions in the window")
    void getIncomeExpenseTrend_ShouldReturnEmpty_WhenNoRows() {
        // Arrange
        when(transactionRepository.findMonthlyTrend(eq(userId), any(), any())).thenReturn(List.of());

        // Act
        List<MonthlyTrendPointResponse> points =
                analyticsService.getIncomeExpenseTrend(userId, new AnalyticsPeriodRequest(null, null));

        // Assert
        assertTrue(points.isEmpty());
    }
    //</editor-fold>

    //<editor-fold desc="Test window validation">
    @Test
    @DisplayName("Should reject an inverted window where startDate is after endDate")
    void getSpendingByCategory_ShouldThrow_WhenStartAfterEnd() {
        // Arrange
        Instant start = Instant.parse("2026-06-10T00:00:00Z");
        Instant end = Instant.parse("2026-06-01T00:00:00Z");
        AnalyticsPeriodRequest request = new AnalyticsPeriodRequest(start, end);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> analyticsService.getSpendingByCategory(userId, request));
        assertEquals(AnalyticsErrorCode.INVALID_DATE_RANGE, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should accept a window with only one bound supplied")
    void getPeriodSummary_ShouldAccept_WhenOnlyOneBoundSupplied() {
        // Arrange
        PeriodTotalsProjection totals = org.mockito.Mockito.mock(PeriodTotalsProjection.class);
        when(totals.getTotalIncome()).thenReturn(BigDecimal.ZERO);
        when(totals.getTotalExpense()).thenReturn(BigDecimal.ZERO);
        when(totals.getTransactionCount()).thenReturn(0L);
        when(transactionRepository.calculatePeriodTotals(eq(userId), any(), any())).thenReturn(totals);
        when(transactionRepository.findCategorySpending(eq(userId), any(), any(), any(Pageable.class)))
                .thenReturn(List.of());

        AnalyticsPeriodRequest request =
                new AnalyticsPeriodRequest(Instant.parse("2026-06-01T00:00:00Z"), null);

        // Act & Assert: a missing upper bound must not be treated as an inverted range
        assertDoesNotThrow(() -> analyticsService.getPeriodSummary(userId, request));
    }
    //</editor-fold>
}

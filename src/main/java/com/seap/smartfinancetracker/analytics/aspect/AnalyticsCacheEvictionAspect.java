package com.seap.smartfinancetracker.analytics.aspect;

import com.seap.smartfinancetracker.analytics.cache.AnalyticsCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Invalidates a user's cached analytics whenever one of their transactions changes.
 * <p>
 * Like {@code TransactionEventAspect}, this aspect is ordered at
 * {@link Ordered#HIGHEST_PRECEDENCE} so it wraps the (lower-precedence) Spring transaction
 * interceptor. Its {@code @AfterReturning} advice therefore runs only once the surrounding
 * {@code @Transactional} boundary has committed — invalidating after commit (rather than via a
 * plain {@code @CacheEvict} inside the transaction) avoids the race where a concurrent read
 * repopulates the cache with not-yet-committed data.
 * </p>
 * <p>
 * A mutation that rolls back never reaches this advice, so the cache is left untouched — which
 * is correct, since nothing changed. Invalidation is scoped to the affected {@code userId}, so
 * one user's activity never evicts another user's cached analytics.
 * </p>
 */
@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class AnalyticsCacheEvictionAspect {

    private final AnalyticsCacheService analyticsCacheService;

    /**
     * Invalidates the user's analytics caches after a transaction is successfully created
     * (including transactions materialized by the recurring scheduler).
     *
     * @param userId the id of the user whose transaction was created (first method argument)
     */
    @AfterReturning("execution(* com.seap.smartfinancetracker.transaction.service.TransactionService.createTransaction(..)) "
            + "&& args(userId, ..)")
    public void evictOnCreate(UUID userId) {
        analyticsCacheService.invalidate(userId);
    }

    /**
     * Invalidates the user's analytics caches after a transaction is successfully updated.
     *
     * @param userId the id of the user whose transaction was updated (first method argument)
     */
    @AfterReturning("execution(* com.seap.smartfinancetracker.transaction.service.TransactionService.updateTransaction(..)) "
            + "&& args(userId, ..)")
    public void evictOnUpdate(UUID userId) {
        analyticsCacheService.invalidate(userId);
    }

    /**
     * Invalidates the user's analytics caches after a transaction is successfully (soft-)deleted.
     *
     * @param userId the id of the user whose transaction was deleted (first method argument)
     */
    @AfterReturning("execution(* com.seap.smartfinancetracker.transaction.service.TransactionService.deleteTransaction(..)) "
            + "&& args(userId, ..)")
    public void evictOnDelete(UUID userId) {
        analyticsCacheService.invalidate(userId);
    }
}

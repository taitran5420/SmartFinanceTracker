package com.seap.smartfinancetracker.transaction.job;

import com.seap.smartfinancetracker.transaction.service.RecurringTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

/**
 * Quartz Job responsible for triggering the evaluation and execution of due recurring transactions.
 * <p>
 * This class acts as the scheduled entry point for the automation engine. It leverages Spring's
 * {@link QuartzJobBean} integration to seamlessly inject application services into the Quartz context.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@DisallowConcurrentExecution
public class RecurringTransactionQuartzJob extends QuartzJobBean {
    private final RecurringTransactionService recurringTransactionService;

    /**
     * Executes the actual job logic upon being fired by the Quartz trigger.
     *
     * @param context the execution context containing runtime information about the trigger and job environment
     * @throws JobExecutionException if an unhandled exception occurs, instructing Quartz how to handle the failure
     */
    @Override
    protected void executeInternal(@NonNull JobExecutionContext context) throws JobExecutionException {
        log.debug("Quartz: Waking up to check for due recurring transactions...");
        try {
            log.info("Doing transaction here");
            recurringTransactionService.processDueRecurringTransactions();

        } catch (Exception e) {
            log.error("Quartz: Error occurred while executing Quartz job.", e);
            throw new JobExecutionException(e);
        }
    }
}

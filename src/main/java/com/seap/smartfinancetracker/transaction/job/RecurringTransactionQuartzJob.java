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

@Slf4j
@Component
@RequiredArgsConstructor
@DisallowConcurrentExecution
public class RecurringTransactionQuartzJob extends QuartzJobBean {
    private final RecurringTransactionService recurringTransactionService;

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

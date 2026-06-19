package com.seap.smartfinancetracker.transaction.config;

import com.seap.smartfinancetracker.transaction.job.RecurringTransactionQuartzJob;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Quartz Scheduler.
 * <p>
 * This class defines the necessary {@link JobDetail} and {@link Trigger} beans to
 * automate the execution of recurring transactions. By utilizing Quartz instead of
 * standard Spring scheduling, the application gains robust capabilities such as
 * database-backed persistence, clustering support, and advanced misfire handling.
 * </p>
 */
@Configuration
public class QuartzConfig {
    private static final String RECURRING_JOB_IDENTITY = "RecurringTransactionJob";
    private static final String RECURRING_TRIGGER_IDENTITY = "RecurringTransactionTrigger";

    private static final String RECURRING_JOB_DESCRIPTION= "Processes recurring transactions";
    private static final String RECURRING_TRIGGER_DESCRIPTION = "Cron-scheduled recurring transaction processing";

    /**
     * Defines the Quartz Job detailing the specific task to be executed.
     * <p>
     * <b>Design Choice:</b> Uses {@code storeDurably()} to ensure the job remains
     * stored in the database even if no active triggers are currently pointing to it.
     * This prevents the job from being orphaned or deleted accidentally.
     * </p>
     *
     * @return the configured {@link JobDetail} for recurring transactions
     */
    @Bean
    public JobDetail recurringTransactionJobDetail() {
        return JobBuilder.newJob(RecurringTransactionQuartzJob.class)
                .withIdentity(RECURRING_JOB_IDENTITY)
                .withDescription(RECURRING_JOB_DESCRIPTION)
                .storeDurably()
                .build();
    }

    /**
     * Defines the execution schedule (Trigger) for the recurring transaction job.
     * <p>
     * <b>Misfire Strategy:</b> Configured with {@code withMisfireHandlingInstructionDoNothing()}.
     * If the server is down and misses a scheduled fire time, this ensures the system
     * does not flood the server with all missed executions simultaneously upon restarting.
     * It simply skips the missed ones and waits for the next scheduled interval.
     * </p>
     *
     * @param recurringTransactionJobDetail the target job detail to be triggered
     * @return the configured Cron {@link Trigger}
     */
    @Bean
    public Trigger recurringTransactionTrigger(JobDetail recurringTransactionJobDetail,
                                               @Value("${recurring.cron:0 0/5 * * * ?}") String cronSchedule) {
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(cronSchedule)
                .withMisfireHandlingInstructionDoNothing();

        return TriggerBuilder.newTrigger()
                .forJob(recurringTransactionJobDetail)
                .withIdentity(RECURRING_TRIGGER_IDENTITY)
                .withDescription(RECURRING_TRIGGER_DESCRIPTION)
                .withSchedule(scheduleBuilder)
                .build();
    }
}

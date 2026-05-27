package com.seap.smartfinancetracker.transaction.config;

import com.seap.smartfinancetracker.transaction.job.RecurringTransactionQuartzJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {
    private static final String RECURRING_JOB_IDENTITY = "RecurringTransactionJob";
    private static final String RECURRING_TRIGGER_IDENTITY = "RecurringTransactionTrigger";

    private static final String RECURRING_JOB_DESCRIPTION= "Processes recurring transactions";
    private static final String RECURRING_TRIGGER_DESCRIPTION = "Cron scheduled recurring transaction every 5 minutes";

    private static final String RECURRING_CRON_SCHEDULE = "0 0/1 * * * ?";

    @Bean
    public JobDetail recurringTransactionJobDetail() {
        return JobBuilder.newJob(RecurringTransactionQuartzJob.class)
                .withIdentity(RECURRING_JOB_IDENTITY)
                .withDescription(RECURRING_JOB_DESCRIPTION)
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger recurringTransactionTrigger(JobDetail recurringTransactionJobDetail) {
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(RECURRING_CRON_SCHEDULE)
                .withMisfireHandlingInstructionDoNothing();

        return TriggerBuilder.newTrigger()
                .forJob(recurringTransactionJobDetail)
                .withIdentity(RECURRING_TRIGGER_IDENTITY)
                .withDescription(RECURRING_TRIGGER_DESCRIPTION)
                .withSchedule(scheduleBuilder)
                .build();
    }
}

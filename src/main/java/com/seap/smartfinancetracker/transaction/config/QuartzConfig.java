package com.seap.smartfinancetracker.transaction.config;

import com.seap.smartfinancetracker.transaction.job.RecurringTransactionQuartzJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {
    private static final String RECURRING_JOB_IDENTITY = "RecurringTransactionJob";

    @Bean
    public JobDetail recurringTransactionJobDetail() {
        return JobBuilder.newJob(RecurringTransactionQuartzJob.class)
                .withIdentity(RECURRING_JOB_IDENTITY)
                .withDescription("Processes recurring transactions")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger recurringTransactionTrigger(JobDetail recurringTransactionJobDetail) {
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule("0 0/1 * * * ?")
                .withMisfireHandlingInstructionDoNothing();

        return TriggerBuilder.newTrigger()
                .forJob(recurringTransactionJobDetail)
                .withIdentity(RECURRING_JOB_IDENTITY + "Trigger")
                .withDescription("Cron scheduled recurring transaction every 5 minutes")
                .withSchedule(scheduleBuilder)
                .build();
    }
}

package com.seap.smartfinancetracker.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class ThreadPoolConfig {
    public static final String TASK_EXECUTOR_BEAN_NAME = "taskExecutor";
    private final static int TASK_EXECUTOR_CORE_POOL_SIZE = 10;
    private final static int TASK_EXECUTOR_MAX_POOL_SIZE = 50;
    private final static int TASK_EXECUTOR_QUEUE_CAPACITY = 500;
    private final static String TASK_EXECUTOR_THREAD_NAME_PREFIX = "Normal-Task-Executor-";

    public static final String NOTIFICATION_EXECUTOR_BEAN_NAME = "notificationTaskExecutor";
    private final static int NOTIFICATION_EXECUTOR_CORE_POOL_SIZE = 5;
    private final static int NOTIFICATION_EXECUTOR_MAX_POOL_SIZE = 10;
    private final static int NOTIFICATION_EXECUTOR_QUEUE_CAPACITY = 1000;
    private final static String NOTIFICATION_EXECUTOR_THREAD_NAME_PREFIX = "Notification-Task-Executor-";

    public static final String RECURRING_TASK_EXECUTOR_BEAN_NAME = "recurringTaskExecutor";
    private final static int RECURRING_TASK_EXECUTOR_CORE_POOL_SIZE = 5;
    private final static int RECURRING_TASK_EXECUTOR_MAX_POOL_SIZE = 10;
    private final static int RECURRING_TASK_EXECUTOR_QUEUE_CAPACITY = 100;
    private final static String RECURRING_TASK_EXECUTOR_THREAD_NAME_PREFIX = "Recurring-Task-Executor-";

    @Bean(name = TASK_EXECUTOR_BEAN_NAME)
    @Primary
    public Executor normalTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(TASK_EXECUTOR_CORE_POOL_SIZE);
        executor.setMaxPoolSize(TASK_EXECUTOR_MAX_POOL_SIZE);
        executor.setQueueCapacity(TASK_EXECUTOR_QUEUE_CAPACITY);
        executor.setThreadNamePrefix(TASK_EXECUTOR_THREAD_NAME_PREFIX);
        executor.initialize();
        return executor;
    }

    @Bean(name = NOTIFICATION_EXECUTOR_BEAN_NAME)
    public Executor notificationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(NOTIFICATION_EXECUTOR_CORE_POOL_SIZE);
        executor.setMaxPoolSize(NOTIFICATION_EXECUTOR_MAX_POOL_SIZE);
        executor.setQueueCapacity(NOTIFICATION_EXECUTOR_QUEUE_CAPACITY);
        executor.setThreadNamePrefix(NOTIFICATION_EXECUTOR_THREAD_NAME_PREFIX);
        executor.initialize();
        return executor;
    }

    @Bean(name = RECURRING_TASK_EXECUTOR_BEAN_NAME)
    public Executor recurringTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(RECURRING_TASK_EXECUTOR_CORE_POOL_SIZE);
        executor.setMaxPoolSize(RECURRING_TASK_EXECUTOR_MAX_POOL_SIZE);
        executor.setQueueCapacity(RECURRING_TASK_EXECUTOR_QUEUE_CAPACITY);
        executor.setThreadNamePrefix(RECURRING_TASK_EXECUTOR_THREAD_NAME_PREFIX);
        executor.initialize();
        return executor;
    }

}

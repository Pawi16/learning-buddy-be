package dev.pawin.backend_learning_buddy.common.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(AsyncConfig.class);

    @Value("${quiz.async.core-pool-size:5}")
    private int corePoolSize;

    @Value("${quiz.async.max-pool-size:10}")
    private int maxPoolSize;

    @Value("${quiz.async.queue-capacity:100}")
    private int queueCapacity;

    @Bean(name = "quizTaskExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);      // Match AI service concurrency limit
        executor.setMaxPoolSize(maxPoolSize);        // Scale up under load
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("QuizGen-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            logger.error("Async method {} threw exception: {}",
                    method.getName(),
                    throwable.getMessage(),
                    throwable);
        };
    }
}

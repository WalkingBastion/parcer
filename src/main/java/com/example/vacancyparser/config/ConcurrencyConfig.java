package com.example.vacancyparser.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConcurrencyConfig {

    @Bean(name = "parserExecutor", destroyMethod = "shutdown")
    public ExecutorService parserExecutor(AppProperties properties) {
        AtomicInteger sequence = new AtomicInteger();
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, "parser-worker-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        AppProperties.Pool pool = properties.pool();
        return new ThreadPoolExecutor(
                pool.core(),
                pool.max(),
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(pool.queue()),
                factory,
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Bean(name = "queryPool", destroyMethod = "shutdown")
    public ForkJoinPool queryPool(AppProperties properties) {
        return new ForkJoinPool(properties.pool().queryParallelism());
    }
}

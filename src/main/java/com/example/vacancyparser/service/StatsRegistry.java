package com.example.vacancyparser.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

import org.springframework.stereotype.Component;

@Component
public class StatsRegistry {

    private final LongAdder succeeded = new LongAdder();
    private final LongAdder failed = new LongAdder();
    private final LongAdder refreshRuns = new LongAdder();
    private final LongAdder refreshedItems = new LongAdder();
    private final ConcurrentHashMap<String, LongAdder> tasksByThread = new ConcurrentHashMap<>();
    private final AtomicReference<Instant> lastRefreshAt = new AtomicReference<>();

    public void success(String thread) {
        succeeded.increment();
        tasksByThread.computeIfAbsent(thread, key -> new LongAdder()).increment();
    }

    public void failure(String thread) {
        failed.increment();
        tasksByThread.computeIfAbsent(thread, key -> new LongAdder()).increment();
    }

    public void refreshFinished(int items) {
        refreshRuns.increment();
        refreshedItems.add(items);
        lastRefreshAt.set(Instant.now());
    }

    public Map<String, Object> snapshot() {
        Map<String, Long> threads = new TreeMap<>();
        tasksByThread.forEach((name, counter) -> threads.put(name, counter.sum()));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("succeeded", succeeded.sum());
        result.put("failed", failed.sum());
        result.put("refreshRuns", refreshRuns.sum());
        result.put("refreshedVacancies", refreshedItems.sum());
        result.put("lastRefreshAt", lastRefreshAt.get());
        result.put("tasksByThread", threads);
        return result;
    }
}

package com.example.vacancyparser.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import com.example.vacancyparser.model.Vacancy;
import com.example.vacancyparser.repository.VacancyRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class VacancyRefreshJob {

    private static final Logger log = LoggerFactory.getLogger(VacancyRefreshJob.class);

    private final VacancyRepository repository;
    private final VacancyService service;
    private final StatsRegistry stats;
    private final ExecutorService executor;

    public VacancyRefreshJob(VacancyRepository repository,
                             VacancyService service,
                             StatsRegistry stats,
                             @Qualifier("parserExecutor") ExecutorService executor) {
        this.repository = repository;
        this.service = service;
        this.stats = stats;
        this.executor = executor;
    }

    @Scheduled(
            initialDelayString = "${app.refresh.initial-delay-ms:20000}",
            fixedDelayString = "${app.refresh.interval-ms:60000}")
    public void refresh() {
        try {
            List<String> urls = repository.findAll().stream().map(Vacancy::getUrl).toList();
            List<CompletableFuture<Void>> futures = urls.stream()
                    .map(url -> CompletableFuture.runAsync(() -> refreshOne(url), executor))
                    .toList();
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            stats.refreshFinished(urls.size());
            log.info("Scheduled refresh finished: {} vacancies re-checked", urls.size());
        } catch (RuntimeException e) {
            log.error("Scheduled refresh failed", e);
        }
    }

    private void refreshOne(String url) {
        String thread = Thread.currentThread().getName();
        try {
            service.process(url);
            stats.success(thread);
        } catch (Exception e) {
            stats.failure(thread);
            log.warn("Refresh failed for {}: {}", url, e.toString());
        }
    }
}

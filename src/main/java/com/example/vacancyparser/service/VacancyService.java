package com.example.vacancyparser.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import com.example.vacancyparser.config.AppProperties;
import com.example.vacancyparser.dto.CollectResult;
import com.example.vacancyparser.dto.VacancyDto;
import com.example.vacancyparser.model.ParsedVacancy;
import com.example.vacancyparser.model.Vacancy;
import com.example.vacancyparser.parser.VacancyExtractor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class VacancyService {

    private static final Logger log = LoggerFactory.getLogger(VacancyService.class);

    private final PageFetcher fetcher;
    private final VacancyExtractor extractor;
    private final VacancyStore store;
    private final StatsRegistry stats;
    private final ExecutorService executor;
    private final int maxUrls;

    public VacancyService(PageFetcher fetcher,
                          VacancyExtractor extractor,
                          VacancyStore store,
                          StatsRegistry stats,
                          @Qualifier("parserExecutor") ExecutorService executor,
                          AppProperties properties) {
        this.fetcher = fetcher;
        this.extractor = extractor;
        this.store = store;
        this.stats = stats;
        this.executor = executor;
        this.maxUrls = properties.fetch().maxUrlsPerRequest();
    }

    public List<CollectResult> collect(Collection<String> urls) {
        return collectAsync(urls).join();
    }

    public CompletableFuture<List<CollectResult>> collectAsync(Collection<String> rawUrls) {
        if (rawUrls == null || rawUrls.isEmpty()) {
            throw new IllegalArgumentException("Provide 'url' or a non-empty 'urls' array");
        }
        if (rawUrls.size() > maxUrls) {
            throw new IllegalArgumentException("Too many URLs: " + rawUrls.size() + " (max " + maxUrls + ")");
        }
        Set<String> seen = new LinkedHashSet<>();
        List<CompletableFuture<CollectResult>> futures = new ArrayList<>();
        for (String raw : rawUrls) {
            String url;
            try {
                url = normalize(raw);
            } catch (IllegalArgumentException e) {
                futures.add(CompletableFuture.completedFuture(
                        CollectResult.failure(raw, e.getMessage(), Thread.currentThread().getName())));
                continue;
            }
            if (seen.add(url)) {
                futures.add(submit(url));
            }
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(ignored -> futures.stream().map(CompletableFuture::join).toList());
    }

    public Vacancy process(String url) {
        String html = fetcher.fetch(url);
        ParsedVacancy parsed = extractor.extract(html, url);
        return store.upsert(url, parsed);
    }

    private CompletableFuture<CollectResult> submit(String url) {
        return CompletableFuture.supplyAsync(() -> run(url), executor);
    }

    private CollectResult run(String url) {
        String thread = Thread.currentThread().getName();
        try {
            Vacancy vacancy = process(url);
            stats.success(thread);
            return CollectResult.ok(url, VacancyDto.from(vacancy), thread);
        } catch (Exception e) {
            stats.failure(thread);
            log.warn("Failed to process {}: {}", url, e.toString());
            return CollectResult.failure(url, e.getClass().getSimpleName() + ": " + e.getMessage(), thread);
        }
    }

    private static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("URL must not be blank");
        }
        URI uri = URI.create(raw.trim());
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("Only http/https URLs are supported: " + raw);
        }
        if (uri.getHost() == null) {
            throw new IllegalArgumentException("URL has no host: " + raw);
        }
        return uri.toString();
    }
}

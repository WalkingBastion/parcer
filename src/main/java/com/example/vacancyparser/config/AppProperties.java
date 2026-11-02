package com.example.vacancyparser.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @DefaultValue Pool pool,
        @DefaultValue Fetch fetch,
        @DefaultValue("true") boolean seedDemo) {

    public record Pool(
            @DefaultValue("8") int core,
            @DefaultValue("16") int max,
            @DefaultValue("200") int queue,
            @DefaultValue("4") int queryParallelism) {
    }

    public record Fetch(
            @DefaultValue("10000") long timeoutMs,
            @DefaultValue("100") int maxUrlsPerRequest,
            @DefaultValue("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36") String userAgent) {
    }
}

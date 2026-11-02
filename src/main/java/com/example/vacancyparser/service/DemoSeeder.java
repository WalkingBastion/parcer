package com.example.vacancyparser.service;

import java.util.ArrayList;
import java.util.List;

import com.example.vacancyparser.config.AppProperties;
import com.example.vacancyparser.dto.CollectResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class DemoSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoSeeder.class);

    private final VacancyService service;
    private final AppProperties properties;
    private final Environment environment;

    public DemoSeeder(VacancyService service, AppProperties properties, Environment environment) {
        this.service = service;
        this.properties = properties;
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        if (!properties.seedDemo()) {
            return;
        }
        String base = "http://localhost:" + environment.getProperty("local.server.port", "8080");
        List<String> urls = new ArrayList<>(List.of(
                base + "/demo/hh.html",
                base + "/demo/superjob.html",
                base + "/demo/habr.html"));
        for (int i = 1; i <= 6; i++) {
            urls.add(base + "/demo/vacancy/" + i);
        }
        service.collectAsync(urls).thenAccept(results -> {
            long ok = results.stream().filter(CollectResult::success).count();
            log.info("Demo seed finished: {} of {} pages parsed", ok, results.size());
        });
    }
}

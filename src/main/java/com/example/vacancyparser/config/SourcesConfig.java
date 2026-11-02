package com.example.vacancyparser.config;

import com.example.vacancyparser.parser.SelectorSource;
import com.example.vacancyparser.parser.VacancySource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class SourcesConfig {

    @Bean
    @Order(10)
    public VacancySource hhSource() {
        return new SelectorSource(new SelectorSource.Selectors(
                "h1[data-qa=vacancy-title]",
                "[data-qa=vacancy-company-name]",
                "[data-qa=vacancy-salary]",
                "[data-qa=vacancy-description]",
                "[data-qa=vacancy-view-location], [data-qa=vacancy-view-raw-address]",
                ".vacancy-creation-time-redesigned, [data-qa=vacancy-creation-time]"));
    }

    @Bean
    @Order(11)
    public VacancySource superJobSource() {
        return new SelectorSource(new SelectorSource.Selectors(
                "h1.f-test-vacancy-title, h1[class*=VacancyView], h1[class*=vacancy-title]",
                "a[href*='/clients/'], .f-test-text-company-item-title",
                ".f-test-text-vacancy-item-salary, span[class*=salary]",
                ".f-test-vacancy-description, [class*=vacancy-description]",
                ".f-test-text-vacancy-item-location, [class*=location]",
                ".f-test-text-vacancy-item-created, [class*=publication-time]"));
    }

    @Bean
    @Order(12)
    public VacancySource habrSource() {
        return new SelectorSource(new SelectorSource.Selectors(
                "h1.page-title__title",
                ".company_name a, .vacancy-company__name a",
                ".basic-salary",
                ".vacancy-description__text",
                ".inline-list a[href*='city_id']",
                "time[datetime]"));
    }
}

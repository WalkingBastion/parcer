package com.example.vacancyparser.parser;

import java.util.Optional;

import com.example.vacancyparser.model.ParsedVacancy;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(100)
public class FallbackSource implements VacancySource {

    @Override
    public Optional<ParsedVacancy> extract(Document document, String url) {
        Element heading = document.selectFirst("h1");
        String title = Texts.firstNonBlank(
                meta(document, "meta[property=og:title]"),
                heading != null ? heading.text() : null,
                document.title());
        String description = Texts.firstNonBlank(
                meta(document, "meta[property=og:description]"),
                meta(document, "meta[name=description]"));
        String published = meta(document, "meta[property=article:published_time]");
        return Optional.of(new ParsedVacancy(title, null, null, description, null, Dates.normalize(published)));
    }

    private static String meta(Document document, String selector) {
        Element element = document.selectFirst(selector);
        return element == null ? null : element.attr("content");
    }
}

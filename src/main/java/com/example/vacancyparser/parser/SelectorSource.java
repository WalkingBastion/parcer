package com.example.vacancyparser.parser;

import java.util.Optional;

import com.example.vacancyparser.model.ParsedVacancy;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class SelectorSource implements VacancySource {

    public record Selectors(String title, String company, String salary, String requirements, String city,
                            String postedDate) {
    }

    private final Selectors selectors;

    public SelectorSource(Selectors selectors) {
        this.selectors = selectors;
    }

    @Override
    public Optional<ParsedVacancy> extract(Document document, String url) {
        Element titleElement = document.selectFirst(selectors.title());
        if (titleElement == null) {
            return Optional.empty();
        }
        return Optional.of(new ParsedVacancy(
                Texts.clean(titleElement.text()),
                read(document, selectors.company()),
                read(document, selectors.salary()),
                read(document, selectors.requirements()),
                read(document, selectors.city()),
                Dates.normalize(read(document, selectors.postedDate()))));
    }

    private static String read(Document document, String selector) {
        Element element = document.selectFirst(selector);
        if (element == null) {
            return null;
        }
        return Texts.clean(element.hasAttr("datetime") ? element.attr("datetime") : element.text());
    }
}

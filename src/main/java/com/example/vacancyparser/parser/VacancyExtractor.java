package com.example.vacancyparser.parser;

import java.util.List;

import com.example.vacancyparser.model.ParsedVacancy;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class VacancyExtractor {

    private static final Logger log = LoggerFactory.getLogger(VacancyExtractor.class);

    private final List<VacancySource> sources;

    public VacancyExtractor(List<VacancySource> sources) {
        this.sources = List.copyOf(sources);
    }

    public ParsedVacancy extract(String html, String url) {
        Document document = Jsoup.parse(html, url);
        ParsedVacancy result = ParsedVacancy.EMPTY;
        for (VacancySource source : sources) {
            try {
                result = result.merge(source.extract(document, url).orElse(ParsedVacancy.EMPTY));
            } catch (RuntimeException e) {
                log.debug("Source {} failed for {}: {}", source.getClass().getSimpleName(), url, e.getMessage());
            }
        }
        if (result.title() == null) {
            throw new ParsingException("Vacancy title was not found at " + url);
        }
        return result;
    }
}

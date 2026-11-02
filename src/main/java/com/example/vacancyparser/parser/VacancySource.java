package com.example.vacancyparser.parser;

import java.util.Optional;

import com.example.vacancyparser.model.ParsedVacancy;

import org.jsoup.nodes.Document;

public interface VacancySource {

    Optional<ParsedVacancy> extract(Document document, String url);
}

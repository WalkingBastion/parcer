package com.example.vacancyparser.model;

public record ParsedVacancy(
        String title,
        String company,
        String salary,
        String requirements,
        String city,
        String postedDate) {

    public static final ParsedVacancy EMPTY = new ParsedVacancy(null, null, null, null, null, null);

    public ParsedVacancy merge(ParsedVacancy other) {
        return new ParsedVacancy(
                pick(title, other.title),
                pick(company, other.company),
                pick(salary, other.salary),
                pick(requirements, other.requirements),
                pick(city, other.city),
                pick(postedDate, other.postedDate));
    }

    private static String pick(String primary, String secondary) {
        return primary != null && !primary.isBlank() ? primary : secondary;
    }
}

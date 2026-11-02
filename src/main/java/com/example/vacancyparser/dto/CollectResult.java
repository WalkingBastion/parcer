package com.example.vacancyparser.dto;

public record CollectResult(String url, boolean success, String error, VacancyDto vacancy, String processedBy) {

    public static CollectResult ok(String url, VacancyDto vacancy, String thread) {
        return new CollectResult(url, true, null, vacancy, thread);
    }

    public static CollectResult failure(String url, String error, String thread) {
        return new CollectResult(url, false, error, null, thread);
    }
}

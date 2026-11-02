package com.example.vacancyparser.dto;

import java.time.Instant;

import com.example.vacancyparser.model.Vacancy;

public record VacancyDto(
        Long id,
        String url,
        String title,
        String company,
        String salary,
        String requirements,
        String city,
        String postedDate,
        Instant createdAt,
        Instant updatedAt) {

    public static VacancyDto from(Vacancy v) {
        return new VacancyDto(
                v.getId(),
                v.getUrl(),
                v.getTitle(),
                v.getCompany(),
                v.getSalary(),
                v.getRequirements(),
                v.getCity(),
                v.getPostedDate(),
                v.getCreatedAt(),
                v.getUpdatedAt());
    }
}

package com.example.vacancyparser.repository;

import java.util.Optional;

import com.example.vacancyparser.model.Vacancy;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VacancyRepository extends JpaRepository<Vacancy, Long> {

    Optional<Vacancy> findByUrl(String url);
}

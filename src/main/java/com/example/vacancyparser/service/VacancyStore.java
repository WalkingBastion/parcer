package com.example.vacancyparser.service;

import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

import com.example.vacancyparser.model.ParsedVacancy;
import com.example.vacancyparser.model.Vacancy;
import com.example.vacancyparser.parser.Texts;
import com.example.vacancyparser.repository.VacancyRepository;

import org.springframework.stereotype.Service;

@Service
public class VacancyStore {

    private static final int STRIPES = 64;

    private final VacancyRepository repository;
    private final ReentrantLock[] locks = new ReentrantLock[STRIPES];

    public VacancyStore(VacancyRepository repository) {
        this.repository = repository;
        for (int i = 0; i < STRIPES; i++) {
            locks[i] = new ReentrantLock();
        }
    }

    public Vacancy upsert(String url, ParsedVacancy parsed) {
        ReentrantLock lock = locks[Math.floorMod(url.hashCode(), STRIPES)];
        lock.lock();
        try {
            Vacancy vacancy = repository.findByUrl(url).orElseGet(Vacancy::new);
            vacancy.setUrl(url);
            vacancy.setTitle(Texts.truncate(parsed.title(), 500));
            vacancy.setCompany(Texts.truncate(parsed.company(), 300));
            vacancy.setSalary(Texts.truncate(parsed.salary(), 300));
            vacancy.setRequirements(Texts.truncate(parsed.requirements(), 4000));
            vacancy.setCity(Texts.truncate(parsed.city(), 200));
            vacancy.setPostedDate(Texts.truncate(parsed.postedDate(), 64));
            Instant now = Instant.now();
            if (vacancy.getCreatedAt() == null) {
                vacancy.setCreatedAt(now);
            }
            vacancy.setUpdatedAt(now);
            return repository.save(vacancy);
        } finally {
            lock.unlock();
        }
    }
}

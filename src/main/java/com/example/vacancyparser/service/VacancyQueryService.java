package com.example.vacancyparser.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.example.vacancyparser.dto.PageResult;
import com.example.vacancyparser.dto.VacancyDto;
import com.example.vacancyparser.model.Vacancy;
import com.example.vacancyparser.repository.VacancyRepository;

import org.springframework.stereotype.Service;

@Service
public class VacancyQueryService {

    private static final String ALLOWED_SORT =
            "id, title, company, city, salary, postedDate, createdAt, updatedAt";

    private final VacancyRepository repository;
    private final ForkJoinPool queryPool;

    public VacancyQueryService(VacancyRepository repository, ForkJoinPool queryPool) {
        this.repository = repository;
        this.queryPool = queryPool;
    }

    private record Computed(List<Vacancy> sorted, Map<String, Long> cityCounts) {
    }

    public PageResult<VacancyDto> query(int page, int size, String sortBy, String direction,
                                        String city, String company, String text) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
        boolean descending;
        if ("asc".equalsIgnoreCase(direction)) {
            descending = false;
        } else if ("desc".equalsIgnoreCase(direction)) {
            descending = true;
        } else {
            throw new IllegalArgumentException("direction must be 'asc' or 'desc'");
        }

        Comparator<Vacancy> comparator = comparator(sortBy, descending).thenComparing(Vacancy::getId);
        Predicate<Vacancy> filter = vacancy ->
                contains(vacancy.getCity(), city)
                        && contains(vacancy.getCompany(), company)
                        && (text == null || text.isBlank()
                        || contains(vacancy.getTitle(), text)
                        || contains(vacancy.getRequirements(), text));

        List<Vacancy> all = repository.findAll();
        Computed computed;
        try {
            computed = queryPool.submit(() -> {
                List<Vacancy> sorted = all.parallelStream()
                        .filter(filter)
                        .sorted(comparator)
                        .collect(Collectors.toList());
                Map<String, Long> counts = sorted.parallelStream()
                        .collect(Collectors.groupingByConcurrent(
                                vacancy -> vacancy.getCity() == null ? "unknown" : vacancy.getCity(),
                                Collectors.counting()));
                return new Computed(sorted, new TreeMap<>(counts));
            }).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Query interrupted", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Query failed", e.getCause());
        }

        int total = computed.sorted().size();
        int from = (int) Math.min((long) page * size, total);
        int to = Math.min(from + size, total);
        List<VacancyDto> content = computed.sorted().subList(from, to).stream().map(VacancyDto::from).toList();
        int totalPages = (int) Math.ceil(total / (double) size);
        return new PageResult<>(content, page, size, total, totalPages, sortBy, descending ? "desc" : "asc",
                computed.cityCounts());
    }

    private static Comparator<Vacancy> comparator(String sortBy, boolean descending) {
        return switch (sortBy) {
            case "id" -> directed(Comparator.comparing(Vacancy::getId), descending);
            case "title" -> byText(Vacancy::getTitle, descending);
            case "company" -> byText(Vacancy::getCompany, descending);
            case "city" -> byText(Vacancy::getCity, descending);
            case "salary" -> byText(Vacancy::getSalary, descending);
            case "postedDate" -> byText(Vacancy::getPostedDate, descending);
            case "createdAt" -> directed(Comparator.comparing(Vacancy::getCreatedAt), descending);
            case "updatedAt" -> directed(Comparator.comparing(Vacancy::getUpdatedAt), descending);
            default -> throw new IllegalArgumentException(
                    "Unsupported sortBy '" + sortBy + "'. Allowed: " + ALLOWED_SORT);
        };
    }

    private static Comparator<Vacancy> directed(Comparator<Vacancy> comparator, boolean descending) {
        return descending ? comparator.reversed() : comparator;
    }

    private static Comparator<Vacancy> byText(Function<Vacancy, String> extractor, boolean descending) {
        Comparator<String> base = String.CASE_INSENSITIVE_ORDER;
        if (descending) {
            base = base.reversed();
        }
        return Comparator.comparing(extractor, Comparator.nullsLast(base));
    }

    private static boolean contains(String value, String needle) {
        if (needle == null || needle.isBlank()) {
            return true;
        }
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }
}

package com.example.vacancyparser.web;

import java.util.List;
import java.util.Map;

import com.example.vacancyparser.dto.CollectResult;
import com.example.vacancyparser.dto.PageResult;
import com.example.vacancyparser.dto.PostRequest;
import com.example.vacancyparser.dto.PostResponse;
import com.example.vacancyparser.dto.VacancyDto;
import com.example.vacancyparser.service.StatsRegistry;
import com.example.vacancyparser.service.VacancyQueryService;
import com.example.vacancyparser.service.VacancyService;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VacancyController {

    private final VacancyService vacancyService;
    private final VacancyQueryService queryService;
    private final StatsRegistry stats;

    public VacancyController(VacancyService vacancyService, VacancyQueryService queryService, StatsRegistry stats) {
        this.vacancyService = vacancyService;
        this.queryService = queryService;
        this.stats = stats;
    }

    @PostMapping(value = "/post", consumes = MediaType.APPLICATION_JSON_VALUE)
    public PostResponse post(@RequestBody PostRequest request) {
        long started = System.nanoTime();
        List<CollectResult> results = vacancyService.collect(request.allUrls());
        long succeeded = results.stream().filter(CollectResult::success).count();
        long elapsedMs = (System.nanoTime() - started) / 1_000_000;
        return new PostResponse(results.size(), succeeded, results.size() - succeeded, elapsedMs, results);
    }

    @GetMapping("/answer")
    public PageResult<VacancyDto> answer(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortBy", defaultValue = "id") String sortBy,
            @RequestParam(name = "direction", defaultValue = "asc") String direction,
            @RequestParam(name = "city", required = false) String city,
            @RequestParam(name = "company", required = false) String company,
            @RequestParam(name = "q", required = false) String text) {
        return queryService.query(page, size, sortBy, direction, city, company, text);
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return stats.snapshot();
    }
}

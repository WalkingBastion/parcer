package com.example.vacancyparser.dto;

import java.util.List;
import java.util.Map;

public record PageResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        String sortBy,
        String direction,
        Map<String, Long> cityCounts) {
}

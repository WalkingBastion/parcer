package com.example.vacancyparser.dto;

import java.util.List;

public record PostResponse(int requested, long succeeded, long failed, long elapsedMs, List<CollectResult> results) {
}

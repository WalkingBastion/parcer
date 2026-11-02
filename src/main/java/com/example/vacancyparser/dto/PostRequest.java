package com.example.vacancyparser.dto;

import java.util.ArrayList;
import java.util.List;

public record PostRequest(String url, List<String> urls) {

    public List<String> allUrls() {
        List<String> result = new ArrayList<>();
        if (url != null && !url.isBlank()) {
            result.add(url);
        }
        if (urls != null) {
            result.addAll(urls);
        }
        return result;
    }
}

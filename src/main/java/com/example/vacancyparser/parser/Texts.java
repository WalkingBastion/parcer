package com.example.vacancyparser.parser;

import org.jsoup.Jsoup;

public final class Texts {

    private Texts() {
    }

    public static String clean(String value) {
        if (value == null) {
            return null;
        }
        String result = value.replace('\u00a0', ' ').replaceAll("\\s+", " ").trim();
        return result.isEmpty() ? null : result;
    }

    public static String firstNonBlank(String... values) {
        for (String value : values) {
            String cleaned = clean(value);
            if (cleaned != null) {
                return cleaned;
            }
        }
        return null;
    }

    public static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max - 3) + "...";
    }

    public static String html(String value) {
        if (value == null) {
            return null;
        }
        return clean(Jsoup.parse(value).text());
    }
}

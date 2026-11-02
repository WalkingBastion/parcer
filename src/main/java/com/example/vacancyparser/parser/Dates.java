package com.example.vacancyparser.parser;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Dates {

    private static final Map<String, Integer> MONTHS = Map.ofEntries(
            Map.entry("января", 1),
            Map.entry("февраля", 2),
            Map.entry("марта", 3),
            Map.entry("апреля", 4),
            Map.entry("мая", 5),
            Map.entry("июня", 6),
            Map.entry("июля", 7),
            Map.entry("августа", 8),
            Map.entry("сентября", 9),
            Map.entry("октября", 10),
            Map.entry("ноября", 11),
            Map.entry("декабря", 12));

    private static final Pattern ISO = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern RUSSIAN = Pattern.compile("(\\d{1,2})\\s+([а-яё]+)\\s+(\\d{4})",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern DOTTED = Pattern.compile("(\\d{1,2})\\.(\\d{1,2})\\.(\\d{4})");

    private Dates() {
    }

    public static String normalize(String raw) {
        String value = Texts.clean(raw);
        if (value == null) {
            return null;
        }
        try {
            Matcher iso = ISO.matcher(value);
            if (iso.find()) {
                return iso.group();
            }
            Matcher russian = RUSSIAN.matcher(value);
            if (russian.find()) {
                Integer month = MONTHS.get(russian.group(2).toLowerCase(Locale.ROOT));
                if (month != null) {
                    return LocalDate.of(Integer.parseInt(russian.group(3)), month, Integer.parseInt(russian.group(1))).toString();
                }
            }
            Matcher dotted = DOTTED.matcher(value);
            if (dotted.find()) {
                return LocalDate.of(Integer.parseInt(dotted.group(3)), Integer.parseInt(dotted.group(2)),
                        Integer.parseInt(dotted.group(1))).toString();
            }
        } catch (DateTimeException | NumberFormatException e) {
            return value;
        }
        return value;
    }
}

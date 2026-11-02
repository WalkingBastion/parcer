package com.example.vacancyparser.web;

import java.time.LocalDate;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/demo")
public class DemoVacancyController {

    private static final String[] TITLES = {
            "Senior Java Developer", "Backend Engineer", "Data Engineer", "DevOps Engineer",
            "QA Automation Engineer", "Kotlin Developer", "Platform Engineer"};
    private static final String[] COMPANIES = {
            "Northwind Labs", "Contoso Systems", "Globex Digital", "Initech Cloud", "Umbrella Soft"};
    private static final String[] CITIES = {
            "Amsterdam", "Berlin", "Moscow", "Warsaw", "Lisbon", "Saint Petersburg"};
    private static final String[] SKILLS = {
            "Java 17 and Spring Boot", "SQL and PostgreSQL", "Docker and Kubernetes",
            "Kafka or RabbitMQ", "Gradle or Maven", "Concurrency and JVM tuning"};

    @GetMapping(value = "/vacancy/{id}", produces = "text/html;charset=UTF-8")
    public String vacancy(@PathVariable("id") int id,
                          @RequestParam(name = "delayMs", defaultValue = "400") long delayMs)
            throws InterruptedException {
        Thread.sleep(Math.min(Math.max(delayMs, 0L), 5000L));
        String title = TITLES[Math.floorMod(id, TITLES.length)];
        String company = COMPANIES[Math.floorMod(id, COMPANIES.length)];
        String city = CITIES[Math.floorMod(id, CITIES.length)];
        String first = SKILLS[Math.floorMod(id, SKILLS.length)];
        String second = SKILLS[Math.floorMod(id + 3, SKILLS.length)];
        LocalDate posted = LocalDate.of(2026, 10, 1).plusDays(Math.floorMod(id, 28));
        int min = 3000 + Math.floorMod(id, 7) * 250;
        int max = min + 1500;
        return """
                <!DOCTYPE html>
                <html lang="en"><head><meta charset="UTF-8"><title>%s</title>
                <script type="application/ld+json">
                {"@context":"https://schema.org","@type":"JobPosting","title":"%s","datePosted":"%s",
                "description":"<p>Requirements:</p><ul><li>%s</li><li>%s</li></ul>",
                "hiringOrganization":{"@type":"Organization","name":"%s"},
                "jobLocation":{"@type":"Place","address":{"@type":"PostalAddress","addressLocality":"%s"}},
                "baseSalary":{"@type":"MonetaryAmount","currency":"EUR",
                "value":{"@type":"QuantitativeValue","minValue":%d,"maxValue":%d,"unitText":"MONTH"}}}
                </script></head>
                <body><h1>%s</h1></body></html>
                """.formatted(title, title, posted, first, second, company, city, min, max, title);
    }
}

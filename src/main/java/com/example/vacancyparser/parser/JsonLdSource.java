package com.example.vacancyparser.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.example.vacancyparser.model.ParsedVacancy;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class JsonLdSource implements VacancySource {

    private final ObjectMapper mapper;

    public JsonLdSource(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<ParsedVacancy> extract(Document document, String url) {
        for (Element script : document.select("script[type='application/ld+json']")) {
            try {
                JsonNode posting = findPosting(mapper.readTree(script.data()));
                if (posting != null) {
                    return Optional.of(toVacancy(posting));
                }
            } catch (Exception e) {
                continue;
            }
        }
        return Optional.empty();
    }

    private JsonNode findPosting(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                JsonNode found = findPosting(child);
                if (found != null) {
                    return found;
                }
            }
            return null;
        }
        if (node.isObject()) {
            if (isJobPosting(node.get("@type"))) {
                return node;
            }
            return findPosting(node.get("@graph"));
        }
        return null;
    }

    private boolean isJobPosting(JsonNode type) {
        if (type == null) {
            return false;
        }
        if (type.isArray()) {
            for (JsonNode element : type) {
                if ("JobPosting".equalsIgnoreCase(element.asText())) {
                    return true;
                }
            }
            return false;
        }
        return "JobPosting".equalsIgnoreCase(type.asText());
    }

    private ParsedVacancy toVacancy(JsonNode posting) {
        String title = Texts.firstNonBlank(str(posting.get("title")), str(posting.get("name")));
        JsonNode organization = posting.get("hiringOrganization");
        String company = organization != null && organization.isObject()
                ? Texts.clean(str(organization.get("name")))
                : Texts.clean(str(organization));
        String requirements = Texts.firstNonBlank(
                joined(posting.get("qualifications")),
                joined(posting.get("skills")),
                joined(posting.get("experienceRequirements")),
                Texts.html(str(posting.get("description"))));
        return new ParsedVacancy(
                title,
                company,
                salary(posting.get("baseSalary")),
                requirements,
                city(posting.get("jobLocation")),
                Dates.normalize(str(posting.get("datePosted"))));
    }

    private String salary(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isValueNode()) {
            return Texts.clean(node.asText());
        }
        String currency = Texts.clean(str(node.get("currency")));
        JsonNode value = node.get("value");
        String amount;
        String unit = null;
        if (value == null || value.isNull()) {
            amount = null;
        } else if (value.isObject()) {
            String min = number(value.get("minValue"));
            String max = number(value.get("maxValue"));
            String exact = number(value.get("value"));
            unit = Texts.clean(str(value.get("unitText")));
            if (currency == null) {
                currency = Texts.clean(str(value.get("currency")));
            }
            amount = min != null && max != null ? min + " - " + max : Texts.firstNonBlank(exact, min, max);
        } else {
            amount = number(value);
        }
        if (amount == null) {
            return null;
        }
        return amount + (currency != null ? " " + currency : "") + (unit != null ? " / " + unit : "");
    }

    private String number(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.decimalValue().stripTrailingZeros().toPlainString();
        }
        return Texts.clean(str(node));
    }

    private String city(JsonNode location) {
        if (location == null || location.isNull()) {
            return null;
        }
        if (location.isArray()) {
            for (JsonNode element : location) {
                String city = city(element);
                if (city != null) {
                    return city;
                }
            }
            return null;
        }
        if (location.isTextual()) {
            return Texts.clean(location.asText());
        }
        JsonNode address = location.get("address");
        if (address != null) {
            if (address.isTextual()) {
                return Texts.clean(address.asText());
            }
            String locality = Texts.clean(str(address.get("addressLocality")));
            if (locality != null) {
                return locality;
            }
        }
        return Texts.clean(str(location.get("name")));
    }

    private String joined(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isArray()) {
            List<String> parts = new ArrayList<>();
            for (JsonNode element : node) {
                String part = joined(element);
                if (part != null) {
                    parts.add(part);
                }
            }
            return parts.isEmpty() ? null : Texts.clean(String.join("; ", parts));
        }
        if (node.isObject()) {
            return Texts.firstNonBlank(joined(node.get("name")), joined(node.get("description")));
        }
        return Texts.html(node.asText());
    }

    private String str(JsonNode node) {
        return node != null && node.isValueNode() && !node.isNull() ? node.asText() : null;
    }
}

package com.example.vacancyparser.service;

import java.net.URI;
import java.time.Duration;

import com.example.vacancyparser.config.AppProperties;
import com.example.vacancyparser.parser.ParsingException;

import io.netty.channel.ChannelOption;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Component
public class PageFetcher {

    private final WebClient client;
    private final Duration timeout;

    public PageFetcher(AppProperties properties) {
        this.timeout = Duration.ofMillis(properties.fetch().timeoutMs());
        HttpClient httpClient = HttpClient.create()
                .followRedirect(true)
                .compress(true)
                .responseTimeout(timeout)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) properties.fetch().timeoutMs());
        this.client = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(8 * 1024 * 1024))
                .defaultHeader(HttpHeaders.USER_AGENT, properties.fetch().userAgent())
                .defaultHeader(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml")
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "ru,en;q=0.8")
                .build();
    }

    public String fetch(String url) {
        String body = client.get()
                .uri(URI.create(url))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(timeout)
                .block(timeout.plusSeconds(2));
        if (body == null || body.isBlank()) {
            throw new ParsingException("Empty response from " + url);
        }
        return body;
    }
}

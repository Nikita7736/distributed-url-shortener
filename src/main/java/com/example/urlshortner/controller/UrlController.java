package com.example.urlshortner.controller;

import com.example.urlshortner.dto.AnalyticsResponse;
import com.example.urlshortner.dto.CreateShortUrlRequest;
import com.example.urlshortner.dto.ShortUrlResponse;
import com.example.urlshortner.model.ShortUrl;
import com.example.urlshortner.service.ShortUrlService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@RestController
public class UrlController {

    private final ShortUrlService shortUrlService;

    public UrlController(ShortUrlService shortUrlService) {
        this.shortUrlService = shortUrlService;
    }

    //Takes a long URL and returns a shortened version
    @PostMapping("/api/urls")
    public ResponseEntity<ShortUrlResponse> createShortUrl(@RequestBody CreateShortUrlRequest request) {
        Duration ttl = request.ttlSeconds() != null ? Duration.ofSeconds(request.ttlSeconds()) : null;
        ShortUrl shortUrl = shortUrlService.createShortUrl(request.url(), ttl);
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        String shortUrlStr = baseUrl + "/" + shortUrl.getCode();

        Long ttlSeconds = null;
        if (shortUrl.getExpiresAt() != null) {
            ttlSeconds = Duration.between(shortUrl.getCreatedAt(), shortUrl.getExpiresAt()).toSeconds();
        }

        ShortUrlResponse response = new ShortUrlResponse(
                shortUrl.getCode(),
                shortUrlStr,
                shortUrl.getOriginalUrl(),
                ttlSeconds
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/r/{code}")
    public ResponseEntity<Void> redirect(@PathVariable("code") String code) {
        Optional<String> optional = shortUrlService.resolveAndIncrementOriginalUrl(code);
        if (optional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(optional.get()));
        return new ResponseEntity<>(headers, HttpStatus.PERMANENT_REDIRECT);
    }

    @GetMapping("/api/urls/{code}")
    public ResponseEntity<AnalyticsResponse> getAnalytics(@PathVariable("code") String code) {
        Optional<ShortUrl> optional = shortUrlService.getByCode(code);
        if (optional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        ShortUrl shortUrl = optional.get();
        AnalyticsResponse response = new AnalyticsResponse(
                shortUrl.getCode(),
                shortUrl.getOriginalUrl(),
                shortUrl.getClickCount(),
                shortUrl.getCreatedAt().toString(),
                shortUrl.getExpiresAt() != null ? shortUrl.getExpiresAt().toString() : null
        );
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}


package com.example.urlshortner.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitingFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redis;

    private final boolean enabled;
    private final long windowSeconds;
    private final long maxCreatePerWindow;
    private final long maxRedirectPerWindow;
    private final long maxAnalyticsPerWindow;

    private final InMemoryFixedWindowLimiter inMemoryLimiter;

    public RateLimitingFilter(
            StringRedisTemplate redis,
            @Value("${app.rate-limit.enabled:true}") boolean enabled,
            @Value("${app.rate-limit.window-seconds:60}") long windowSeconds,
            @Value("${app.rate-limit.max-create-per-window:20}") long maxCreatePerWindow,
            @Value("${app.rate-limit.max-redirect-per-window:120}") long maxRedirectPerWindow,
            @Value("${app.rate-limit.max-analytics-per-window:60}") long maxAnalyticsPerWindow
    ) {
        this.redis = redis;
        this.enabled = enabled;
        this.windowSeconds = windowSeconds;
        this.maxCreatePerWindow = maxCreatePerWindow;
        this.maxRedirectPerWindow = maxRedirectPerWindow;
        this.maxAnalyticsPerWindow = maxAnalyticsPerWindow;
        this.inMemoryLimiter = new InMemoryFixedWindowLimiter(windowSeconds);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled) return true;
        String path = request.getRequestURI();
        return path.startsWith("/styles.css")
                || path.startsWith("/app.js")
                || path.startsWith("/favicon")
                || path.startsWith("/error");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();
        String bucket = bucketFor(path, method);

        if (bucket == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientId = clientId(request);
        long limit = switch (bucket) {
            case "create" -> maxCreatePerWindow;
            case "redirect" -> maxRedirectPerWindow;
            case "analytics" -> maxAnalyticsPerWindow;
            default -> Long.MAX_VALUE;
        };

        boolean allowed = isAllowedRedisFailOpen(bucket, clientId, limit);
        if (!allowed) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"error\":\"rate limit exceeded\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String bucketFor(String path, String method) {
        if ("/api/urls".equals(path) && "POST".equalsIgnoreCase(method)) return "create";
        if (path.startsWith("/api/urls/") && "GET".equalsIgnoreCase(method)) return "analytics";
        if (path.startsWith("/api/")) return null;
        if (path.equals("/") || path.endsWith(".html") || path.endsWith(".css") || path.endsWith(".js")) return null;
        if (path.length() > 1 && "GET".equalsIgnoreCase(method)) return "redirect";
        return null;
    }

    private boolean isAllowedRedisFailOpen(String bucket, String clientId, long limit) {
        try {
            long window = Instant.now().getEpochSecond() / windowSeconds;
            String key = "rl:" + bucket + ":" + clientId + ":" + window;

            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redis.expire(key, java.time.Duration.ofSeconds(windowSeconds * 2));
            }
            return count == null || count <= limit;
        } catch (Exception e) {
            return inMemoryLimiter.isAllowed(bucket, clientId, limit);
        }
    }

    private String clientId(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String remote = request.getRemoteAddr();
        return remote != null ? remote : "unknown";
    }

    private static class InMemoryFixedWindowLimiter {
        private final long windowSeconds;
        private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();

        private InMemoryFixedWindowLimiter(long windowSeconds) {
            this.windowSeconds = windowSeconds;
        }

        boolean isAllowed(String bucket, String clientId, long limit) {
            long window = Instant.now().getEpochSecond() / windowSeconds;
            String key = bucket + ":" + clientId + ":" + window;
            Counter counter = counters.compute(key, (k, existing) -> {
                if (existing == null) return new Counter(1);
                existing.value++;
                return existing;
            });
            // Opportunistic cleanup of older windows
            if (counters.size() > 50_000) {
                counters.entrySet().removeIf(e -> !e.getKey().endsWith(":" + window));
            }
            return counter.value <= limit;
        }

        private static class Counter {
            private long value;

            private Counter(long value) {
                this.value = value;
            }
        }
    }
}


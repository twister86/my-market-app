package ru.yandex.practicum.mymarket.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

/**
 * Управляет sessionId через куку SESSION вручную.
 * WebSession в WebFlux ненадёжен при редиректах — кука может не дойти до браузера.
 */
@Slf4j
public class SessionUtils {

    public static final String SESSION_COOKIE = "SHOP_SESSION";

    /**
     * Возвращает существующий sessionId из куки или создаёт новый и добавляет куку в ответ.
     */
    public static Mono<String> getOrCreateSessionId(ServerWebExchange exchange) {
        var allCookies = exchange.getRequest().getCookies();
        log.debug("Request cookies: {}", allCookies);

        HttpCookie cookie = allCookies.getFirst(SESSION_COOKIE);

        if (cookie != null && !cookie.getValue().isBlank()) {
            log.debug("Found existing session: {}", cookie.getValue());
            return Mono.just(cookie.getValue());
        }

        String newId = UUID.randomUUID().toString();
        log.debug("Creating new session: {}", newId);
        exchange.getResponse().addCookie(
                ResponseCookie.from(SESSION_COOKIE, newId)
                        .httpOnly(true)
                        .path("/")
                        .maxAge(Duration.ofDays(1))
                        .build()
        );
        return Mono.just(newId);
    }
}

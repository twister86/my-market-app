package ru.yandex.practicum.mymarket.utils;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;

public class SecurityUtils {

    /**
     * Получить username текущего авторизованного пользователя из реактивного SecurityContext.
     * Возвращает пустой Mono для анонимного пользователя.
     */
    public static Mono<String> getCurrentUsername() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(auth -> auth != null && auth.isAuthenticated()
                        && !"anonymousUser".equals(auth.getPrincipal()))
                .map(auth -> auth.getName());
    }
}

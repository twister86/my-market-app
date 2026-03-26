package ru.yandex.practicum.mymarket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentClientService {

    private final WebClient paymentWebClient;

    /** Получить баланс пользователя */
    public Mono<Long> getBalance(String userId) {
        return paymentWebClient.get()
                .uri("/payment/balance/{userId}", userId)
                .retrieve()
                .bodyToMono(Map.class)
                .map(body -> ((Number) body.get("balance")).longValue())
                .doOnNext(b -> log.debug("Balance userId={}: {}", userId, b))
                .onErrorResume(e -> {
                    log.warn("Failed to get balance userId={}: {}", userId, e.getMessage());
                    // При недоступности сервиса — возвращаем 0 (кнопка будет недоступна)
                    return Mono.just(0L);
                });
    }

    /**
     * Проверяет, хватает ли баланса для оплаты суммы.
     * Используется для управления доступностью кнопки оформления заказа.
     */
    public Mono<Boolean> hasEnoughBalance(String userId, long amount) {
        return getBalance(userId)
                .map(balance -> balance >= amount)
                .doOnNext(ok -> log.debug("Balance check userId={} amount={} ok={}", userId, amount, ok));
    }

    /**
     * Осуществить платёж (списание баланса).
     * @return true если платёж прошёл успешно
     */
    public Mono<Boolean> pay(String userId, long amount, long orderId) {
        Map<String, Object> request = Map.of(
                "userId",  userId,
                "amount",  amount,
                "orderId", orderId
        );
        return paymentWebClient.post()
                .uri("/payment/pay")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .map(body -> Boolean.TRUE.equals(body.get("success")))
                .doOnNext(ok -> log.debug("Payment userId={} amount={} success={}", userId, amount, ok))
                .onErrorResume(WebClientResponseException.BadRequest.class, e -> {
                    log.warn("Payment rejected userId={} amount={}: {}", userId, amount, e.getMessage());
                    return Mono.just(false);
                })
                .onErrorResume(e -> {
                    log.warn("Payment error userId={} amount={}: {}", userId, amount, e.getMessage());
                    return Mono.just(false);
                });
    }
}

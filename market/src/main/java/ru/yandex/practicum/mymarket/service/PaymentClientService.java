package ru.yandex.practicum.mymarket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.market.payment.api.PaymentApi;
import ru.yandex.practicum.market.payment.model.BalanceResponse;
import ru.yandex.practicum.market.payment.model.PaymentRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentClientService {

    // Сгенерированный WebClient-клиент из OpenAPI схемы
    private final PaymentApi paymentApi;

    public Mono<Long> getBalance(String userId) {
        return paymentApi.getBalance(userId)
                .map(BalanceResponse::getBalance)
                .doOnNext(b -> log.debug("Balance userId={}: {}", userId, b))
                .onErrorResume(e -> {
                    log.warn("Failed to get balance userId={}: {}", userId, e.getMessage());
                    return Mono.just(0L);
                });
    }

    public Mono<Boolean> hasEnoughBalance(String userId, long amount) {
        return getBalance(userId)
                .map(balance -> balance >= amount);
    }

    public Mono<Boolean> pay(String userId, long amount, long orderId) {
        PaymentRequest request = new PaymentRequest();
        request.setUserId(userId);
        request.setAmount(amount);
        request.setOrderId(orderId);

        return paymentApi.pay(request)
                .map(response -> response.getSuccess())
                .doOnNext(ok -> log.debug("Payment userId={} amount={} success={}", userId, amount, ok))
                .onErrorResume(e -> {
                    log.warn("Payment error userId={} amount={}: {}", userId, amount, e.getMessage());
                    return Mono.just(false);
                });
    }
}

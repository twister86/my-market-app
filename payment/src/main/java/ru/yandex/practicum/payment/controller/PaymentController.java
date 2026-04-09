package ru.yandex.practicum.payment.controller;

import org.springframework.web.server.ServerWebExchange;
import ru.yandex.practicum.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.api.PaymentApi;
import ru.yandex.practicum.payment.model.BalanceResponse;
import ru.yandex.practicum.payment.model.PaymentRequest;
import ru.yandex.practicum.payment.model.PaymentResponse;


@Slf4j
@RestController
@RequiredArgsConstructor
public class PaymentController implements PaymentApi {

    private final PaymentService paymentService;

    @Override
    public Mono<ResponseEntity<BalanceResponse>> getBalance(
            String userId, ServerWebExchange exchange) {

        return paymentService.getBalance(userId)
                .map(balance -> {
                    BalanceResponse body = new BalanceResponse();
                    body.setUserId(userId);
                    body.setBalance(balance);
                    return ResponseEntity.ok(body);
                })
                .onErrorResume(IllegalArgumentException.class, e ->
                        Mono.just(ResponseEntity.<BalanceResponse>notFound().build())
                );
    }

    @Override
    public Mono<ResponseEntity<PaymentResponse>> pay(
            Mono<PaymentRequest> paymentRequestMono, ServerWebExchange exchange) {

        return paymentRequestMono.flatMap(req ->
                paymentService.pay(req.getUserId(), req.getAmount())
                        .map(remaining -> {
                            PaymentResponse body = new PaymentResponse();
                            body.setSuccess(true);
                            body.setRemainingBalance(remaining);
                            body.setOrderId(req.getOrderId());
                            return ResponseEntity.ok(body);
                        })
                        .onErrorResume(IllegalArgumentException.class, e ->
                                Mono.just(ResponseEntity.<PaymentResponse>notFound().build())
                        )
                        .onErrorResume(IllegalStateException.class, e -> {
                            PaymentResponse body = new PaymentResponse();
                            body.setSuccess(false);
                            body.setRemainingBalance(0L);
                            body.setOrderId(req.getOrderId());
                            return Mono.just(ResponseEntity.badRequest().body(body));
                        })
        );
    }
}

package ru.yandex.practicum.payment.controller;

import ru.yandex.practicum.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/payment/balance/{userId}")
    public Mono<ResponseEntity<Map<String, Object>>> getBalance(@PathVariable String userId) {
        return paymentService.getBalance(userId)
                .map(balance -> {
                    Map<String, Object> body = new HashMap<>();
                    body.put("userId", userId);
                    body.put("balance", balance);
                    return ResponseEntity.<Map<String, Object>>ok(body);
                })
                .onErrorResume(IllegalArgumentException.class, e ->
                        Mono.just(ResponseEntity.<Map<String, Object>>notFound().build())
                );
    }

    @PostMapping("/payment/pay")
    public Mono<ResponseEntity<Map<String, Object>>> pay(
            @RequestBody Map<String, Object> request) {

        String userId = (String) request.get("userId");
        long amount   = ((Number) request.get("amount")).longValue();
        long orderId  = ((Number) request.get("orderId")).longValue();

        return paymentService.pay(userId, amount)
                .map(remaining -> {
                    Map<String, Object> body = new HashMap<>();
                    body.put("success", true);
                    body.put("remainingBalance", remaining);
                    body.put("orderId", orderId);
                    return ResponseEntity.<Map<String, Object>>ok(body);
                })
                .onErrorResume(IllegalArgumentException.class, e ->
                        Mono.just(ResponseEntity.<Map<String, Object>>notFound().build())
                )
                .onErrorResume(IllegalStateException.class, e -> {
                    Map<String, Object> body = new HashMap<>();
                    body.put("success", false);
                    body.put("remainingBalance", 0L);
                    body.put("orderId", orderId);
                    body.put("message", e.getMessage());
                    return Mono.just(ResponseEntity.<Map<String, Object>>badRequest().body(body));
                });
    }
}

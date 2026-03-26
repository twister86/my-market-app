package ru.yandex.practicum.payment;

import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.controller.PaymentController;
import ru.yandex.practicum.payment.model.PaymentAccount;
import ru.yandex.practicum.payment.model.PaymentResponse;
import ru.yandex.practicum.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(PaymentController.class)
@Import(PaymentService.class)
class PaymentControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        PaymentAccount.ACCOUNTS.put("default", new PaymentAccount("default", 100_000L));
        PaymentAccount.ACCOUNTS.put("poor",    new PaymentAccount("poor",    500L));
    }

    @Test
    void getBalance_existingUser_returns200() {
        when(paymentService.getBalance(anyString()))
                .thenReturn(Mono.just(100000L));

        when(paymentService.pay(anyString(), anyLong()))
                .thenReturn(Mono.just(90000L));
        webTestClient.get().uri("/payment/balance/default")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.userId").isEqualTo("default")
                .jsonPath("$.balance").isEqualTo(100_000);
    }

    @Test
    void getBalance_unknownUser_returns404() {
        when(paymentService.getBalance(anyString()))
                .thenReturn(Mono.error(new IllegalArgumentException("Insufficient funds")));
        webTestClient.get().uri("/payment/balance/unknown")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void pay_sufficientFunds_returns200WithSuccess() {
        when(paymentService.getBalance(anyString()))
                .thenReturn(Mono.just(10000L));

        when(paymentService.pay(anyString(), anyLong()))
                .thenReturn(Mono.just(90000L));
        webTestClient.post().uri("/payment/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("userId", "default", "amount", 10_000, "orderId", 1))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.remainingBalance").isEqualTo(90_000)
                .jsonPath("$.orderId").isEqualTo(1);
    }

    @Test
    void pay_insufficientFunds_returns400() {
        when(paymentService.pay(anyString(), anyLong()))
                .thenReturn(Mono.error(new IllegalStateException("Insufficient funds")));
        webTestClient.post().uri("/payment/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("userId", "poor", "amount", 10_000, "orderId", 2))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false);
    }

    @Test
    void pay_unknownUser_returns404() {
        when(paymentService.pay(any(), anyLong()))
                .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)));
        webTestClient.post().uri("/payment/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("userId", "ghost", "amount", 100, "orderId", 3))
                .exchange()
                .expectStatus().isNotFound();
    }
}

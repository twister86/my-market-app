package ru.yandex.practicum.payment;

import ru.yandex.practicum.payment.controller.PaymentController;
import ru.yandex.practicum.payment.model.PaymentAccount;
import ru.yandex.practicum.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.util.Map;

@WebFluxTest(PaymentController.class)
@Import(PaymentService.class)
class PaymentControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        PaymentAccount.ACCOUNTS.put("default", new PaymentAccount("default", 100_000L));
        PaymentAccount.ACCOUNTS.put("poor",    new PaymentAccount("poor",    500L));
    }

    @Test
    void getBalance_existingUser_returns200() {
        webTestClient.get().uri("/payment/balance/default")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.userId").isEqualTo("default")
                .jsonPath("$.balance").isEqualTo(100_000);
    }

    @Test
    void getBalance_unknownUser_returns404() {
        webTestClient.get().uri("/payment/balance/unknown")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void pay_sufficientFunds_returns200WithSuccess() {
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
        webTestClient.post().uri("/payment/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("userId", "ghost", "amount", 100, "orderId", 3))
                .exchange()
                .expectStatus().isNotFound();
    }
}

package ru.yandex.practicum.payment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.config.PaymentSecurityConfig;
import ru.yandex.practicum.payment.controller.PaymentController;
import ru.yandex.practicum.payment.model.PaymentRequest;
import ru.yandex.practicum.payment.service.PaymentService;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@WebFluxTest(PaymentController.class)
@Import(PaymentSecurityConfig.class)
class PaymentControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void getBalance_returns200() {
        when(paymentService.getBalance("user1")).thenReturn(Mono.just(50_000L));

        webTestClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("SCOPE_payment.read")))
                .get().uri("/payment/balance/user1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.userId").isEqualTo("user1")
                .jsonPath("$.balance").isEqualTo(50_000);
    }

    @Test
    void pay_success_returns200() {
        when(paymentService.pay(eq("user1"), eq(10_000L)))
                .thenReturn(Mono.just(40_000L));

        PaymentRequest req = new PaymentRequest();
        req.setUserId("user1");
        req.setAmount(10_000L);
        req.setOrderId(1L);

        webTestClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("SCOPE_payment.write")))
                .post().uri("/payment/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.remainingBalance").isEqualTo(40_000)
                .jsonPath("$.orderId").isEqualTo(1);
    }

    @Test
    void pay_insufficientFunds_returns400() {
        when(paymentService.pay(eq("poor"), anyLong()))
                .thenReturn(Mono.error(new IllegalStateException("Недостаточно средств")));

        PaymentRequest req = new PaymentRequest();
        req.setUserId("poor");
        req.setAmount(99_999L);
        req.setOrderId(2L);

        webTestClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("SCOPE_payment.write")))
                .post().uri("/payment/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false);
    }

    @Test
    void getBalance_withoutToken_returns401() {
        webTestClient.get().uri("/payment/balance/user1")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void pay_withoutToken_returns401() {
        webTestClient.post().uri("/payment/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"userId\":\"u\",\"amount\":100,\"orderId\":1}")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getBalance_wrongScope_returns403() {
        webTestClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("SCOPE_payment.write")))
                .get().uri("/payment/balance/user1")
                .exchange()
                .expectStatus().isForbidden();
    }
}
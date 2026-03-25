package ru.yandex.practicum.payment;

import ru.yandex.practicum.payment.model.PaymentAccount;
import ru.yandex.practicum.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class PaymentServiceTest {

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService();
        // Сбрасываем балансы перед каждым тестом
        PaymentAccount.ACCOUNTS.put("default", new PaymentAccount("default", 100_000L));
        PaymentAccount.ACCOUNTS.put("user1",   new PaymentAccount("user1",   50_000L));
        PaymentAccount.ACCOUNTS.put("poor",    new PaymentAccount("poor",    100L));
    }

    @Test
    void getBalance_existingUser_returnsBalance() {
        StepVerifier.create(paymentService.getBalance("user1"))
                .expectNext(50_000L)
                .verifyComplete();
    }

    @Test
    void getBalance_unknownUser_returnsError() {
        StepVerifier.create(paymentService.getBalance("unknown"))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException
                        && e.getMessage().contains("не найден"))
                .verify();
    }

    @Test
    void pay_sufficientFunds_deductsAndReturnsRemaining() {
        StepVerifier.create(paymentService.pay("user1", 10_000L))
                .expectNext(40_000L)
                .verifyComplete();
    }

    @Test
    void pay_insufficientFunds_returnsError() {
        StepVerifier.create(paymentService.pay("poor", 5_000L))
                .expectErrorMatches(e -> e instanceof IllegalStateException
                        && e.getMessage().contains("Недостаточно средств"))
                .verify();
    }

    @Test
    void pay_unknownUser_returnsError() {
        StepVerifier.create(paymentService.pay("ghost", 100L))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException)
                .verify();
    }

    @Test
    void pay_exactAmount_leavesZeroBalance() {
        StepVerifier.create(paymentService.pay("poor", 100L))
                .expectNext(0L)
                .verifyComplete();
    }

    @Test
    void pay_multiplePayments_accumulateCorrectly() {
        StepVerifier.create(
                paymentService.pay("default", 30_000L)
                        .then(paymentService.pay("default", 20_000L))
        )
                .expectNext(50_000L)
                .verifyComplete();
    }
}

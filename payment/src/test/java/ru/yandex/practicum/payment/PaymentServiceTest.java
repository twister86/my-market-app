package ru.yandex.practicum.payment;

import ru.yandex.practicum.payment.model.PaymentAccount;
import ru.yandex.practicum.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentServiceTest {

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService();
        PaymentAccount.ACCOUNTS.clear();
        // Фиксируем балансы для детерминированных тестов
        PaymentAccount.ACCOUNTS.put("rich",  new PaymentAccount("rich",  100_000L));
        PaymentAccount.ACCOUNTS.put("poor",  new PaymentAccount("poor",  500L));
        PaymentAccount.ACCOUNTS.put("exact", new PaymentAccount("exact", 1_000L));
    }

    @Test
    void getBalance_existingUser_returnsBalance() {
        StepVerifier.create(paymentService.getBalance("rich"))
                .expectNext(100_000L)
                .verifyComplete();
    }

    @Test
    void getBalance_newUser_createsAccountWithRandomBalance() {
        StepVerifier.create(paymentService.getBalance("brand-new-user"))
                .assertNext(balance -> {
                    assertThat(balance).isBetween(1_000L, 50_000L);
                    // Повторный запрос — тот же баланс
                    assertThat(PaymentAccount.ACCOUNTS.get("brand-new-user").getBalance())
                            .isEqualTo(balance);
                })
                .verifyComplete();
    }

    @Test
    void pay_sufficientFunds_deductsAndReturnsRemaining() {
        StepVerifier.create(paymentService.pay("rich", 10_000L))
                .expectNext(90_000L)
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
    void pay_exactAmount_leavesZeroBalance() {
        StepVerifier.create(paymentService.pay("exact", 1_000L))
                .expectNext(0L)
                .verifyComplete();
    }

    @Test
    void pay_multiplePayments_accumulateCorrectly() {
        StepVerifier.create(
                        paymentService.pay("rich", 30_000L)
                                .then(paymentService.pay("rich", 20_000L))
                )
                .expectNext(50_000L)
                .verifyComplete();
    }

    @Test
    void pay_afterInsufficientFunds_balanceUnchanged() {
        StepVerifier.create(paymentService.pay("poor", 1_000L))
                .expectError(IllegalStateException.class)
                .verify();

        // Баланс не изменился
        assertThat(PaymentAccount.ACCOUNTS.get("poor").getBalance()).isEqualTo(500L);
    }
}

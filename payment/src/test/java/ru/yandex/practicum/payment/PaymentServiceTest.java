package ru.yandex.practicum.payment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.core.ReactiveInsertOperation;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.payment.model.PaymentAccount;
import ru.yandex.practicum.payment.repository.PaymentAccountRepository;
import ru.yandex.practicum.payment.service.PaymentService;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentAccountRepository accountRepository;
    @Mock private R2dbcEntityTemplate template;
    @Mock private ReactiveRedisTemplate<String, Long> redisTemplate;
    @Mock private ReactiveValueOperations<String, Long> valueOps;
    @Mock private ReactiveInsertOperation.ReactiveInsert<PaymentAccount> insertOp;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        paymentService = new PaymentService(accountRepository, template, redisTemplate);
    }

    @Test
    void getBalance_cacheHit_returnsFromRedis() {
        when(valueOps.get("balance:user1")).thenReturn(Mono.just(50_000L));

        StepVerifier.create(paymentService.getBalance("user1"))
                .expectNext(50_000L)
                .verifyComplete();

        verify(accountRepository, never()).findByUserId(anyString());
    }

    @Test
    void getBalance_cacheMiss_existingAccount_loadsFromDb() {
        when(valueOps.get("balance:user1")).thenReturn(Mono.empty());
        when(accountRepository.findByUserId("user1"))
                .thenReturn(Mono.just(new PaymentAccount("user1", 30_000L)));
        when(valueOps.set(eq("balance:user1"), any(Long.class), any(Duration.class)))
                .thenReturn(Mono.just(true));

        StepVerifier.create(paymentService.getBalance("user1"))
                .expectNext(30_000L)
                .verifyComplete();
    }

    @Test
    void getBalance_cacheMiss_newUser_createsAccount() {
        when(valueOps.get("balance:new")).thenReturn(Mono.empty());
        when(accountRepository.findByUserId("new")).thenReturn(Mono.empty());
        // template.insert нужен только в этом тесте — настраиваем локально
        when(template.insert(PaymentAccount.class)).thenReturn(insertOp);
        when(insertOp.using(any(PaymentAccount.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(valueOps.set(eq("balance:new"), any(Long.class), any(Duration.class)))
                .thenReturn(Mono.just(true));

        StepVerifier.create(paymentService.getBalance("new"))
                .assertNext(b -> assertThat(b).isGreaterThan(0))
                .verifyComplete();
    }

    @Test
    void pay_sufficientFunds_deductsAndEvictsCache() {
        when(accountRepository.findByUserId("user1"))
                .thenReturn(Mono.just(new PaymentAccount("user1", 10_000L)));
        when(accountRepository.save(any()))
                .thenReturn(Mono.just(new PaymentAccount("user1", 7_000L)));
        when(valueOps.delete("balance:user1")).thenReturn(Mono.just(true));

        StepVerifier.create(paymentService.pay("user1", 3_000L))
                .expectNext(7_000L)
                .verifyComplete();

        verify(valueOps).delete("balance:user1");
    }

    @Test
    void pay_insufficientFunds_returnsError() {
        when(accountRepository.findByUserId("poor"))
                .thenReturn(Mono.just(new PaymentAccount("poor", 500L)));

        StepVerifier.create(paymentService.pay("poor", 5_000L))
                .expectErrorMatches(e -> e instanceof IllegalStateException
                        && e.getMessage().contains("Недостаточно средств"))
                .verify();

        verify(accountRepository, never()).save(any());
    }
}

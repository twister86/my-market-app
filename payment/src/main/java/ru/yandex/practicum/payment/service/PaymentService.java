package ru.yandex.practicum.payment.service;

import lombok.RequiredArgsConstructor;
import ru.yandex.practicum.payment.model.PaymentAccount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.repository.PaymentAccountRepository;

import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

import java.time.Duration;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String CACHE_PREFIX = "balance:";
    private static final Duration CACHE_TTL  = Duration.ofMinutes(5);
    private static final Random   RANDOM      = new Random();

    private final PaymentAccountRepository accountRepository;
    private final R2dbcEntityTemplate      template;
    private final ReactiveRedisTemplate<String, Long> redisTemplate;

    /**
     * Получить баланс.
     * Сначала проверяет Redis-кеш, при промахе — H2, при отсутствии — создаёт счёт.
     */
    public Mono<Long> getBalance(String userId) {
        String key = CACHE_PREFIX + userId;
        return redisTemplate.opsForValue().get(key)
                .doOnNext(b -> log.debug("Balance cache HIT userId={} balance={}", userId, b))
                .switchIfEmpty(Mono.defer(() ->
                        accountRepository.findByUserId(userId)
                                .switchIfEmpty(Mono.defer(() -> createAccount(userId)))
                                .flatMap(account -> redisTemplate.opsForValue()
                                        .set(key, account.getBalance(), CACHE_TTL)
                                        .thenReturn(account.getBalance()))
                                .doOnNext(b -> log.debug("Balance cache MISS userId={} balance={}", userId, b))
                ));
    }

    /**
     * Списать сумму с баланса.
     * Инвалидирует кеш после успешного списания.
     */
    public Mono<Long> pay(String userId, long amount) {
        return accountRepository.findByUserId(userId)
                .switchIfEmpty(Mono.defer(() -> createAccount(userId)))
                .flatMap(account -> {
                    if (account.getBalance() < amount) {
                        return Mono.error(new IllegalStateException(
                                "Недостаточно средств: баланс " + account.getBalance() +
                                        " руб., требуется " + amount + " руб."));
                    }
                    account.setBalance(account.getBalance() - amount);
                    return accountRepository.save(account)
                            .flatMap(saved -> evictCache(userId).thenReturn(saved.getBalance()))
                            .doOnNext(remaining -> log.info(
                                    "Payment OK: userId={} amount={} remaining={}",
                                    userId, amount, remaining));
                });
    }

    private Mono<PaymentAccount> createAccount(String userId) {
        long balance = 1000L + RANDOM.nextInt(49_001);
        log.info("Creating new account: userId={} balance={}", userId, balance);
        return template.insert(PaymentAccount.class)
                .using(new PaymentAccount(userId, balance));
    }

    private Mono<Void> evictCache(String userId) {
        return redisTemplate.opsForValue()
                .delete(CACHE_PREFIX + userId)
                .then();
    }
}

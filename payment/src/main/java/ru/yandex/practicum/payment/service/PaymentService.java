package ru.yandex.practicum.payment.service;

import ru.yandex.practicum.payment.model.PaymentAccount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class PaymentService {

    /** Получить баланс пользователя */
    public Mono<Long> getBalance(String userId) {
        PaymentAccount account = PaymentAccount.ACCOUNTS.get(userId);
        if (account == null) {
            return Mono.error(new IllegalArgumentException("Пользователь не найден: " + userId));
        }
        return Mono.just(account.getBalance());
    }

    /**
     * Списать сумму с баланса.
     * @return остаток баланса после списания
     */
    public Mono<Long> pay(String userId, long amount) {
        PaymentAccount account = PaymentAccount.ACCOUNTS.get(userId);
        if (account == null) {
            return Mono.error(new IllegalArgumentException("Пользователь не найден: " + userId));
        }
        if (account.getBalance() < amount) {
            return Mono.error(new IllegalStateException(
                    "Недостаточно средств: баланс " + account.getBalance() + ", требуется " + amount));
        }
        account.setBalance(account.getBalance() - amount);
        log.info("Payment: userId={} amount={} remaining={}", userId, amount, account.getBalance());
        return Mono.just(account.getBalance());
    }
}

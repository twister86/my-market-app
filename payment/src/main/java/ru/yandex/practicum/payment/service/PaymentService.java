package ru.yandex.practicum.payment.service;

import ru.yandex.practicum.payment.model.PaymentAccount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class PaymentService {

    /**
     * Получить баланс пользователя.
     * Если счёт не существует — создаётся автоматически со случайным балансом.
     */
    public Mono<Long> getBalance(String userId) {
        PaymentAccount account = PaymentAccount.getOrCreate(userId);
        log.debug("getBalance userId={} balance={}", userId, account.getBalance());
        return Mono.just(account.getBalance());
    }

    /**
     * Списать сумму с баланса.
     * @return остаток баланса после списания
     * @throws IllegalStateException если средств недостаточно
     */
    public Mono<Long> pay(String userId, long amount) {
        PaymentAccount account = PaymentAccount.getOrCreate(userId);
        if (account.getBalance() < amount) {
            return Mono.error(new IllegalStateException(
                    "Недостаточно средств: баланс " + account.getBalance() + " руб., требуется " + amount + " руб."));
        }
        account.setBalance(account.getBalance() - amount);
        log.info("Payment OK: userId={} amount={} remaining={}", userId, amount, account.getBalance());
        return Mono.just(account.getBalance());
    }
}

package ru.yandex.practicum.payment.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.model.PaymentAccount;

public interface PaymentAccountRepository
        extends ReactiveCrudRepository<PaymentAccount, String> {

    @Query("SELECT * FROM payment_accounts WHERE user_id = :userId")
    Mono<PaymentAccount> findByUserId(String userId);
}

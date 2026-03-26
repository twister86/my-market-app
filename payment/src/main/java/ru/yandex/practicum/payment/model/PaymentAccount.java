package ru.yandex.practicum.payment.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Data
@AllArgsConstructor
public class PaymentAccount {
    private String userId;
    private long balance;

    private static final Random RANDOM = new Random();
    public static final Map<String, PaymentAccount> ACCOUNTS = new ConcurrentHashMap<>();

    /**
     * Получить или создать счёт для userId.
     * Баланс нового счёта — случайное значение от 10000 до 50000 руб.
     */
    public static PaymentAccount getOrCreate(String userId) {
        return ACCOUNTS.computeIfAbsent(userId,
                id -> new PaymentAccount(id, 10000L + RANDOM.nextInt(49_001)));
    }
}

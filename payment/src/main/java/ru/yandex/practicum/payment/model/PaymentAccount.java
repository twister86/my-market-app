package ru.yandex.practicum.payment.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Data
@AllArgsConstructor
public class PaymentAccount {
    private String userId;
    private long balance;

    /** In-memory хранилище счетов (в реальном проекте — БД) */
    public static final Map<String, PaymentAccount> ACCOUNTS = new ConcurrentHashMap<>();

    static {
        // Тестовые счета
        ACCOUNTS.put("default", new PaymentAccount("default", 100_000L));
        ACCOUNTS.put("user1",   new PaymentAccount("user1",   50_000L));
        ACCOUNTS.put("user2",   new PaymentAccount("user2",   10_000L));
    }
}

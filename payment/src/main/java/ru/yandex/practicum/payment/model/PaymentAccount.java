package ru.yandex.practicum.payment.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("payment_accounts")
public class PaymentAccount {

    @Id
    private String userId;

    private long balance;
}
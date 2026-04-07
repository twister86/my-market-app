package ru.yandex.practicum.mymarket.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
public class User {

    @Id
    private Long id;

    private String username;

    /** Хранится в зашифрованном виде (BCrypt) */
    private String password;

    private String role; // ROLE_USER, ROLE_ADMIN
}

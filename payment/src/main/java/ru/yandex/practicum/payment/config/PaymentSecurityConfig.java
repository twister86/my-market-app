package ru.yandex.practicum.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class PaymentSecurityConfig {

    /**
     * Все эндпоинты /payment/** требуют валидного JWT-токена от auth-server.
     * Токен должен содержать scope payment.read или payment.write.
     */
    @Bean
    public SecurityWebFilterChain paymentSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/payment/balance/**").hasAuthority("SCOPE_payment.read")
                        .pathMatchers("/payment/pay").hasAuthority("SCOPE_payment.write")
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> {}) // валидация JWT по JWKS от auth-server
                )
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .build();
    }
}

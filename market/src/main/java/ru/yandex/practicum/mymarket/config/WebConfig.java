package ru.yandex.practicum.mymarket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.WebFilter;

@Configuration
public class WebConfig {

    /**
     * Регистрирует фильтр сессии явно как бин.
     * Вызов session.save() гарантирует что кука SESSION отправляется
     * браузеру при первом запросе и не меняется на последующих.
     */
    @Bean
    public WebFilter sessionSaveFilter() {
        return (exchange, chain) -> exchange.getSession()
                .flatMap(session -> session.save()
                        .then(chain.filter(exchange)));
    }
}


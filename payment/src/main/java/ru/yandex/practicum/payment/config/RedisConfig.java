package ru.yandex.practicum.payment.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;


@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * ReactiveRedisTemplate для кеширования балансов (ключ=userId, значение=balance)
     */
    @Bean
    public ReactiveRedisTemplate<String, Long> balanceRedisTemplate(
            ReactiveRedisConnectionFactory factory) {

        RedisSerializationContext<String, Long> context =
                RedisSerializationContext.<String, Long>newSerializationContext(
                                RedisSerializer.string())
                        .value(new GenericToStringSerializer<>(Long.class))
                        .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }
}

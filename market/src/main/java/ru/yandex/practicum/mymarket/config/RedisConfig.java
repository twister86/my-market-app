package ru.yandex.practicum.mymarket.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import ru.yandex.practicum.mymarket.entity.Item;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public ReactiveRedisTemplate<String, Item> itemRedisTemplate(
            ReactiveRedisConnectionFactory factory) {

        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<Item> valueSerializer =
                new Jackson2JsonRedisSerializer<>(Item.class);

        RedisSerializationContext<String, Item> context =
                RedisSerializationContext.<String, Item>newSerializationContext(keySerializer)
                        .value(valueSerializer)
                        .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }
}

package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import ru.yandex.practicum.mymarket.entity.Order;

public interface OrderRepository extends ReactiveCrudRepository<Order, Long> {

    @Query("SELECT * FROM orders WHERE session_id = :sessionId ORDER BY id DESC")
    Flux<Order> findBySessionId(String sessionId);
}

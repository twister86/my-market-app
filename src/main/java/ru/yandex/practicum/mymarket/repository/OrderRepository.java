package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import ru.yandex.practicum.mymarket.entity.Order;

public interface OrderRepository extends ReactiveCrudRepository<Order, Long> {
}

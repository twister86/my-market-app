package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import ru.yandex.practicum.mymarket.entity.Item;

public interface ItemRepository extends ReactiveCrudRepository<Item, Long> {

    @Query("SELECT * FROM items WHERE LOWER(title) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(description) LIKE LOWER(CONCAT('%', :search, '%'))")
    Flux<Item> findByTitleOrDescriptionContainingIgnoreCase(String search);
}

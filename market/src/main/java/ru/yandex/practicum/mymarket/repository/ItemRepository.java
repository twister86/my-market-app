package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.entity.Item;

public interface ItemRepository extends ReactiveCrudRepository<Item, Long> {

    // Поиск по названию/описанию (для фильтрации)
    @Query("SELECT * FROM items WHERE LOWER(title) LIKE LOWER(CONCAT('%', :search, '%'))" +
            " OR LOWER(description) LIKE LOWER(CONCAT('%', :search, '%'))")
    Flux<Item> findByTitleOrDescriptionContainingIgnoreCase(String search);

    // ===== Пагинация на уровне БД =====

    @Query("SELECT * FROM items ORDER BY id LIMIT :limit OFFSET :offset")
    Flux<Item> findAllPaged(int limit, int offset);

    @Query("SELECT * FROM items ORDER BY title LIMIT :limit OFFSET :offset")
    Flux<Item> findAllPagedOrderByTitle(int limit, int offset);

    @Query("SELECT * FROM items ORDER BY price LIMIT :limit OFFSET :offset")
    Flux<Item> findAllPagedOrderByPrice(int limit, int offset);

    @Query("SELECT * FROM items WHERE LOWER(title) LIKE LOWER(CONCAT('%', :search, '%'))" +
            " OR LOWER(description) LIKE LOWER(CONCAT('%', :search, '%'))" +
            " ORDER BY id LIMIT :limit OFFSET :offset")
    Flux<Item> findBySearchPaged(String search, int limit, int offset);

    @Query("SELECT * FROM items WHERE LOWER(title) LIKE LOWER(CONCAT('%', :search, '%'))" +
            " OR LOWER(description) LIKE LOWER(CONCAT('%', :search, '%'))" +
            " ORDER BY title LIMIT :limit OFFSET :offset")
    Flux<Item> findBySearchPagedOrderByTitle(String search, int limit, int offset);

    @Query("SELECT * FROM items WHERE LOWER(title) LIKE LOWER(CONCAT('%', :search, '%'))" +
            " OR LOWER(description) LIKE LOWER(CONCAT('%', :search, '%'))" +
            " ORDER BY price LIMIT :limit OFFSET :offset")
    Flux<Item> findBySearchPagedOrderByPrice(String search, int limit, int offset);

    @Query("SELECT COUNT(*) FROM items WHERE LOWER(title) LIKE LOWER(CONCAT('%', :search, '%'))" +
            " OR LOWER(description) LIKE LOWER(CONCAT('%', :search, '%'))")
    Mono<Long> countBySearch(String search);
}

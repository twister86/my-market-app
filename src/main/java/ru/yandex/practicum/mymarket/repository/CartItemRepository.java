package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.entity.CartItem;

public interface CartItemRepository extends ReactiveCrudRepository<CartItem, Long> {
    @Query("SELECT * FROM cart_items WHERE item_id = :itemId AND session_id = :sessionId")
    Mono<CartItem> findByItemIdAndSessionId(Long itemId, String sessionId);

    @Query("SELECT * FROM cart_items WHERE session_id = :sessionId")
    Flux<CartItem> findBySessionId(String sessionId);

    @Query("DELETE FROM cart_items WHERE session_id = :sessionId")
    Mono<Void> deleteBySessionId(String sessionId);

    @Query("DELETE FROM cart_items WHERE item_id = :itemId AND session_id = :sessionId")
    Mono<Void> deleteByItemIdAndSessionId(Long itemId, String sessionId);
}

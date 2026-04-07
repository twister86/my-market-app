package ru.yandex.practicum.mymarket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.entity.CartItem;
import ru.yandex.practicum.mymarket.entity.Item;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;
    private final R2dbcEntityTemplate template;

    public Flux<Item> getCartItems(String sessionId) {
        return cartItemRepository.findBySessionId(sessionId)
                .flatMap(ci -> itemRepository.findById(ci.getItemId())
                        .map(item -> {
                            item.setCount(ci.getCount());
                            return item;
                        }));
    }

    public Mono<Long> getTotal(String sessionId) {
        return getCartItems(sessionId)
                .map(item -> item.getPrice() * item.getCount())
                .reduce(0L, Long::sum);
    }

    public Mono<Void> updateCart(Long itemId, String action, String sessionId) {
        return cartItemRepository.findByItemIdAndSessionId(itemId, sessionId)
                .flatMap(existing -> {
                    int newCount = switch (action) {
                        case "PLUS"   -> existing.getCount() + 1;
                        case "MINUS"  -> Math.max(0, existing.getCount() - 1);
                        case "DELETE" -> 0;
                        default       -> existing.getCount();
                    };
                    if (newCount <= 0) {
                        return cartItemRepository.deleteByItemIdAndSessionId(itemId, sessionId)
                                .thenReturn(Boolean.TRUE);
                    }
                    existing.setCount(newCount);
                    return cartItemRepository.save(existing)
                            .thenReturn(Boolean.TRUE);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    if (!"PLUS".equals(action)) {
                        return Mono.just(Boolean.FALSE);
                    }
                    CartItem newItem = CartItem.builder()
                            .itemId(itemId)
                            .sessionId(sessionId)
                            .count(1)
                            .build();
                    return template.insert(CartItem.class)
                            .using(newItem)
                            .thenReturn(Boolean.FALSE);
                }))
                .then();
    }

    public Mono<Integer> getCount(Long itemId, String sessionId) {
        return cartItemRepository.findByItemIdAndSessionId(itemId, sessionId)
                .map(CartItem::getCount)
                .defaultIfEmpty(0);
    }

    public Mono<Void> clear(String sessionId) {
        return cartItemRepository.deleteBySessionId(sessionId);
    }

    public Flux<CartItem> getAllCartItems(String sessionId) {
        return cartItemRepository.findBySessionId(sessionId);
    }
}
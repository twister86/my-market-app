package ru.yandex.practicum.mymarket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.core.DatabaseClient;
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

    public Flux<Item> getCartItems() {
        return cartItemRepository.findAll()
                .flatMap(ci -> itemRepository.findById(ci.getId())
                        .map(item -> {
                            item.setCount(ci.getCount());
                            return item;
                        }));
    }

    public Mono<Long> getTotal() {
        return getCartItems()
                .map(item -> item.getPrice() * item.getCount())
                .reduce(0L, Long::sum);
    }

    public Mono<Void> updateCart(Long itemId, String action) {
        return cartItemRepository.findById(itemId)
                .flatMap(existing -> {
                    int newCount = switch (action) {
                        case "PLUS"   -> existing.getCount() + 1;
                        case "MINUS"  -> Math.max(0, existing.getCount() - 1);
                        case "DELETE" -> 0;
                        default       -> existing.getCount();
                    };
                    if (newCount <= 0) {
                        return cartItemRepository.deleteById(itemId)
                                .thenReturn(Boolean.TRUE);
                    }
                    // version != null → Spring Data сделает UPDATE
                    existing.setCount(newCount);
                    return cartItemRepository.save(existing)
                            .thenReturn(Boolean.TRUE);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    if (!"PLUS".equals(action)) {
                        return Mono.just(Boolean.FALSE);
                    }
                    // Явный INSERT — template.insert() всегда INSERT независимо от id
                    return template.insert(CartItem.class)
                            .using(new CartItem(itemId, 1))
                            .thenReturn(Boolean.FALSE);
                }))
                .then();
    }

    public Mono<Integer> getCount(Long itemId) {
        return cartItemRepository.findById(itemId)
                .map(CartItem::getCount)
                .defaultIfEmpty(0);
    }

    public Mono<Void> clear() {
        return cartItemRepository.deleteAll();
    }

    public Flux<CartItem> getAllCartItems() {
        return cartItemRepository.findAll();
    }
}
package ru.yandex.practicum.mymarket.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.entity.Item;
import ru.yandex.practicum.mymarket.entity.OrderItem;
import ru.yandex.practicum.mymarket.mapper.ItemMapper;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
@RequiredArgsConstructor
public class CartService {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    private final Map<Long, Integer> cart = new HashMap<>();

    public long getTotalPrice() {
        return getCartItems().stream()
                .mapToLong(item -> item.getPrice() * item.getCount())
                .reduce(0L, Long::sum);
    }

    public List<ItemDto> getCartItems() {
        return cart.entrySet().stream()
                .map(entry -> {
                    Item item = itemRepository.findById(entry.getKey())
                            .orElseThrow(() -> new RuntimeException("Item not found"));
                    ItemDto dto = itemMapper.toDto(item);
                    dto.setCount(entry.getValue());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public List<OrderItem> prepareOrderItems() {
        return cart.entrySet().stream()
                .map(entry -> {
                    Item item = itemRepository.findById(entry.getKey())
                            .orElseThrow(() -> new RuntimeException("Item not found"));
                    return new OrderItem()
                            .setItem(item)
                            .setCount(entry.getValue());
                })
                .collect(Collectors.toList());
    }

    public void addToCart(Long itemId, int quantity) {
        cart.put(itemId, cart.getOrDefault(itemId, 0) + quantity);
    }

    public void removeFromCart(Item item) {
        cart.remove(item.getId());
    }

    public void minusFromCart(Item item) {
        if (cart.containsKey(item.getId())) {
            cart.put(item.getId(), Math.max((cart.get(item.getId()) - 1), 0));
        }

    }

    public void plusFromCart(Item item) {
        if (cart.containsKey(item.getId())) {
            cart.put(item.getId(), Math.max((cart.get(item.getId()) + 1), 0));
        }
    }

    public void clearCart() {
        cart.clear();
    }

    public int getItemCount(Item item) {
        return cart.getOrDefault(item.getId(), 0);
    }

    public void updateCount(Item item, String action) {
        for (Long i : cart.keySet()) {
            if (i.equals(item.getId())) {
                if ("PLUS".equals(action)) plusFromCart(item);
                if ("MINUS".equals(action)) minusFromCart(item);
                if ("DELETE".equals(action)) removeFromCart(item);
                return;
            }
        }
        addToCart(item.getId(), 1);
    }
}
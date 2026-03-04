package ru.yandex.practicum.mymarket.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.entity.Item;
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

    private final Map<Item, Integer> cart = new HashMap<>();

    public long getTotalPrice() {
        return getCartItems().stream()
                .mapToLong(item -> item.getPrice() * item.getCount())
                .reduce(0L, Long::sum);
    }

    public List<ItemDto> getCartItems() {
        return cart.entrySet().stream()
                .map(entry -> {
                    ItemDto dto = itemMapper.toDto(entry.getKey());
                    dto.setCount(entry.getValue());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public void addToCart(Long itemId, int quantity) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));
        cart.put(item, cart.getOrDefault(itemId, 0) + quantity);
    }

    public void removeFromCart(Item item) {
        cart.remove(item);
    }

    public void clearCart() {
        cart.clear();
    }

    public void updateCount(Item item, String action) {
        for (Item i : cart.keySet()) {
            if (i.getId().equals(item.getId())) {
                if ("PLUS".equals(action)) i.setCount(i.getCount() + 1);
                if ("MINUS".equals(action) && i.getCount() > 1) i.setCount(i.getCount() - 1);
                if ("DELETE".equals(action)) removeFromCart(i);
                return;
            }
        }
    }

}
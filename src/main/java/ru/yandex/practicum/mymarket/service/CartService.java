package ru.yandex.practicum.mymarket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.entity.CartItem;
import ru.yandex.practicum.mymarket.entity.Item;
import ru.yandex.practicum.mymarket.mapper.ItemMapper;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;

    private final ItemMapper itemMapper;

    public List<ItemDto> getCart(String sessionId) {
        List<CartItem> items = cartItemRepository.findBySessionId(sessionId);
        return items.stream()
                .map(item -> {
                    ItemDto dto = itemMapper.toDto(item.getItem());
                    dto.setCount(item.getQuantity());
                    return dto;
                })
                .toList();
    }

    public void addItem(String sessionId, Long itemId) {

        Item item = itemRepository.findById(itemId)
                .orElseThrow();

        CartItem cartItem = cartItemRepository
                .findBySessionIdAndItemId(sessionId, itemId)
                .orElse(null);

        if (cartItem == null) {

            cartItem = new CartItem();
            cartItem.setSessionId(sessionId);
            cartItem.setItem(item);
            cartItem.setQuantity(1);

        } else {

            cartItem.setQuantity(cartItem.getQuantity() + 1);

        }

        cartItemRepository.save(cartItem);
    }

    public void removeItem(String sessionId, Long itemId) {

        CartItem cartItem = cartItemRepository
                .findBySessionIdAndItemId(sessionId, itemId)
                .orElseThrow();

        cartItemRepository.delete(cartItem);
    }

    public void clearCart(String sessionId) {
        cartItemRepository.deleteBySessionId(sessionId);
    }

    @Transactional
    public void updateQuantity(String sessionId, Long itemId, String action) {

        CartItem cartItem = cartItemRepository
                .findBySessionIdAndItemId(sessionId, itemId)
                .orElse(null);

        if (cartItem == null) {
            addItem(sessionId, itemId);
            return;
        }

        int quantity = cartItem.getQuantity();

        if ("PLUS".equals(action)) {
            cartItem.setQuantity(quantity + 1);
            cartItemRepository.save(cartItem);
        } else if ("MINUS".equals(action)) {
            if (quantity > 1) {
                cartItem.setQuantity(quantity - 1);
                cartItemRepository.save(cartItem);
            } else {
                cartItemRepository.delete(cartItem);
            }
        } else if ("DELETE".equals(action)) {
            cartItemRepository.delete(cartItem);
        }


    }
}
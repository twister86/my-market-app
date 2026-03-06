package ru.yandex.practicum.mymarket.service;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.entity.CartItem;
import ru.yandex.practicum.mymarket.entity.Item;
import ru.yandex.practicum.mymarket.mapper.ItemMapper;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ItemService {
    private final ItemRepository itemRepository;
    private final CartItemRepository cartItemRepository;
    private final ItemMapper itemMapper;

    @Transactional(readOnly = true)
    public Page<ItemDto> findAll(String search, String sort, int pageNumber, int pageSize, String sessionId) {
        Sort sortOrder = Sort.unsorted();
        if ("ALPHA".equals(sort)) {
            sortOrder = Sort.by(Sort.Direction.ASC, "title");
        } else if ("PRICE".equals(sort)) {
            sortOrder = Sort.by(Sort.Direction.ASC, "price");
        }
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, sortOrder);

        Page<Item> items;

        // Поиск по названию/описанию
        if (search != null && !search.isEmpty()) {
            items = itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search, pageable);
        } else {
            items = itemRepository.findAll(pageable);
        }

        List<CartItem> cartItems = cartItemRepository.findBySessionId(sessionId);

        Map<Long, Integer> cartMap =
                cartItems.stream()
                        .collect(Collectors.toMap(
                                c -> c.getItem().getId(),
                                CartItem::getQuantity
                        ));

        List<ItemDto> dtoList = items.getContent().stream()
                .map(item -> {
                    ItemDto dto = itemMapper.toDto(item);
                    dto.setCount(cartMap.getOrDefault(item.getId(), 0));
                    return dto;
                })
                .toList();

        return new PageImpl<>(dtoList, items.getPageable(), items.getTotalElements());
    }

    public ItemDto findById(Long id) {
        return itemMapper.toDto(itemRepository.findById(id).orElse(null));
    }

    public Item findItemById(Long id) {
        return itemRepository.findById(id).orElse(null);
    }
}
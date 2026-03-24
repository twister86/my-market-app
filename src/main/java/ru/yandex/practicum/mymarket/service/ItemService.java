package ru.yandex.practicum.mymarket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.PagingDto;
import ru.yandex.practicum.mymarket.dto.SortType;
import ru.yandex.practicum.mymarket.entity.Item;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final CartService cartService;

    public Mono<List<List<Item>>> getItemsPage(String search, String sort,
                                               int pageNumber, int pageSize,
                                               String sessionId) {
        return getItemsPage(search, SortType.fromString(sort), pageNumber, pageSize, sessionId);
    }

    public Mono<List<List<Item>>> getItemsPage(String search, SortType sort,
                                               int pageNumber, int pageSize,
                                               String sessionId) {
        Flux<Item> source = (search != null && !search.isBlank())
                ? itemRepository.findByTitleOrDescriptionContainingIgnoreCase(search)
                : itemRepository.findAll();

        return source
                .concatMap(item -> cartService.getCount(item.getId(), sessionId)
                        .map(count -> {
                            item.setCount(count);
                            return item;
                        }))
                .collectList()
                .map(items -> toPagedRows(items, sort, pageNumber, pageSize));
    }

    private List<List<Item>> toPagedRows(List<Item> items, SortType sort, int pageNumber, int pageSize) {
        switch (sort) {
            case ALPHA -> items.sort(Comparator.comparing(Item::getTitle));
            case PRICE -> items.sort(Comparator.comparingLong(Item::getPrice));
            default    -> { /* NO — без сортировки */ }
        }

        int fromIndex = (pageNumber - 1) * pageSize;
        if (fromIndex >= items.size()) {
            return new ArrayList<>();
        }
        int toIndex = Math.min(fromIndex + pageSize, items.size());
        List<Item> page = new ArrayList<>(items.subList(fromIndex, toIndex));

        while (page.size() % 3 != 0) {
            Item stub = new Item();
            stub.setId(-1L);
            page.add(stub);
        }

        List<List<Item>> rows = new ArrayList<>();
        for (int i = 0; i < page.size(); i += 3) {
            rows.add(page.subList(i, i + 3));
        }
        return rows;
    }

    public Mono<Long> countItems(String search) {
        if (search != null && !search.isBlank()) {
            return itemRepository.findByTitleOrDescriptionContainingIgnoreCase(search).count();
        }
        return itemRepository.count();
    }

    public Mono<Item> getItem(Long id, String sessionId) {
        return itemRepository.findById(id)
                .flatMap(item -> cartService.getCount(item.getId(), sessionId)
                        .map(count -> {
                            item.setCount(count);
                            return item;
                        }));
    }

    public Mono<Item> save(Item item) {
        return itemRepository.save(item);
    }

    public Mono<PagingDto> buildPaging(String search, String sort,
                                       int pageNumber, int pageSize) {
        return countItems(search).map(total -> {
            int totalPages = (int) Math.ceil((double) total / pageSize);
            return new PagingDto(
                    pageSize,
                    pageNumber,
                    pageNumber > 1,
                    pageNumber < totalPages
            );
        });
    }
}
package ru.yandex.practicum.mymarket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.PagingDto;
import ru.yandex.practicum.mymarket.dto.SortType;
import ru.yandex.practicum.mymarket.entity.Item;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemService {

    private static final String CACHE_PREFIX = "item:";
    private static final Duration CACHE_TTL  = Duration.ofMinutes(10);

    private final ItemRepository itemRepository;
    private final CartService cartService;
    private final ReactiveRedisTemplate<String, Item> redisTemplate;

    public Mono<List<List<Item>>> getItemsPage(String search, String sort,
                                               int pageNumber, int pageSize,
                                               String sessionId) {
        return getItemsPage(search, SortType.fromString(sort), pageNumber, pageSize, sessionId);
    }

    public Mono<List<List<Item>>> getItemsPage(String search, SortType sort,
                                               int pageNumber, int pageSize,
                                               String sessionId) {
        int offset = (pageNumber - 1) * pageSize;
        boolean hasSearch = search != null && !search.isBlank();

        // Пагинация на уровне БД — не загружаем все записи в память
        Flux<Item> source = switch (sort) {
            case ALPHA -> hasSearch
                    ? itemRepository.findBySearchPagedOrderByTitle(search, pageSize, offset)
                    : itemRepository.findAllPagedOrderByTitle(pageSize, offset);
            case PRICE -> hasSearch
                    ? itemRepository.findBySearchPagedOrderByPrice(search, pageSize, offset)
                    : itemRepository.findAllPagedOrderByPrice(pageSize, offset);
            default    -> hasSearch
                    ? itemRepository.findBySearchPaged(search, pageSize, offset)
                    : itemRepository.findAllPaged(pageSize, offset);
        };

        return source
                .concatMap(item -> enrichWithCount(item, sessionId))
                .collectList()
                .map(this::toRows);
    }

    public Mono<Item> getItem(Long id, String sessionId) {
        String key = CACHE_PREFIX + id;
        return redisTemplate.opsForValue().get(key)
                .doOnNext(i -> log.debug("Cache HIT item id={}", id))
                .switchIfEmpty(Mono.defer(() ->
                        itemRepository.findById(id)
                                .doOnNext(i -> log.debug("Cache MISS item id={}", id))
                                .flatMap(i -> redisTemplate.opsForValue()
                                        .set(key, i, CACHE_TTL)
                                        .thenReturn(i))
                ))
                .flatMap(item -> enrichWithCount(item, sessionId));
    }

    public Mono<Void> evictCache(Long id) {
        return redisTemplate.opsForValue().delete(CACHE_PREFIX + id).then();
    }

    public Mono<Item> save(Item item) {
        return itemRepository.save(item)
                .flatMap(saved -> evictCache(saved.getId()).thenReturn(saved));
    }

    public Mono<Long> countItems(String search) {
        if (search != null && !search.isBlank()) {
            return itemRepository.countBySearch(search);
        }
        return itemRepository.count();
    }

    public Mono<PagingDto> buildPaging(String search, String sort,
                                       int pageNumber, int pageSize) {
        return countItems(search).map(total -> {
            int totalPages = (int) Math.ceil((double) total / pageSize);
            return new PagingDto(pageSize, pageNumber, pageNumber > 1, pageNumber < totalPages);
        });
    }

    private Mono<Item> enrichWithCount(Item item, String sessionId) {
        return cartService.getCount(item.getId(), sessionId)
                .map(count -> {
                    item.setCount(count);
                    return item;
                });
    }

    /** Разбиваем плоский список на строки по 3 товара для отображения сеткой */
    private List<List<Item>> toRows(List<Item> items) {
        // Дополняем до кратного 3 пустыми заглушками
        List<Item> padded = new ArrayList<>(items);
        while (padded.size() % 3 != 0) {
            Item stub = new Item();
            stub.setId(-1L);
            padded.add(stub);
        }
        List<List<Item>> rows = new ArrayList<>();
        for (int i = 0; i < padded.size(); i += 3) {
            rows.add(padded.subList(i, i + 3));
        }
        return rows;
    }
}
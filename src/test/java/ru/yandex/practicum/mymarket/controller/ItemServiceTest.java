package ru.yandex.practicum.mymarket.controller;

import ru.yandex.practicum.mymarket.entity.Item;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private CartService cartService;

    @InjectMocks
    private ItemService itemService;

    private Item ball;
    private Item racket;
    private Item helmet;

    @BeforeEach
    void setUp() {
        ball   = Item.builder().id(1L).title("Мяч").description("Футбольный мяч").price(1500L).build();
        racket = Item.builder().id(2L).title("Ракетка").description("Теннисная ракетка").price(3800L).build();
        helmet = Item.builder().id(3L).title("Шлем").description("Велосипедный шлем").price(2900L).build();

        when(cartService.getCount(anyLong())).thenReturn(Mono.just(0));
    }

    @Test
    void getItemsPage_noSearch_returnsAllItemsGroupedByThree() {
        when(itemRepository.findAll()).thenReturn(Flux.just(ball, racket, helmet));

        StepVerifier.create(itemService.getItemsPage("", "NO", 1, 5))
                .assertNext(rows -> {
                    assertThat(rows).hasSize(1);
                    assertThat(rows.getFirst()).hasSize(3);
                    assertThat(rows.getFirst().getFirst().getId()).isEqualTo(1L);
                })
                .verifyComplete();
    }

    @Test
    void getItemsPage_withSearch_filtersItems() {
        when(itemRepository.findByTitleOrDescriptionContainingIgnoreCase("мяч"))
                .thenReturn(Flux.just(ball));

        StepVerifier.create(itemService.getItemsPage("мяч", "NO", 1, 5))
                .assertNext(rows -> {
                    // 1 товар + 2 заглушки = 1 строка из 3
                    assertThat(rows).hasSize(1);
                    assertThat(rows.get(0).get(0).getId()).isEqualTo(1L);
                    assertThat(rows.get(0).get(1).getId()).isEqualTo(-1L); // stub
                })
                .verifyComplete();
    }

    @Test
    void getItemsPage_sortByAlpha_sortsCorrectly() {
        when(itemRepository.findAll()).thenReturn(Flux.just(racket, ball, helmet));

        StepVerifier.create(itemService.getItemsPage("", "ALPHA", 1, 5))
                .assertNext(rows -> {
                    List<Item> flat = rows.stream().flatMap(List::stream)
                            .filter(i -> !i.isStub()).toList();
                    assertThat(flat.get(0).getTitle()).isEqualTo("Мяч");
                    assertThat(flat.get(1).getTitle()).isEqualTo("Ракетка");
                    assertThat(flat.get(2).getTitle()).isEqualTo("Шлем");
                })
                .verifyComplete();
    }

    @Test
    void getItemsPage_sortByPrice_sortsCorrectly() {
        when(itemRepository.findAll()).thenReturn(Flux.just(racket, ball, helmet));

        StepVerifier.create(itemService.getItemsPage("", "PRICE", 1, 5))
                .assertNext(rows -> {
                    List<Item> flat = rows.stream().flatMap(List::stream)
                            .filter(i -> !i.isStub()).toList();
                    assertThat(flat.get(0).getPrice()).isEqualTo(1500L);
                    assertThat(flat.get(1).getPrice()).isEqualTo(2900L);
                    assertThat(flat.get(2).getPrice()).isEqualTo(3800L);
                })
                .verifyComplete();
    }

    @Test
    void getItemsPage_pagination_secondPage() {
        // pageSize=2, pageNumber=2 → должна вернуть только helmet
        when(itemRepository.findAll()).thenReturn(Flux.just(ball, racket, helmet));

        StepVerifier.create(itemService.getItemsPage("", "NO", 2, 2))
                .assertNext(rows -> {
                    List<Item> nonStub = rows.stream().flatMap(List::stream)
                            .filter(i -> !i.isStub()).toList();
                    assertThat(nonStub).hasSize(1);
                    assertThat(nonStub.get(0).getId()).isEqualTo(3L);
                })
                .verifyComplete();
    }

    @Test
    void buildPaging_firstPage_hasPreviousFalse() {
        when(itemRepository.count()).thenReturn(Mono.just(12L));

        StepVerifier.create(itemService.buildPaging("", "NO", 1, 5))
                .assertNext(paging -> {
                    assertThat(paging.isHasPrevious()).isFalse();
                    assertThat(paging.isHasNext()).isTrue();
                    assertThat(paging.getPageNumber()).isEqualTo(1);
                    assertThat(paging.getPageSize()).isEqualTo(5);
                })
                .verifyComplete();
    }

    @Test
    void buildPaging_lastPage_hasNextFalse() {
        when(itemRepository.count()).thenReturn(Mono.just(12L));

        StepVerifier.create(itemService.buildPaging("", "NO", 3, 5))
                .assertNext(paging -> {
                    assertThat(paging.isHasPrevious()).isTrue();
                    assertThat(paging.isHasNext()).isFalse();
                })
                .verifyComplete();
    }

    @Test
    void getItem_returnsItemWithCartCount() {
        when(itemRepository.findById(1L)).thenReturn(Mono.just(ball));
        when(cartService.getCount(1L)).thenReturn(Mono.just(3));

        StepVerifier.create(itemService.getItem(1L))
                .assertNext(item -> {
                    assertThat(item.getId()).isEqualTo(1L);
                    assertThat(item.getCount()).isEqualTo(3);
                })
                .verifyComplete();
    }
}

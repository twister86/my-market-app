package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.entity.CartItem;
import ru.yandex.practicum.mymarket.entity.Item;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.service.CartService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private CartService cartService;

    private Item item1;
    private Item item2;

    @BeforeEach
    void setUp() {
        item1 = Item.builder().id(1L).title("Мяч").price(1500L).count(0).build();
        item2 = Item.builder().id(2L).title("Ракетка").price(3800L).count(0).build();
    }

    @Test
    void getCartItems_returnsItemsWithCount() {
        CartItem ci1 = new CartItem(1L, 2);
        CartItem ci2 = new CartItem(2L, 1);

        when(cartItemRepository.findAll()).thenReturn(Flux.just(ci1, ci2));
        when(itemRepository.findById(1L)).thenReturn(Mono.just(item1));
        when(itemRepository.findById(2L)).thenReturn(Mono.just(item2));

        StepVerifier.create(cartService.getCartItems())
                .expectNextMatches(i -> i.getId() == 1L && i.getCount() == 2)
                .expectNextMatches(i -> i.getId() == 2L && i.getCount() == 1)
                .verifyComplete();
    }

    @Test
    void getTotal_calculatesCorrectSum() {
        CartItem ci1 = new CartItem(1L, 2); // 1500 * 2 = 3000
        CartItem ci2 = new CartItem(2L, 1); // 3800 * 1 = 3800

        when(cartItemRepository.findAll()).thenReturn(Flux.just(ci1, ci2));
        when(itemRepository.findById(1L)).thenReturn(Mono.just(item1));
        when(itemRepository.findById(2L)).thenReturn(Mono.just(item2));

        StepVerifier.create(cartService.getTotal())
                .expectNext(6800L)
                .verifyComplete();
    }

    @Test
    void updateCart_plus_incrementsCount() {
        when(cartItemRepository.findById(1L)).thenReturn(Mono.just(new CartItem(1L, 1)));
        when(cartItemRepository.save(any())).thenReturn(Mono.just(new CartItem(1L, 2)));

        StepVerifier.create(cartService.updateCart(1L, "PLUS"))
                .verifyComplete();

        verify(cartItemRepository).save(argThat(ci -> ci.getCount() == 2));
    }

    @Test
    void updateCart_minus_decrementsCount() {
        when(cartItemRepository.findById(1L)).thenReturn(Mono.just(new CartItem(1L, 3)));
        when(cartItemRepository.save(any())).thenReturn(Mono.just(new CartItem(1L, 2)));

        StepVerifier.create(cartService.updateCart(1L, "MINUS"))
                .verifyComplete();

        verify(cartItemRepository).save(argThat(ci -> ci.getCount() == 2));
    }

    @Test
    void updateCart_minus_toZero_deletesItem() {
        when(cartItemRepository.findById(1L)).thenReturn(Mono.just(new CartItem(1L, 1)));
        when(cartItemRepository.deleteById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(cartService.updateCart(1L, "MINUS"))
                .verifyComplete();

        verify(cartItemRepository).deleteById(1L);
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void updateCart_delete_removesItem() {
        when(cartItemRepository.findById(1L)).thenReturn(Mono.just(new CartItem(1L, 5)));
        when(cartItemRepository.deleteById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(cartService.updateCart(1L, "DELETE"))
                .verifyComplete();

        verify(cartItemRepository).deleteById(1L);
    }

    @Test
    void updateCart_plus_newItem_savesWithCountOne() {
        when(cartItemRepository.findById(99L)).thenReturn(Mono.empty());
        when(cartItemRepository.save(any())).thenReturn(Mono.just(new CartItem(99L, 1)));

        StepVerifier.create(cartService.updateCart(99L, "PLUS"))
                .verifyComplete();

        verify(cartItemRepository).save(argThat(ci -> ci.getId() == 99L && ci.getCount() == 1));
    }

    @Test
    void getCount_returnsCountForExistingItem() {
        when(cartItemRepository.findById(1L)).thenReturn(Mono.just(new CartItem(1L, 4)));

        StepVerifier.create(cartService.getCount(1L))
                .expectNext(4)
                .verifyComplete();
    }

    @Test
    void getCount_returnsZeroForMissingItem() {
        when(cartItemRepository.findById(999L)).thenReturn(Mono.empty());

        StepVerifier.create(cartService.getCount(999L))
                .expectNext(0)
                .verifyComplete();
    }

    @Test
    void clear_deletesAllCartItems() {
        when(cartItemRepository.deleteAll()).thenReturn(Mono.empty());

        StepVerifier.create(cartService.clear())
                .verifyComplete();

        verify(cartItemRepository).deleteAll();
    }
}

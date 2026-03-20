package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.entity.Item;
import ru.yandex.practicum.mymarket.service.CartService;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@WebFluxTest(CartController.class)
class CartControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CartService cartService;

    private Item item;

    @BeforeEach
    void setUp() {
        item = Item.builder().id(1L).title("Мяч").price(1500L).count(2).build();

        when(cartService.getCartItems(anyString()))
                .thenReturn(Flux.just(item));
        when(cartService.getTotal(anyString()))
                .thenReturn(Mono.just(3000L));
        when(cartService.updateCart(anyLong(), anyString(), anyString()))
                .thenReturn(Mono.empty());
    }

    @Test
    void getCart_returns200() {
        webTestClient.get().uri("/cart/items")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void postCart_plusAction_returns200() {
        webTestClient.post().uri("/cart/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("id", "1")
                        .with("action", "PLUS"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void postCart_minusAction_returns200() {
        webTestClient.post().uri("/cart/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("id", "1")
                        .with("action", "MINUS"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void postCart_deleteAction_returns200() {
        webTestClient.post().uri("/cart/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("id", "1")
                        .with("action", "DELETE"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void postCart_emptyCart_stillReturns200() {
        when(cartService.getCartItems(anyString())).thenReturn(Flux.empty());
        when(cartService.getTotal(anyString())).thenReturn(Mono.just(0L));

        webTestClient.post().uri("/cart/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("id", "99")
                        .with("action", "DELETE"))
                .exchange()
                .expectStatus().isOk();
    }
}

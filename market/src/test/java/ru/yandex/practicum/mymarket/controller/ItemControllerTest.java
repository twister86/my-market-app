package ru.yandex.practicum.mymarket.controller;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.config.SecurityConfig;
import ru.yandex.practicum.mymarket.dto.PagingDto;
import ru.yandex.practicum.mymarket.entity.Item;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;
import ru.yandex.practicum.mymarket.service.UserDetailsServiceImpl;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@WebFluxTest(ItemController.class)
@Import(SecurityConfig.class)
class ItemControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean private ItemService itemService;
    @MockitoBean private CartService cartService;
    @MockitoBean private UserDetailsServiceImpl userDetailsService;

    private Item item;
    private PagingDto paging;

    @BeforeEach
    void setUp() {
        item = Item.builder().id(1L).title("Мяч").description("Описание").price(1500L).count(0).build();
        paging = new PagingDto(5, 1, false, false);

        when(itemService.getItemsPage(anyString(), anyString(), anyInt(), anyInt(), anyString()))
                .thenReturn(Mono.just(List.of(List.of(item))));
        when(itemService.buildPaging(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(Mono.just(paging));
        when(itemService.getItem(anyLong(), anyString()))
                .thenReturn(Mono.just(item));
        when(cartService.updateCart(anyLong(), anyString(), anyString()))
                .thenReturn(Mono.empty());
    }

    // ===== Публичные эндпоинты — доступны анонимам =====

    @Test
    void getItems_anonymous_returns200() {
        webTestClient.get().uri("/items")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getItems_withSearchAndSort_returns200() {
        webTestClient.get().uri("/items?search=мяч&sort=PRICE&pageNumber=1&pageSize=5")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getItem_anonymous_returns200() {
        webTestClient.get().uri("/items/1")
                .exchange()
                .expectStatus().isOk();
    }

    // ===== POST эндпоинты — только авторизованным =====

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void postItems_withPlusAction_redirects() {
        webTestClient.post().uri("/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("id", "1")
                        .with("action", "PLUS")
                        .with("search", "")
                        .with("sort", "NO")
                        .with("pageNumber", "1")
                        .with("pageSize", "5"))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", ".*/items.*");
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void postItems_withoutId_redirectsWithoutUpdate() {
        webTestClient.post().uri("/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("search", "")
                        .with("sort", "NO")
                        .with("pageNumber", "1")
                        .with("pageSize", "5"))
                .exchange()
                .expectStatus().is3xxRedirection();
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void postItemById_withPlusAction_returns200() {
        webTestClient.post().uri("/items/1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("action", "PLUS"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void postItemById_withMinusAction_returns200() {
        webTestClient.post().uri("/items/1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("action", "MINUS"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void postItems_anonymous_redirectsToLogin() {
        webTestClient.post().uri("/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("id", "1").with("action", "PLUS"))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", ".*/login.*");
    }
}
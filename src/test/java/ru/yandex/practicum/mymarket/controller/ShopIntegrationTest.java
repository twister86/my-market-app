package ru.yandex.practicum.mymarket.controller;

import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class ShopIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @BeforeEach
    void setUp() {
        cartItemRepository.deleteAll().block();
    }

    @Test
    void itemsPage_returns200() {
        webTestClient.get().uri("/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assert body.contains("Витрина товаров");
                });
    }

    @Test
    void itemsPage_withSearch_returns200() {
        webTestClient.get().uri("/items?search=мяч")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void itemsPage_withSort_returns200() {
        webTestClient.get().uri("/items?sort=PRICE")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void itemPage_returns200() {
        Long id = itemRepository.findAll().blockFirst().getId();

        webTestClient.get().uri("/items/" + id)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("В корзину"));
    }

    @Test
    void cartPage_returns200() {
        webTestClient.get().uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("Корзина"));
    }

    @Test
    void ordersPage_returns200() {
        webTestClient.get().uri("/orders")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("заказ"));
    }

    @Test
    void addToCart_redirectsToItems() {
        Long id = itemRepository.findAll().blockFirst().getId();

        webTestClient.post()
                .uri("/items?id=" + id + "&action=PLUS")
                .exchange()
                .expectStatus().is3xxRedirection();
    }

    @Test
    void addItemForm_returns200() {
        webTestClient.get().uri("/admin/items/add")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("Добавить товар"));
    }

    @Test
    void addItem_redirectsToItems() {
        webTestClient.post()
                .uri("/admin/items/add?title=Тест&description=Описание&price=999")
                .exchange()
                .expectStatus().is3xxRedirection();
    }

    @Test
    void checkout_withEmptyCart_redirectsToOrder() {
        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", ".*/orders/.*newOrder=true");
    }
}

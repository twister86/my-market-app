package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ShopIntegrationTest {

    @LocalServerPort
    private int port;

    // Создаём WebTestClient вручную с baseUrl — так заголовки передаются корректно
    private WebTestClient client;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    private static final String COOKIE_NAME = "SHOP_SESSION";

    @BeforeEach
    void setUp() {
        client = WebTestClient
                .bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();

        cartItemRepository.deleteAll().block();
        orderRepository.deleteAll().block();
    }

    @Test
    void fullShoppingFlow() {
        // 1. Первый запрос — получаем куку сессии из ответа
        MultiValueMap<String, ResponseCookie> cookies = client.get().uri("/items")
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class)
                .getResponseCookies();

        assertThat(cookies.containsKey(COOKIE_NAME))
                .as("Сервер должен вернуть куку SHOP_SESSION").isTrue();

        String sessionId     = cookies.getFirst(COOKIE_NAME).getValue();
        String sessionCookie = COOKIE_NAME + "=" + sessionId;

        // 2. Берём id первого товара — если товаров нет, пропускаем тест
        Long itemId = itemRepository.findAll()
                .map(item -> item.getId())
                .blockFirst();

        assumeTrue(itemId != null, "Товаров нет в БД — тест пропущен");

        // 3. Добавляем товар в корзину — передаём куку
        client.post().uri("/items")
                .header(HttpHeaders.COOKIE, sessionCookie)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("id", itemId.toString())
                        .with("action", "PLUS")
                        .with("search", "")
                        .with("sort", "NO")
                        .with("pageNumber", "1")
                        .with("pageSize", "5"))
                .exchange()
                .expectStatus().is3xxRedirection();

        // 4. Проверяем что запись в БД создана с нашей сессией
        Long cartCount = cartItemRepository.findAll()
                .filter(ci -> sessionId.equals(ci.getSessionId()))
                .count()
                .block();
        assertThat(cartCount).as("В корзине должен быть 1 товар").isGreaterThan(0);

        // 5. Корзина отвечает 200
        client.get().uri("/cart/items")
                .header(HttpHeaders.COOKIE, sessionCookie)
                .exchange()
                .expectStatus().isOk();

        // 6. Оформляем заказ
        String location = client.post().uri("/orders/buy")
                .header(HttpHeaders.COOKIE, sessionCookie)
                .exchange()
                .expectStatus().is3xxRedirection()
                .returnResult(String.class)
                .getResponseHeaders()
                .getFirst("Location");

        assertThat(location).isNotNull();
        assertThat(location).contains("/orders/");
        assertThat(location).contains("newOrder=true");

        // 7. После оформления корзина пуста
        Long cartAfter = cartItemRepository.findAll()
                .filter(ci -> sessionId.equals(ci.getSessionId()))
                .count()
                .block();
        assertThat(cartAfter).as("Корзина должна быть пуста после заказа").isZero();

        // 8. Страница заказа отвечает 200
        client.get().uri(location)
                .header(HttpHeaders.COOKIE, sessionCookie)
                .exchange()
                .expectStatus().isOk();

        // 9. Список заказов отвечает 200
        client.get().uri("/orders")
                .header(HttpHeaders.COOKIE, sessionCookie)
                .exchange()
                .expectStatus().isOk();

        // 10. Заказ сохранён в БД для нашей сессии
        Long orderCount = orderRepository.findBySessionId(sessionId).count().block();
        assertThat(orderCount).as("Должен быть ровно 1 заказ").isEqualTo(1);
    }
}
package ru.yandex.practicum.mymarket.controller;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import ru.yandex.practicum.mymarket.entity.Item;
import ru.yandex.practicum.mymarket.entity.User;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;
import ru.yandex.practicum.mymarket.repository.UserRepository;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ShopIntegrationTest {

    static MockWebServer mockPaymentServer = new MockWebServer();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) throws IOException {
        mockPaymentServer.start();
        mockPaymentServer.setDispatcher(new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest request) {
                String path = request.getPath();
                if (path != null && path.startsWith("/payment/balance/")) {
                    return new MockResponse()
                            .setResponseCode(200)
                            .addHeader("Content-Type", "application/json")
                            .setBody("{\"userId\":\"test\",\"balance\":999999}");
                }
                if ("/payment/pay".equals(path)) {
                    return new MockResponse()
                            .setResponseCode(200)
                            .addHeader("Content-Type", "application/json")
                            .setBody("{\"success\":true,\"remainingBalance\":990000,\"orderId\":1}");
                }
                // OAuth2 token endpoint
                if (path != null && path.contains("/oauth2/token")) {
                    return new MockResponse()
                            .setResponseCode(200)
                            .addHeader("Content-Type", "application/json")
                            .setBody("{\"access_token\":\"test-token\",\"token_type\":\"Bearer\",\"expires_in\":3600}");
                }
                return new MockResponse().setResponseCode(404);
            }
        });
        registry.add("payment.service.url",
                () -> "http://localhost:" + mockPaymentServer.getPort());
        registry.add("spring.security.oauth2.client.provider.payment-client.token-uri",
                () -> "http://localhost:" + mockPaymentServer.getPort() + "/oauth2/token");
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockPaymentServer.shutdown();
    }

    @LocalServerPort
    private int port;

    private WebTestClient client;

    @Autowired private ItemRepository itemRepository;
    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @BeforeEach
    void setUp() {
        client = WebTestClient
                .bindToServer()
                .baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofSeconds(10))
                .build();

        cartItemRepository.deleteAll().block(TIMEOUT);
        orderRepository.deleteAll().block(TIMEOUT);

        // Создаём тестового пользователя если ещё не существует
        userRepository.findByUsername("user")
                .switchIfEmpty(userRepository.save(
                        User.builder()
                                .username("user")
                                .password(passwordEncoder.encode("user123"))
                                .role("ROLE_USER")
                                .build()
                ))
                .block(TIMEOUT);
    }

    /**
     * Логинится через форму, получает SESSION-куку Spring Security,
     * использует её для всех последующих запросов.
     */
    private String login() {
        var result = client.post().uri("/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("username", "user")
                        .with("password", "user123"))
                .exchange()
                .expectStatus().is3xxRedirection()
                .returnResult(String.class);

        String location = result.getResponseHeaders().getFirst("Location");
        System.out.println("=== LOGIN redirect location: " + location);
        System.out.println("=== LOGIN response cookies: " + result.getResponseCookies());

        // Spring Security при неудачном логине редиректит на /login?error
        // При успехе — на / или на запрошенную страницу
        assertThat(location)
                .as("Редирект после логина не должен вести на /login?error")
                .doesNotContain("error");

        var sessionCookie = result.getResponseCookies().getFirst("SESSION");
        assertThat(sessionCookie)
                .as("SESSION кука должна быть установлена после логина")
                .isNotNull();
        return "SESSION=" + sessionCookie.getValue();
    }

    @Test
    void fullShoppingFlow() {
        // 1. Логинимся — получаем SESSION куку
        String sessionCookie = login();

        // 2. Берём id первого товара
        Long itemId = itemRepository.findAll()
                .map(Item::getId)
                .blockFirst(TIMEOUT);
        assumeTrue(itemId != null, "Товаров нет в БД — тест пропущен");

        // 3. Добавляем товар в корзину
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

        // 4. Проверяем что запись в БД создана для пользователя "user"
        Long cartCount = cartItemRepository.findAll()
                .filter(ci -> "user".equals(ci.getSessionId()))
                .count()
                .block(TIMEOUT);
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
                .filter(ci -> "user".equals(ci.getSessionId()))
                .count()
                .block(TIMEOUT);
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

        // 10. Заказ сохранён в БД для пользователя "user"
        Long orderCount = orderRepository.findBySessionId("user").count().block(TIMEOUT);
        assertThat(orderCount).as("Должен быть ровно 1 заказ").isEqualTo(1);
    }
}
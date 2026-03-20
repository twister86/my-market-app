package ru.yandex.practicum.mymarket.controller;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.entity.Order;
import ru.yandex.practicum.mymarket.entity.OrderItem;
import ru.yandex.practicum.mymarket.service.OrderService;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@WebFluxTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private OrderService orderService;

    private Order order;

    @BeforeEach
    void setUp() {
        OrderItem orderItem = OrderItem.builder()
                .id(1L).orderId(1L).itemId(1L)
                .title("Мяч").price(1500L).count(2)
                .build();

        order = Order.builder()
                .id(1L)
                .sessionId("test-session")
                .totalSum(3000L)
                .items(List.of(orderItem))
                .build();

        when(orderService.getAllOrders(anyString()))
                .thenReturn(Flux.just(order));
        when(orderService.getOrder(anyLong()))
                .thenReturn(Mono.just(order));
        when(orderService.checkout(anyString()))
                .thenReturn(Mono.just(1L));
    }

    @Test
    void getOrders_returns200() {
        webTestClient.get().uri("/orders")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getOrders_emptyList_returns200() {
        when(orderService.getAllOrders(anyString())).thenReturn(Flux.empty());

        webTestClient.get().uri("/orders")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getOrder_returns200() {
        webTestClient.get().uri("/orders/1")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getOrder_withNewOrderParam_returns200() {
        webTestClient.get().uri("/orders/1?newOrder=true")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void postBuy_redirectsToNewOrder() {
        webTestClient.post().uri("/orders/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", ".*/orders/1.*newOrder=true");
    }
}

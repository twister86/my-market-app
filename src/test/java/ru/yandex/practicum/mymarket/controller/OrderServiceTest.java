package ru.yandex.practicum.mymarket.controller;

import ru.yandex.practicum.mymarket.entity.CartItem;
import ru.yandex.practicum.mymarket.entity.Item;
import ru.yandex.practicum.mymarket.entity.Order;
import ru.yandex.practicum.mymarket.entity.OrderItem;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private CartService cartService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void checkout_createsOrderAndClearsCart() {
        CartItem ci = new CartItem(1L, 2);
        Item item = Item.builder().id(1L).title("Мяч").price(1500L).build();
        Order savedOrder = Order.builder().id(10L).totalSum(3000L).build();
        OrderItem oi = OrderItem.builder().id(1L).orderId(10L).itemId(1L)
                .title("Мяч").price(1500L).count(2).build();

        when(cartService.getAllCartItems()).thenReturn(Flux.just(ci));
        when(itemRepository.findById(1L)).thenReturn(Mono.just(item));
        when(orderRepository.save(any())).thenReturn(Mono.just(savedOrder));
        when(orderItemRepository.saveAll(anyList())).thenReturn(Flux.just(oi));
        when(cartService.clear()).thenReturn(Mono.empty());

        StepVerifier.create(orderService.checkout())
                .expectNext(10L)
                .verifyComplete();

        verify(orderRepository).save(argThat(o -> o.getTotalSum() == 3000L));
        verify(cartService).clear();
    }

    @Test
    void getAllOrders_returnsOrdersWithItems() {
        Order o1 = Order.builder().id(1L).totalSum(5000L).build();
        Order o2 = Order.builder().id(2L).totalSum(2000L).build();
        OrderItem oi1 = OrderItem.builder().id(1L).orderId(1L).title("Мяч").price(1500L).count(2).build();
        OrderItem oi2 = OrderItem.builder().id(2L).orderId(2L).title("Шлем").price(2000L).count(1).build();

        when(orderRepository.findAll()).thenReturn(Flux.just(o1, o2));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(Flux.just(oi1));
        when(orderItemRepository.findByOrderId(2L)).thenReturn(Flux.just(oi2));

        StepVerifier.create(orderService.getAllOrders())
                .assertNext(o -> {
                    assertThat(o.getId()).isEqualTo(1L);
                    assertThat(o.getItems()).hasSize(1);
                    assertThat(o.getItems().get(0).getTitle()).isEqualTo("Мяч");
                })
                .assertNext(o -> {
                    assertThat(o.getId()).isEqualTo(2L);
                    assertThat(o.getItems()).hasSize(1);
                })
                .verifyComplete();
    }

    @Test
    void getOrder_returnsOrderWithItems() {
        Order order = Order.builder().id(5L).totalSum(7600L).build();
        OrderItem oi = OrderItem.builder().id(1L).orderId(5L).title("Ракетка").price(3800L).count(2).build();

        when(orderRepository.findById(5L)).thenReturn(Mono.just(order));
        when(orderItemRepository.findByOrderId(5L)).thenReturn(Flux.just(oi));

        StepVerifier.create(orderService.getOrder(5L))
                .assertNext(o -> {
                    assertThat(o.getId()).isEqualTo(5L);
                    assertThat(o.getTotalSum()).isEqualTo(7600L);
                    assertThat(o.getItems()).hasSize(1);
                    assertThat(o.getItems().getFirst().getCount()).isEqualTo(2);
                })
                .verifyComplete();
    }

    @Test
    void checkout_emptyCart_createsOrderWithZeroTotal() {
        Order savedOrder = Order.builder().id(11L).totalSum(0L).build();

        when(cartService.getAllCartItems()).thenReturn(Flux.empty());
        when(orderRepository.save(any())).thenReturn(Mono.just(savedOrder));
        when(orderItemRepository.saveAll(anyList())).thenReturn(Flux.empty());
        when(cartService.clear()).thenReturn(Mono.empty());

        StepVerifier.create(orderService.checkout())
                .expectNext(11L)
                .verifyComplete();

        verify(orderRepository).save(argThat(o -> o.getTotalSum() == 0L));
    }
}

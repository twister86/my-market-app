package ru.yandex.practicum.mymarket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.entity.Order;
import ru.yandex.practicum.mymarket.entity.OrderItem;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ItemRepository itemRepository;
    private final CartService cartService;

    /** Оформить заказ из корзины */
    public Mono<Long> checkout() {
        return cartService.getAllCartItems()
                .flatMap(ci -> itemRepository.findById(ci.getId())
                        .map(item -> OrderItem.builder()
                                .itemId(item.getId())
                                .title(item.getTitle())
                                .price(item.getPrice())
                                .count(ci.getCount())
                                .build()))
                .collectList()
                .flatMap(orderItems -> {
                    long total = orderItems.stream()
                            .mapToLong(oi -> oi.getPrice() * oi.getCount())
                            .sum();
                    Order order = Order.builder().totalSum(total).build();
                    return orderRepository.save(order)
                            .flatMap(savedOrder -> {
                                orderItems.forEach(oi -> oi.setOrderId(savedOrder.getId()));
                                return orderItemRepository.saveAll(orderItems)
                                        .then(cartService.clear())
                                        .thenReturn(savedOrder.getId());
                            });
                });
    }

    /** Все заказы с позициями */
    public Flux<Order> getAllOrders() {
        return orderRepository.findAll()
                .flatMap(this::enrichOrder);
    }

    /** Заказ по id с позициями */
    public Mono<Order> getOrder(Long id) {
        return orderRepository.findById(id)
                .flatMap(this::enrichOrder);
    }

    private Mono<Order> enrichOrder(Order order) {
        return orderItemRepository.findByOrderId(order.getId())
                .collectList()
                .map(items -> {
                    order.setItems(items);
                    return order;
                });
    }
}
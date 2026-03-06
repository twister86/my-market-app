package ru.yandex.practicum.mymarket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.entity.CartItem;
import ru.yandex.practicum.mymarket.entity.Order;
import ru.yandex.practicum.mymarket.entity.OrderItem;
import ru.yandex.practicum.mymarket.mapper.ItemMapper;
import ru.yandex.practicum.mymarket.mapper.OrderMapper;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @Transactional
    public Long checkout(String sessionId) {

        List<CartItem> cartItems = cartItemRepository.findBySessionId(sessionId);

        if (cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        Order order = new Order();
        order.setSessionId(sessionId);

        long total = 0;
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setItem(cartItem.getItem());
            orderItem.setCount(cartItem.getQuantity());
            orderItems.add(orderItem);

            total += cartItem.getItem().getPrice() * cartItem.getQuantity();
        }
        order.setItems(orderItems);
        order.setTotalSum(total);

        Order savedOrder = orderRepository.save(order);

        cartItemRepository.deleteBySessionId(sessionId);

        return savedOrder.getId();
    }

    public List<OrderDto> getOrders(String sessionId) {
        return orderRepository.findBySessionId(sessionId).stream()
                .map(orderMapper::toDto)
                .toList();
    }

    public OrderDto getOrderById(String sessionId, Long orderId) {

        return orderMapper.toDto(orderRepository
                .findByIdAndSessionId(orderId, sessionId)
                .orElseThrow(() -> new RuntimeException("Order not found")));
    }
}
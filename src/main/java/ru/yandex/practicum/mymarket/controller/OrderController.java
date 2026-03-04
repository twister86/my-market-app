package ru.yandex.practicum.mymarket.controller;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.mymarket.entity.Order;
import ru.yandex.practicum.mymarket.mapper.ItemMapper;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.OrderService;

@Controller
@RequestMapping("/orders")
@AllArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;
    private final ItemMapper itemMapper;

    @GetMapping
    public String orders(Model model) {
        model.addAttribute("orders", orderService.findAll());
        return "orders";
    }

    @GetMapping("/{id}")
    public String getOrder(@PathVariable Long id, Model model) {
        Order order = orderService.findById(id);
        model.addAttribute("order", order);
        return "order";
    }

    @PostMapping("/buy")
    public String buy() {
        Order order = Order.builder()
                .items(itemMapper.toEntityList(cartService.getCartItems()))
                .totalSum(cartService.getTotalPrice())
                .build();
        Order saved = orderService.save(order);
        cartService.clearCart();
        return "redirect:/orders/" + saved.getId();
    }
}
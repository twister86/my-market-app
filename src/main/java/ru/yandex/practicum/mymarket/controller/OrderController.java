package ru.yandex.practicum.mymarket.controller;

import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.mymarket.dto.OrderDto;
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
    public String orders(Model model, HttpSession session) {
        model.addAttribute("orders", orderService.getOrders(session.getId()));
        return "orders";
    }

    @GetMapping("/{id}")
    public String getOrder(@PathVariable Long id, Model model, HttpSession session) {
        OrderDto order = orderService.getOrderById(session.getId(), id);
        model.addAttribute("order", order);
        return "order";
    }

    @PostMapping("/buy")
    public String buy(HttpSession session) {
        return "redirect:/orders/" + orderService.checkout(session.getId());
    }
}
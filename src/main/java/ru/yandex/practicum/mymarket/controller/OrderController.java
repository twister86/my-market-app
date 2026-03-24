package ru.yandex.practicum.mymarket.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.utils.SessionUtils;
import ru.yandex.practicum.mymarket.service.OrderService;

@Controller
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/orders")
    public Mono<String> orders(ServerWebExchange exchange, Model model) {
        return SessionUtils.getOrCreateSessionId(exchange).flatMap(sessionId ->
                orderService.getAllOrders(sessionId)
                        .collectList()
                        .doOnNext(orders -> model.addAttribute("orders", orders))
                        .thenReturn("orders")
        );
    }

    @GetMapping("/orders/{id}")
    public Mono<String> order(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean newOrder,
            Model model) {

        return orderService.getOrder(id)
                .doOnNext(order -> {
                    model.addAttribute("order", order);
                    model.addAttribute("newOrder", newOrder);
                })
                .thenReturn("order");
    }

    @PostMapping("/orders/buy")
    public Mono<String> buy(ServerWebExchange exchange) {
        return SessionUtils.getOrCreateSessionId(exchange).flatMap(sessionId ->
                orderService.checkout(sessionId)
                        .map(id -> "redirect:/orders/" + id + "?newOrder=true")
        );
    }
}
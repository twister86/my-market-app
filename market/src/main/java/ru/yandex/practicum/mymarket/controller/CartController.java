package ru.yandex.practicum.mymarket.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.PaymentClientService;
import ru.yandex.practicum.mymarket.utils.SecurityUtils;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final PaymentClientService paymentClientService;

    @GetMapping("/items")
    public Mono<String> cart(Model model) {
        // username из SecurityContext — у каждого пользователя своя корзина
        return SecurityUtils.getCurrentUsername().flatMap(username ->
                Mono.zip(
                        cartService.getCartItems(username).collectList(),
                        cartService.getTotal(username)
                ).flatMap(tuple -> {
                    long total = tuple.getT2();
                    return paymentClientService.hasEnoughBalance(username, total)
                            .map(canPay -> {
                                model.addAttribute("items",  tuple.getT1());
                                model.addAttribute("total",  total);
                                model.addAttribute("canPay", canPay);
                                return "cart";
                            });
                })
        );
    }

    @PostMapping("/items")
    public Mono<String> updateCart(ServerWebExchange exchange, Model model) {
        return SecurityUtils.getCurrentUsername().flatMap(username ->
                exchange.getFormData().flatMap(form -> {
                    String idStr  = form.getFirst("id");
                    String action = form.getFirst("action");

                    Mono<Void> update = (idStr != null && action != null && !action.isBlank())
                            ? cartService.updateCart(Long.parseLong(idStr), action, username)
                            : Mono.empty();

                    return update
                            .then(Mono.zip(
                                    cartService.getCartItems(username).collectList(),
                                    cartService.getTotal(username)
                            ))
                            .flatMap(tuple -> {
                                long total = tuple.getT2();
                                return paymentClientService.hasEnoughBalance(username, total)
                                        .map(canPay -> {
                                            model.addAttribute("items",  tuple.getT1());
                                            model.addAttribute("total",  total);
                                            model.addAttribute("canPay", canPay);
                                            return "cart";
                                        });
                            });
                })
        );
    }
}
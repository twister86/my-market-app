package ru.yandex.practicum.mymarket.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.service.PaymentClientService;
import ru.yandex.practicum.mymarket.utils.SessionUtils;
import ru.yandex.practicum.mymarket.service.CartService;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final PaymentClientService paymentClientService;

    @GetMapping("/items")
    public Mono<String> cart(ServerWebExchange exchange, Model model) {
        return SessionUtils.getOrCreateSessionId(exchange).flatMap(sessionId ->
                Mono.zip(
                        cartService.getCartItems(sessionId).collectList(),
                        cartService.getTotal(sessionId)
                ).flatMap(tuple -> {
                    long total = tuple.getT2();
                    // Проверяем баланс — хватает ли средств для оплаты
                    return paymentClientService.hasEnoughBalance(sessionId, total)
                            .map(canPay -> {
                                model.addAttribute("items",  tuple.getT1());
                                model.addAttribute("total",  total);
                                model.addAttribute("canPay", canPay);
                                model.addAttribute("paymentError", null);
                                return "cart";
                            });
                })
        );
    }

    @PostMapping("/items")
    public Mono<String> updateCart(ServerWebExchange exchange, Model model) {
        return SessionUtils.getOrCreateSessionId(exchange).flatMap(sessionId ->
                exchange.getFormData().flatMap(form -> {
                    String idStr  = form.getFirst("id");
                    String action = form.getFirst("action");

                    Mono<Void> update = (idStr != null && action != null && !action.isBlank())
                            ? cartService.updateCart(Long.parseLong(idStr), action, sessionId)
                            : Mono.empty();

                    return update
                            .then(Mono.zip(
                                    cartService.getCartItems(sessionId).collectList(),
                                    cartService.getTotal(sessionId)
                            ))
                            .flatMap(tuple -> {
                                long total = tuple.getT2();
                                return paymentClientService.hasEnoughBalance(sessionId, total)
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

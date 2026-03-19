package ru.yandex.practicum.mymarket.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.service.CartService;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/items")
    public Mono<String> cart(Model model) {
        return Mono.zip(
                cartService.getCartItems().collectList(),
                cartService.getTotal()
        ).doOnNext(tuple -> {
            model.addAttribute("items", tuple.getT1());
            model.addAttribute("total", tuple.getT2());
        }).thenReturn("cart");
    }

    @PostMapping("/items")
    public Mono<String> updateCart(
            @RequestParam Long id,
            @RequestParam String action,
            Model model) {

        return cartService.updateCart(id, action)
                .then(Mono.zip(
                        cartService.getCartItems().collectList(),
                        cartService.getTotal()
                ))
                .doOnNext(tuple -> {
                    model.addAttribute("items", tuple.getT1());
                    model.addAttribute("total", tuple.getT2());
                })
                .thenReturn("cart");
    }
}

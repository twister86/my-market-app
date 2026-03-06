package ru.yandex.practicum.mymarket.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.entity.CartItem;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;

import java.util.List;


@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final ItemService itemService;

    @GetMapping("/items")
    public String cartItems(HttpSession session, Model model) {
        String sessionId = session.getId();
        List<ItemDto> cartItems = cartService.getCart(sessionId);

        model.addAttribute("items",
                cartItems);
        model.addAttribute("total", cartItems.stream()
                .mapToLong(ci -> ci.getPrice() * ci.getCount())
                .sum());
        return "cart";
    }

    @PostMapping("/items")
    public String updateCart(@RequestParam Long id, @RequestParam String action, HttpSession session) {
        String sessionId = session.getId();
        if (id != null) cartService.updateQuantity(sessionId, id, action);
        return "redirect:/cart/items";
    }
}
package ru.yandex.practicum.mymarket.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.entity.Item;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final ItemService itemService;

    @GetMapping("/items")
    public String cartItems(Model model) {
        model.addAttribute("items", cartService.getCartItems());
        model.addAttribute("total", cartService.getTotalPrice());
        return "cart";
    }

    @PostMapping("/items")
    public String updateCart(@RequestParam Long id, @RequestParam String action) {
        Item item = itemService.findById(id);
        if (item != null) cartService.updateCount(item, action);
        return "redirect:/cart/items";
    }
}
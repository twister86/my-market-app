package ru.yandex.practicum.mymarket.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.ItemForm;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;

@Controller
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private final CartService cartService;

    // ===== Витрина =====
    @GetMapping({"/", "/items"})
    public Mono<String> items(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "NO") String sort,
            @RequestParam(defaultValue = "1") int pageNumber,
            @RequestParam(defaultValue = "5") int pageSize,
            Model model) {

        return Mono.zip(
                itemService.getItemsPage(search, sort, pageNumber, pageSize),
                itemService.buildPaging(search, sort, pageNumber, pageSize)
        ).doOnNext(tuple -> {
            model.addAttribute("items", tuple.getT1());
            model.addAttribute("paging", tuple.getT2());
            model.addAttribute("search", search);
            model.addAttribute("sort", sort);
        }).thenReturn("items");
    }

    @PostMapping("/items")
    public Mono<String> updateCartFromItems(@ModelAttribute ItemForm form) {
        Long id = form.getId();
        String action = form.getAction();
        String search = form.getSearch();
        String sort = form.getSort();
        int pageNumber = form.getPageNumber();
        int pageSize = form.getPageSize();

        if (id == null || action == null || action.isBlank()) {
            // Редирект на items без изменений
            return Mono.just("redirect:/items?search=" + search
                    + "&sort=" + sort
                    + "&pageNumber=" + pageNumber
                    + "&pageSize=" + pageSize);
        }

        return cartService.updateCart(id, action)
                .thenReturn("redirect:/items?search=" + search
                        + "&sort=" + sort
                        + "&pageNumber=" + pageNumber
                        + "&pageSize=" + pageSize);
    }

    // ===== Карточка товара =====

    @GetMapping("/items/{id}")
    public Mono<String> item(@PathVariable Long id, Model model) {
        return itemService.getItem(id)
                .doOnNext(item -> model.addAttribute("item", item))
                .thenReturn("item");
    }

    @PostMapping("/items/{id}")
    public Mono<String> updateCartFromItem(
            @PathVariable Long id,
            ServerWebExchange exchange,
            Model model) {

        return exchange.getFormData().flatMap(form -> {
            String action = form.getFirst("action");
            Mono<Void> update = (action != null && !action.isBlank())
                    ? cartService.updateCart(id, action)
                    : Mono.empty();
            return update
                    .then(itemService.getItem(id))
                    .doOnNext(item -> model.addAttribute("item", item))
                    .thenReturn("item");
        });
    }
}
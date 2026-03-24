package ru.yandex.practicum.mymarket.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.utils.SessionUtils;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private final CartService cartService;

    @GetMapping({"/", "/items"})
    public Mono<String> items(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "NO") String sort,
            @RequestParam(defaultValue = "1") int pageNumber,
            @RequestParam(defaultValue = "5") int pageSize,
            ServerWebExchange exchange,
            Model model) {

        return SessionUtils.getOrCreateSessionId(exchange).flatMap(sessionId ->
                Mono.zip(
                        itemService.getItemsPage(search, sort, pageNumber, pageSize, sessionId),
                        itemService.buildPaging(search, sort, pageNumber, pageSize)
                ).doOnNext(tuple -> {
                    model.addAttribute("items", tuple.getT1());
                    model.addAttribute("paging", tuple.getT2());
                    model.addAttribute("search", search);
                    model.addAttribute("sort", sort);
                }).thenReturn("items")
        );
    }

    @PostMapping("/items")
    public Mono<String> updateCartFromItems(ServerWebExchange exchange) {
        return SessionUtils.getOrCreateSessionId(exchange).flatMap(sessionId ->
                exchange.getFormData().flatMap(form -> {
                    String idStr      = form.getFirst("id");
                    String action     = form.getFirst("action");
                    String search     = form.getOrDefault("search",     List.of("")).get(0);
                    String sort       = form.getOrDefault("sort",       List.of("NO")).get(0);
                    String pageNumber = form.getOrDefault("pageNumber", List.of("1")).get(0);
                    String pageSize   = form.getOrDefault("pageSize",   List.of("5")).get(0);

                    Mono<Void> update = (idStr != null && action != null && !action.isBlank())
                            ? cartService.updateCart(Long.parseLong(idStr), action, sessionId)
                            : Mono.empty();

                    return update.thenReturn("redirect:/items?search=" + search
                            + "&sort=" + sort
                            + "&pageNumber=" + pageNumber
                            + "&pageSize=" + pageSize);
                })
        );
    }

    @GetMapping("/items/{id}")
    public Mono<String> item(@PathVariable Long id, ServerWebExchange exchange, Model model) {
        return SessionUtils.getOrCreateSessionId(exchange).flatMap(sessionId ->
                itemService.getItem(id, sessionId)
                        .doOnNext(item -> model.addAttribute("item", item))
                        .thenReturn("item")
        );
    }

    @PostMapping("/items/{id}")
    public Mono<String> updateCartFromItem(
            @PathVariable Long id,
            ServerWebExchange exchange,
            Model model) {

        return SessionUtils.getOrCreateSessionId(exchange).flatMap(sessionId ->
                exchange.getFormData().flatMap(form -> {
                    String action = form.getFirst("action");
                    Mono<Void> update = (action != null && !action.isBlank())
                            ? cartService.updateCart(id, action, sessionId)
                            : Mono.empty();
                    return update
                            .then(itemService.getItem(id, sessionId))
                            .doOnNext(item -> model.addAttribute("item", item))
                            .thenReturn("item");
                })
        );
    }
}
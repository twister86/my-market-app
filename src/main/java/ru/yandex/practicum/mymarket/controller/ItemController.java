package ru.yandex.practicum.mymarket.controller;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.mymarket.config.Paging;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.entity.Item;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/items")
@AllArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private final CartService cartService;

    @GetMapping
    public String getItems(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "NO") String sort,
            @RequestParam(required = false, defaultValue = "1") int pageNumber,
            @RequestParam(required = false, defaultValue = "5") int pageSize,
            Model model) {

        Page<ItemDto> itemDtoPage = itemService.findAll(search, sort, pageNumber, pageSize);
        List<ItemDto> items = itemDtoPage.getContent();

        List<List<ItemDto>> groupedItems = new ArrayList<>();
        for (int i = 0; i < items.size(); i += 3) {
            List<ItemDto> row = items.subList(i, Math.min(i + 3, items.size()));
            groupedItems.add(row);
        }
        model.addAttribute("items", groupedItems);
        model.addAttribute("search", search);
        model.addAttribute("sort", sort);
        model.addAttribute("paging", new Paging(pageSize, pageNumber, itemDtoPage.hasPrevious(), itemDtoPage.hasNext()));

        return "items";
    }

    @GetMapping("/{id}")
    public String getItem(@PathVariable Long id, Model model) {
        ItemDto item = itemService.findById(id);
        model.addAttribute("item", item);
        return "item";
    }

    @PostMapping("/{id}")
    public String updateItemCount(@PathVariable Long id, @RequestParam String action) {
        Item item = itemService.findItemById(id);
        if (item != null) cartService.updateCount(item, action);
        return "redirect:/items/" + id;
    }

    @PostMapping
    public String updateCart(
            @RequestParam Long id,
            @RequestParam String action,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "NO") String sort,
            @RequestParam(required = false, defaultValue = "1") int pageNumber,
            @RequestParam(required = false, defaultValue = "5") int pageSize
            ) {
        if ("PLUS".equals(action)) {
            cartService.addToCart(id, 1);
        } else if ("MINUS".equals(action)) {
            cartService.updateCount(itemService.findItemById(id), action);
        }

        return "redirect:/items?search=" + (search != null ? search : "") +
                "&sort=" + sort +
                "&pageNumber=" + pageNumber +
                "&pageSize=" + pageSize;
    }


}
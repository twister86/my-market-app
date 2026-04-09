package ru.yandex.practicum.mymarket.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.entity.Item;
import ru.yandex.practicum.mymarket.service.ImageService;
import ru.yandex.practicum.mymarket.service.ItemService;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ItemService itemService;
    private final ImageService imageService;

    @GetMapping("/items/add")
    public String addItemForm(Model model) {
        model.addAttribute("item", new Item());
        return "add-item";
    }

    /**
     * Добавить товар с опциональной загрузкой изображения.
     * Поле image — multipart-файл (необязательное).
     */
    @PostMapping(value = "/items/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<String> addItem(
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam long price,
            @RequestPart(value = "image", required = false) FilePart imagePart) {

        return resolveImagePath(imagePart)
                .flatMap(imgPath -> {
                    Item item = Item.builder()
                            .title(title)
                            .description(description)
                            .price(price)
                            .imgPath(imgPath)
                            .build();
                    return itemService.save(item);
                })
                .thenReturn("redirect:/items");
    }

    /**
     * Если файл передан и не пустой — сохраняет и возвращает путь.
     * Иначе возвращает путь к дефолтному изображению.
     */
    private Mono<String> resolveImagePath(FilePart imagePart) {
        if (imagePart == null || imagePart.filename().isBlank()) {
            return Mono.just("/images/default.jpg");
        }

        String filename = sanitizeFilename(imagePart.filename());

        return imagePart.content()
                .reduce(new byte[0], (acc, dataBuffer) -> {
                    byte[] chunk = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(chunk);
                    byte[] merged = new byte[acc.length + chunk.length];
                    System.arraycopy(acc, 0, merged, 0, acc.length);
                    System.arraycopy(chunk, 0, merged, acc.length, chunk.length);
                    return merged;
                })
                .flatMap(bytes -> {
                    if (bytes.length == 0) {
                        return Mono.just("/images/default.jpg");
                    }
                    return imageService.saveImage(filename, bytes)
                            .thenReturn("/images/" + filename);
                });
    }

    /** Убирает path-traversal символы из имени файла */
    private String sanitizeFilename(String original) {
        return original.replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }
}

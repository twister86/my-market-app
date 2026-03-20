package ru.yandex.practicum.mymarket.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.service.ImageService;

@Controller
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    /**
     * GET /images/{filename}
     * Отдаёт изображение по имени файла.
     * Пример: GET /images/ball.jpg
     */
    @GetMapping("/images/{filename}")
    public Mono<ResponseEntity<byte[]>> getImage(@PathVariable String filename) {
        // Защита от path traversal
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        String mediaType = imageService.detectMediaType(filename);

        return imageService.loadImage(filename)
                .filter(bytes -> bytes != null && bytes.length > 0)
                .map(bytes -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(mediaType))
                        .body(bytes))
                .switchIfEmpty(Mono.just(ResponseEntity.<byte[]>notFound().build()));
    }
}

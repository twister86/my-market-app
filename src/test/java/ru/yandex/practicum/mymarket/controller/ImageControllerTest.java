package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.service.ImageService;

import static org.mockito.Mockito.when;

@WebFluxTest(ImageController.class)
class ImageControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ImageService imageService;

    @Test
    void getImage_existingJpeg_returns200() {
        byte[] fakeBytes = {(byte) 0xFF, (byte) 0xD8, 0x01, 0x02};
        when(imageService.loadImage("1.jpg")).thenReturn(Mono.just(fakeBytes));
        when(imageService.detectMediaType("1.jpg")).thenReturn("image/jpeg");

        webTestClient.get().uri("/images/1.jpg")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.IMAGE_JPEG)
                .expectBody(byte[].class).isEqualTo(fakeBytes);
    }

    @Test
    void getImage_existingPng_returns200WithPngType() {
        byte[] fakeBytes = {(byte) 0x89, 0x50, 0x4E, 0x47};
        when(imageService.loadImage("logo.png")).thenReturn(Mono.just(fakeBytes));
        when(imageService.detectMediaType("logo.png")).thenReturn("image/png");

        webTestClient.get().uri("/images/logo.png")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.IMAGE_PNG);
    }

    @Test
    void getImage_notFound_returns404() {
        when(imageService.loadImage("missing.jpg")).thenReturn(Mono.justOrEmpty(null));
        when(imageService.detectMediaType("missing.jpg")).thenReturn("image/jpeg");

        webTestClient.get().uri("/images/missing.jpg")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getImage_pathTraversal_returns400() {
        webTestClient.get().uri("/images/..%2Fsecret.txt")
                .exchange()
                .expectStatus().isBadRequest();
    }
}

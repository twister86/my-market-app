package ru.yandex.practicum.mymarket.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ImageService {

    private static final Path UPLOAD_DIR = Paths.get("uploads/images");
    /** Директория встроенных изображений в classpath */
    private static final String CLASSPATH_IMAGES = "static/";

    /**
     * Загружает изображение по имени файла.
     * Сначала ищет в папке uploads/images (runtime),
     * затем в classpath:/static/images/,
     * при отсутствии — возвращает default.jpg из classpath.
     *
     * @param filename имя файла, например "ball.jpg"
     * @return байты изображения
     */
    public Mono<byte[]> loadImage(String filename) {
        return Mono.fromCallable(() -> {
            ClassPathResource classpathResource = new ClassPathResource(CLASSPATH_IMAGES + filename);
            if (classpathResource.exists()) {
                return classpathResource.getInputStream().readAllBytes();
            }
            ClassPathResource defaultResource = new ClassPathResource(CLASSPATH_IMAGES + "1.jpg");
            if (defaultResource.exists()) {
                return defaultResource.getInputStream().readAllBytes();
            }
            return null;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Определяет MIME-тип по расширению файла.
     */
    public String detectMediaType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png"))  return "image/png";
        if (lower.endsWith(".gif"))  return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".svg"))  return "image/svg+xml";
        if (lower.endsWith(".bmp"))  return "image/bmp";
        return "image/jpeg"; // jpg / jpeg — по умолчанию
    }

    /**
     * Сохраняет байты изображения в uploads/images/<filename>.
     * Создаёт директорию, если её нет.
     */
    public Mono<Void> saveImage(String filename, byte[] data) {
        return Mono.fromCallable(() -> {
            Files.createDirectories(UPLOAD_DIR);
            Files.write(UPLOAD_DIR.resolve(filename), data);
            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
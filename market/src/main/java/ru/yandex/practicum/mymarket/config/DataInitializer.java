package ru.yandex.practicum.mymarket.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.entity.Item;
import ru.yandex.practicum.mymarket.entity.User;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.repository.UserRepository;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Первоначальное заполнение таблицы items при старте приложения.
 *
 * Сканирует каталог resources/static/images/ и для каждого файла вида
 * {id}.jpg создаёт запись в таблице items, если таблица пуста.
 *
 * Соответствие: файл 1.jpg → item с id=1, imgPath="/images/1.jpg"
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer {

    private static final Pattern IMAGE_PATTERN = Pattern.compile("^(\\d+)\\.jpg$", Pattern.CASE_INSENSITIVE);
    private static final String IMAGES_LOCATION = "classpath:static/*.jpg";
    private static final java.util.Random RANDOM = new java.util.Random();


    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final R2dbcEntityTemplate template;
    private final PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        itemRepository.count()
                .flatMap(count -> {
                    if (count > 0) {
                        log.info("DataInitializer: таблица items уже содержит {} записей, пропускаем.", count);
                        return Mono.empty();
                    }
                    log.info("DataInitializer: таблица items пуста, начинаем заполнение...");
                    return populateFromImages();
                })
                .subscribe(
                        null,
                        err -> log.error("DataInitializer: ошибка при заполнении таблицы items", err)
                );
    }

    private Mono<Void> populateFromImages() {
        List<Item> items = scanImages();

        if (items.isEmpty()) {
            log.warn("DataInitializer: изображения не найдены в {}", IMAGES_LOCATION);
            return Mono.empty();
        }

        return Flux.fromIterable(items)
                // insert() всегда выполняет INSERT, игнорируя наличие @Id
                .concatMap(item -> template.insert(Item.class).using(item))
                .doOnNext(saved -> log.info("DataInitializer: сохранён товар id={}, imgPath={}",
                        saved.getId(), saved.getImgPath()))
                .then()
                .doOnSuccess(v -> log.info("DataInitializer: заполнение завершено, добавлено {} товаров.", items.size()));
    }


    private long randomPrice() {
        // Цена кратна 100, в диапазоне 1000–10000
        return (RANDOM.nextInt(91) + 10) * 100L;
    }

    private List<Item> scanImages() {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources(IMAGES_LOCATION);
            return Arrays.stream(resources)
                    .map(Resource::getFilename)
                    .filter(Objects::nonNull)
                    .map(filename -> {
                        Matcher m = IMAGE_PATTERN.matcher(filename);
                        if (!m.matches()) return null;
                        long id = Long.parseLong(m.group(1));
                        return Item.builder()
                                .id(id)
                                .title("Товар " + id)
                                .description("Описание товара " + id)
                                .imgPath("/images/" + filename)
                                .price(randomPrice())
                                .build();
                    })
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparingLong(Item::getId))
                    .toList();
        } catch (Exception e) {
            log.error("DataInitializer: не удалось просканировать {}", IMAGES_LOCATION, e);
            return List.of();
        }
    }

    // ===== Пользователи =====

    private Mono<Void> initUsers() {
        return userRepository.count()
                .flatMap(count -> {
                    if (count > 0) {
                        log.info("DataInitializer: таблица users уже содержит {} записей, пропускаем.", count);
                        return Mono.empty();
                    }
                    log.info("DataInitializer: создаём пользователей по умолчанию...");
                    return populateDefaultUsers();
                });
    }

    private Mono<Void> populateDefaultUsers() {
        List<User> users = List.of(
                User.builder()
                        .username("user")
                        .password(passwordEncoder.encode("user123"))
                        .role("ROLE_USER")
                        .build(),
                User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin123"))
                        .role("ROLE_ADMIN")
                        .build()
        );

        return Flux.fromIterable(users)
                .concatMap(user -> userRepository.save(user))
                .doOnNext(saved -> log.info("DataInitializer: создан пользователь username={}",
                        saved.getUsername()))
                .then()
                .doOnSuccess(v -> log.info("DataInitializer: пользователи по умолчанию созданы."));
    }
}
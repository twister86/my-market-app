package ru.yandex.practicum.mymarket.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.mymarket.entity.Item;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.util.Random;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ItemRepository itemRepository;

    @Override
    public void run(String... args) throws Exception {
        // Проверяем, чтобы не дублировать данные при перезапуске
        if (itemRepository.count() == 0) {
            for (int i = 1; i <= 15; i++) {
                Item item = new Item();
                item.setTitle("Item " + i);
                item.setDescription("Описание товара " + i);
                item.setImgPath("/images/item" + i + ".jpg");
                item.setPrice((new Random(12345L)).nextLong()); // пример цены
                item.setCount(0); // по умолчанию в корзине нет
                itemRepository.save(item);
            }
            System.out.println("✅ 15 items initialized in database");
        }
    }
}
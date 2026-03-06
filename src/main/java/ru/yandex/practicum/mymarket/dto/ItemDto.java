package ru.yandex.practicum.mymarket.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Data
@RequiredArgsConstructor
@Accessors(chain = true)
public class ItemDto {
    private Long id;
    private String title;
    private String description;
    private String imgPath;
    private Long price;
    private int count;
}
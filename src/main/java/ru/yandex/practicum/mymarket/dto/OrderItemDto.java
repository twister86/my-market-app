package ru.yandex.practicum.mymarket.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Data
@RequiredArgsConstructor
@Accessors(chain = true)
public class OrderItemDto {
    private Long id;
    private String title;
    private Long price;
    private Integer count;
}

package ru.yandex.practicum.mymarket.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@RequiredArgsConstructor
@Accessors(chain = true)
public class OrderDto {
    private Long id;
    private List<OrderItemDto> items;
    private Long totalSum;
}
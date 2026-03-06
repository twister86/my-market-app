package ru.yandex.practicum.mymarket.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.OrderItemDto;
import ru.yandex.practicum.mymarket.entity.Item;
import ru.yandex.practicum.mymarket.entity.OrderItem;
import ru.yandex.practicum.mymarket.service.CartService;

import java.util.List;

@Mapper(componentModel = "spring")
public abstract class OrderItemMapper {

    @Autowired
    protected CartService cartService;

    @Mapping(target = "price", expression = "java(item.getItem().getPrice())")
    @Mapping(target = "title", expression = "java(item.getItem().getTitle())")
    public abstract OrderItemDto toDto(OrderItem item);



}

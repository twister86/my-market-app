package ru.yandex.practicum.mymarket.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.entity.Item;
import ru.yandex.practicum.mymarket.entity.Order;
import ru.yandex.practicum.mymarket.service.CartService;

import java.util.List;

@Mapper(componentModel = "spring",
        uses = {OrderItemMapper.class}
)
public abstract class OrderMapper {

    public abstract OrderDto toDto(Order order);

    public abstract Order toEntity(OrderDto orderDto);


}

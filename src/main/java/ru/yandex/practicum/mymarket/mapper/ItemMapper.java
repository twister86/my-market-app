package ru.yandex.practicum.mymarket.mapper;

import org.mapstruct.Mapper;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.entity.Item;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ItemMapper {

    ItemDto toDto(Item item);

    Item toEntity(ItemDto itemDto);

    List<Item> toEntityList(List<ItemDto> itemDto);

}

package ru.yandex.practicum.mymarket.mapper;

import org.mapstruct.Mapper;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.entity.Item;

import java.util.List;

@Mapper(componentModel = "spring")
public abstract class ItemMapper {

    public abstract ItemDto toDto(Item item);

    public abstract Item toEntity(ItemDto itemDto);

    public abstract List<Item> toEntityList(List<ItemDto> itemDto);

}

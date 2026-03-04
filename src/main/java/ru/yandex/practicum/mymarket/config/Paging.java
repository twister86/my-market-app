package ru.yandex.practicum.mymarket.config;
import lombok.Data;

@Data
public class Paging {
    private final int pageSize;
    private final int pageNumber;
    private final boolean hasPrevious;
    private final boolean hasNext;
}
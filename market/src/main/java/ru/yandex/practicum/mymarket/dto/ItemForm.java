package ru.yandex.practicum.mymarket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ItemForm {
    private Long id;
    private String action;
    private String search;
    private String sort;
    private int pageNumber = 1;
    private int pageSize = 5;
}

package ru.yandex.practicum.mymarket.dto;

public enum SortType {
    NO,
    ALPHA,
    PRICE;

    public static SortType fromString(String value) {
        if (value == null || value.isBlank()) {
            return NO;
        }
        try {
            return SortType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NO;
        }
    }
}

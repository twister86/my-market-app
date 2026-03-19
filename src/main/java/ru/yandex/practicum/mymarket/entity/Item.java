package ru.yandex.practicum.mymarket.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("items")
public class Item {

    @Id
    private Long id;

    private String title;

    private String description;

    private String imgPath;

    private long price;

    @Transient
    private int count;

    public boolean isStub() {
        return id != null && id == -1L;
    }
}

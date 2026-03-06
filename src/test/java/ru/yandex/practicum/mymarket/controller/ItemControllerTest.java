package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ItemControllerTest {

    private MockMvc mockMvc;
    @Autowired
    private ItemService itemService;
    @Autowired
    private CartService cartService;

    @BeforeEach
    void setup() {
        itemService = Mockito.mock(ItemService.class);
        cartService = Mockito.mock(CartService.class);
        ItemController controller = new ItemController(itemService, cartService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setSingleView(new org.springframework.web.servlet.view.InternalResourceView("/items.html"))
                .build();
    }

    @Test
    void testItemsPage() throws Exception {

        ItemDto itemDto = new ItemDto()
                .setId(1L);

        when(itemService.findAll(any(), any(), anyInt(), anyInt(), any())).thenReturn(new PageImpl<>(List.of(itemDto)));

        mockMvc.perform(get("/items"))
                .andExpect(status().isOk());
    }
}
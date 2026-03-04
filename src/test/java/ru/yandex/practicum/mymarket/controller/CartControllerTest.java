package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CartControllerTest {

    private MockMvc mockMvc;
    @Autowired
    private ItemService itemService;
    @Autowired
    private CartService cartService;

    @BeforeEach
    void setup() {
        cartService = Mockito.mock(CartService.class);
        CartController controller = new CartController(cartService, itemService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setSingleView(new org.springframework.web.servlet.view.InternalResourceView("/cart.html"))
                .build();
    }

    @Test
    void testCartPage() throws Exception {
        mockMvc.perform(get("/cart/items"))
                .andExpect(status().isOk());
    }
}
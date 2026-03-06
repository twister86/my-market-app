package ru.yandex.practicum.mymarket.controller;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.yandex.practicum.mymarket.mapper.ItemMapper;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.OrderService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerTest {

    private MockMvc mockMvc;
    private OrderService orderService;
    private CartService cartService;
    private ItemMapper itemMapper;

    @BeforeEach
    void setup() {
        orderService = Mockito.mock(OrderService.class);
        cartService = Mockito.mock(CartService.class);
        OrderController controller = new OrderController(orderService, cartService, itemMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setSingleView(new org.springframework.web.servlet.view.InternalResourceView("/orders.html"))
                .build();
    }

    @Test
    void testOrdersPage() throws Exception {
        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk());
    }
}
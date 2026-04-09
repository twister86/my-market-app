package ru.yandex.practicum.mymarket.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import ru.yandex.practicum.market.payment.ApiClient;
import ru.yandex.practicum.market.payment.api.PaymentApi;

@Configuration
public class PaymentClientConfig {

    @Value("${payment.service.url:http://localhost:8081}")
    private String paymentServiceUrl;

    @Bean
    public ApiClient paymentApiClient() {
        ApiClient client = new ApiClient(
                WebClient.builder().baseUrl(paymentServiceUrl).build()
        );
        client.setBasePath(paymentServiceUrl);
        return client;
    }

    @Bean
    public PaymentApi paymentApi(ApiClient apiClient) {
        return new PaymentApi(apiClient);
    }
}

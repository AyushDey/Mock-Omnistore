package com.omnistore.mockui.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configures a WebClient bean for calling the Quotation API.
 * The base URL is configurable via application.properties.
 */
@Configuration
public class QuotationClientConfig {

    @Value("${quotation.api.base-url:http://localhost:8081/api/quotation}")
    private String quotationApiBaseUrl;

    @Bean
    public WebClient quotationWebClient() {
        return WebClient.builder()
                .baseUrl(quotationApiBaseUrl)
                .build();
    }
}

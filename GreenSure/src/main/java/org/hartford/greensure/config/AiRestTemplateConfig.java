package org.hartford.greensure.config;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Dedicated {@link RestTemplate} for outbound AI HTTP calls with bounded
 * connect, connection-pool, and response timeouts so a slow or stuck provider
 * cannot block servlet threads indefinitely.
 */
@Configuration
public class AiRestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(
            @Value("${ai.request.timeout.ms:15000}") int aiRequestTimeoutMs) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setResponseTimeout(Timeout.ofMilliseconds(aiRequestTimeoutMs))
                .build();
        HttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setConnectTimeout(10_000);
        factory.setConnectionRequestTimeout(15_000);
        return new RestTemplate(factory);
    }
}

package com.secure.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Configuration des filtres globaux pour le logging et la traçabilité.
 */
@Configuration
@Slf4j
public class GatewayConfig {

    /**
     * Filtre global pour le logging des requêtes.
     */
    @Bean
    @Order(-1)
    public GlobalFilter loggingFilter() {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String requestId = UUID.randomUUID().toString().substring(0, 8);

            long startTime = System.currentTimeMillis();

            log.info("[{}] Requête entrante: {} {} depuis {}",
                    requestId,
                    request.getMethod(),
                    request.getURI().getPath(),
                    request.getRemoteAddress());

            return chain.filter(exchange)
                    .then(Mono.fromRunnable(() -> {
                        long duration = System.currentTimeMillis() - startTime;
                        log.info("[{}] Réponse: {} - Durée: {}ms",
                                requestId,
                                exchange.getResponse().getStatusCode(),
                                duration);
                    }));
        };
    }

    /**
     * Filtre pour ajouter des en-têtes de sécurité.
     */
    @Bean
    @Order(0)
    public GlobalFilter securityHeadersFilter() {
        return (exchange, chain) -> {
            exchange.getResponse().getHeaders().add("X-Content-Type-Options", "nosniff");
            exchange.getResponse().getHeaders().add("X-Frame-Options", "DENY");
            exchange.getResponse().getHeaders().add("X-XSS-Protection", "1; mode=block");
            return chain.filter(exchange);
        };
    }
}

package com.secure.order.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

/**
 * Client REST pour communiquer avec le service Produit.
 * Utilise WebClient pour les appels non-bloquants et propage le token JWT.
 */
@Component
@Slf4j
public class ProductServiceClient {

    private final WebClient webClient;

    public ProductServiceClient(WebClient.Builder webClientBuilder,
            @Value("${product-service.url:http://localhost:8081}") String productServiceUrl) {
        this.webClient = webClientBuilder
                .baseUrl(productServiceUrl)
                .build();
    }

    /**
     * Récupère un produit par son ID.
     * Le token JWT est propagé dans l'en-tête Authorization.
     */
    @CircuitBreaker(name = "productService", fallbackMethod = "getProductFallback")
    public Optional<ProductDTO> getProduct(Long productId, String jwtToken) {
        log.info("Appel au service Produit pour le produit ID: {}", productId);

        try {
            ProductDTO product = webClient.get()
                    .uri("/api/products/{id}", productId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response -> {
                        log.warn("Produit non trouvé: {}", productId);
                        return Mono.empty();
                    })
                    .bodyToMono(ProductDTO.class)
                    .block();

            return Optional.ofNullable(product);
        } catch (Exception e) {
            log.error("Erreur lors de l'appel au service Produit: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Vérifie la disponibilité du stock d'un produit.
     */
    @CircuitBreaker(name = "productService", fallbackMethod = "checkStockFallback")
    public boolean checkStockAvailability(Long productId, Integer quantity, String jwtToken) {
        log.info("Vérification du stock pour le produit {} - quantité: {}", productId, quantity);

        try {
            Map<String, Boolean> response = webClient.get()
                    .uri("/api/products/{id}/stock/check?quantity={quantity}", productId, quantity)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return response != null && Boolean.TRUE.equals(response.get("available"));
        } catch (Exception e) {
            log.error("Erreur lors de la vérification du stock: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Décrémente le stock d'un produit après validation de la commande.
     */
    @CircuitBreaker(name = "productService", fallbackMethod = "decrementStockFallback")
    public boolean decrementStock(Long productId, Integer quantity, String jwtToken) {
        log.info("Décrémentation du stock pour le produit {} - quantité: {}", productId, quantity);

        try {
            webClient.put()
                    .uri("/api/products/{id}/stock/decrement", productId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .bodyValue(Map.of("quantity", quantity))
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            return true;
        } catch (Exception e) {
            log.error("Erreur lors de la décrémentation du stock: {}", e.getMessage());
            return false;
        }
    }

    // Fallback methods for Circuit Breaker

    private Optional<ProductDTO> getProductFallback(Long productId, String jwtToken, Throwable t) {
        log.warn("Fallback activé pour getProduct - productId: {}, erreur: {}", productId, t.getMessage());
        return Optional.empty();
    }

    private boolean checkStockFallback(Long productId, Integer quantity, String jwtToken, Throwable t) {
        log.warn("Fallback activé pour checkStock - productId: {}, erreur: {}", productId, t.getMessage());
        return false;
    }

    private boolean decrementStockFallback(Long productId, Integer quantity, String jwtToken, Throwable t) {
        log.warn("Fallback activé pour decrementStock - productId: {}, erreur: {}", productId, t.getMessage());
        return false;
    }
}

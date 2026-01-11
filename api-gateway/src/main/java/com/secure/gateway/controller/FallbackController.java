package com.secure.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Fallback Controller for Circuit Breaker
 * Handles requests when downstream services are unavailable
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/product")
    public Mono<ResponseEntity<Map<String, Object>>> productFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SERVICE_UNAVAILABLE");
        response.put("message", "Le service Produit est temporairement indisponible. Veuillez réessayer plus tard.");
        response.put("service", "product-service");
        response.put("timestamp", System.currentTimeMillis());

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    @GetMapping("/order")
    public Mono<ResponseEntity<Map<String, Object>>> orderFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SERVICE_UNAVAILABLE");
        response.put("message", "Le service Commande est temporairement indisponible. Veuillez réessayer plus tard.");
        response.put("service", "order-service");
        response.put("timestamp", System.currentTimeMillis());

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }
}

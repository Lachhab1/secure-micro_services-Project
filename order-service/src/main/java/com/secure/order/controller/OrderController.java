package com.secure.order.controller;

import com.secure.order.entity.Order;
import com.secure.order.entity.OrderStatus;
import com.secure.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Contrôleur REST pour la gestion des commandes.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Orders", description = "API de gestion des commandes")
@SecurityRequirement(name = "bearer-jwt")
public class OrderController {

    private final OrderService orderService;

    /**
     * Crée une nouvelle commande.
     * Réservé au rôle CLIENT.
     */
    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Créer une commande", description = "Crée une nouvelle commande (CLIENT uniquement)")
    public ResponseEntity<Order> createOrder(@Valid @RequestBody Order order,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        String username = jwt.getClaimAsString("preferred_username");
        String token = jwt.getTokenValue();

        log.info("POST /api/orders - Création par: {}", username);

        Order createdOrder = orderService.createOrder(order, userId, username, token);
        return new ResponseEntity<>(createdOrder, HttpStatus.CREATED);
    }

    /**
     * Récupère les commandes de l'utilisateur connecté.
     * Réservé au rôle CLIENT.
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Mes commandes", description = "Récupère les commandes de l'utilisateur connecté")
    public ResponseEntity<List<Order>> getMyOrders(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.info("GET /api/orders/my - Utilisateur: {}", jwt.getClaimAsString("preferred_username"));

        List<Order> orders = orderService.getOrdersByUserId(userId);
        return ResponseEntity.ok(orders);
    }

    /**
     * Récupère toutes les commandes.
     * Réservé au rôle ADMIN.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lister toutes les commandes", description = "Récupère toutes les commandes (ADMIN uniquement)")
    public ResponseEntity<List<Order>> getAllOrders(@AuthenticationPrincipal Jwt jwt) {
        log.info("GET /api/orders - Utilisateur ADMIN: {}", jwt.getClaimAsString("preferred_username"));

        List<Order> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    /**
     * Récupère une commande par son ID.
     * ADMIN peut voir toutes les commandes, CLIENT uniquement les siennes.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT')")
    @Operation(summary = "Obtenir une commande", description = "Récupère les détails d'une commande")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        log.info("GET /api/orders/{} - Utilisateur: {}", id, jwt.getClaimAsString("preferred_username"));

        Order order = orderService.getOrderById(id);

        // Vérifier que le CLIENT ne peut voir que ses propres commandes
        boolean isAdmin = jwt.getClaimAsStringList("realm_access") != null;
        if (!isAdmin && !order.getUserId().equals(jwt.getSubject())) {
            log.warn("Tentative d'accès non autorisé à la commande {} par {}", id, jwt.getSubject());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(order);
    }

    /**
     * Met à jour le statut d'une commande.
     * Réservé au rôle ADMIN.
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mettre à jour le statut", description = "Change le statut d'une commande (ADMIN uniquement)")
    public ResponseEntity<Order> updateOrderStatus(@PathVariable Long id,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("PATCH /api/orders/{}/status - Par: {}", id, jwt.getClaimAsString("preferred_username"));

        OrderStatus newStatus = OrderStatus.valueOf(request.get("status"));
        Order updatedOrder = orderService.updateOrderStatus(id, newStatus);
        return ResponseEntity.ok(updatedOrder);
    }

    /**
     * Annule une commande.
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT')")
    @Operation(summary = "Annuler une commande", description = "Annule une commande")
    public ResponseEntity<Order> cancelOrder(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        log.info("POST /api/orders/{}/cancel - Par: {}", id, jwt.getClaimAsString("preferred_username"));

        Order cancelledOrder = orderService.cancelOrder(id, jwt.getTokenValue());
        return ResponseEntity.ok(cancelledOrder);
    }
}

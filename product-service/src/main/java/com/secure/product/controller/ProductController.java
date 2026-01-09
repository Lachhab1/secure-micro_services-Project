package com.secure.product.controller;

import com.secure.product.entity.Product;
import com.secure.product.service.ProductService;
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
 * Contrôleur REST pour la gestion des produits.
 * Toutes les opérations sont sécurisées par rôle via Keycloak.
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Products", description = "API de gestion des produits")
@SecurityRequirement(name = "bearer-jwt")
public class ProductController {

    private final ProductService productService;

    /**
     * Liste tous les produits.
     * Accessible aux rôles ADMIN et CLIENT.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT')")
    @Operation(summary = "Lister tous les produits", description = "Retourne la liste de tous les produits du catalogue")
    public ResponseEntity<List<Product>> getAllProducts(@AuthenticationPrincipal Jwt jwt) {
        log.info("GET /api/products - Utilisateur: {}", jwt.getSubject());
        List<Product> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    /**
     * Récupère un produit par son identifiant.
     * Accessible aux rôles ADMIN et CLIENT.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT')")
    @Operation(summary = "Obtenir un produit par ID", description = "Retourne les détails d'un produit spécifique")
    public ResponseEntity<Product> getProductById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        log.info("GET /api/products/{} - Utilisateur: {}", id, jwt.getSubject());
        Product product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    /**
     * Crée un nouveau produit.
     * Réservé au rôle ADMIN.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer un produit", description = "Crée un nouveau produit dans le catalogue (ADMIN uniquement)")
    public ResponseEntity<Product> createProduct(@Valid @RequestBody Product product,
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        log.info("POST /api/products - Création par: {}", username);
        Product createdProduct = productService.createProduct(product, username);
        return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
    }

    /**
     * Met à jour un produit existant.
     * Réservé au rôle ADMIN.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mettre à jour un produit", description = "Met à jour les informations d'un produit existant (ADMIN uniquement)")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @Valid @RequestBody Product product,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("PUT /api/products/{} - Modification par: {}", id, jwt.getClaimAsString("preferred_username"));
        Product updatedProduct = productService.updateProduct(id, product);
        return ResponseEntity.ok(updatedProduct);
    }

    /**
     * Supprime un produit.
     * Réservé au rôle ADMIN.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer un produit", description = "Supprime un produit du catalogue (ADMIN uniquement)")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        log.info("DELETE /api/products/{} - Suppression par: {}", id, jwt.getClaimAsString("preferred_username"));
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Recherche des produits par nom.
     * Accessible aux rôles ADMIN et CLIENT.
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT')")
    @Operation(summary = "Rechercher des produits", description = "Recherche des produits par nom")
    public ResponseEntity<List<Product>> searchProducts(@RequestParam String name, @AuthenticationPrincipal Jwt jwt) {
        log.info("GET /api/products/search?name={} - Utilisateur: {}", name, jwt.getSubject());
        List<Product> products = productService.searchProductsByName(name);
        return ResponseEntity.ok(products);
    }

    /**
     * Vérifie la disponibilité du stock.
     * Endpoint interne pour le service Commande.
     */
    @GetMapping("/{id}/stock/check")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT')")
    @Operation(summary = "Vérifier le stock", description = "Vérifie si la quantité demandée est disponible")
    public ResponseEntity<Map<String, Boolean>> checkStock(@PathVariable Long id, @RequestParam Integer quantity) {
        log.info("GET /api/products/{}/stock/check?quantity={}", id, quantity);
        boolean available = productService.checkStockAvailability(id, quantity);
        return ResponseEntity.ok(Map.of("available", available));
    }

    /**
     * Décrémente le stock d'un produit.
     * Endpoint interne appelé par le service Commande.
     */
    @PutMapping("/{id}/stock/decrement")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT')")
    @Operation(summary = "Décrémenter le stock", description = "Réduit le stock d'un produit (appelé par le service Commande)")
    public ResponseEntity<Void> decrementStock(@PathVariable Long id, @RequestBody Map<String, Integer> request) {
        log.info("PUT /api/products/{}/stock/decrement - quantité: {}", id, request.get("quantity"));
        productService.decrementStock(id, request.get("quantity"));
        return ResponseEntity.ok().build();
    }

    /**
     * Incrémente le stock d'un produit.
     * Endpoint interne pour annulation de commande.
     */
    @PutMapping("/{id}/stock/increment")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Incrémenter le stock", description = "Augmente le stock d'un produit (annulation de commande)")
    public ResponseEntity<Void> incrementStock(@PathVariable Long id, @RequestBody Map<String, Integer> request) {
        log.info("PUT /api/products/{}/stock/increment - quantité: {}", id, request.get("quantity"));
        productService.incrementStock(id, request.get("quantity"));
        return ResponseEntity.ok().build();
    }
}

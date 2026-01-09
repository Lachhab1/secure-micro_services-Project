package com.secure.product.repository;

import com.secure.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository JPA pour la gestion des produits.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Recherche les produits par nom (insensible à la casse).
     */
    List<Product> findByNameContainingIgnoreCase(String name);

    /**
     * Recherche les produits avec stock disponible.
     */
    List<Product> findByStockQuantityGreaterThan(Integer minStock);

    /**
     * Vérifie si un produit existe par son nom.
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Met à jour le stock d'un produit.
     */
    @Modifying
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity - :quantity WHERE p.id = :id AND p.stockQuantity >= :quantity")
    int decrementStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    /**
     * Vérifie la disponibilité du stock.
     */
    @Query("SELECT CASE WHEN p.stockQuantity >= :quantity THEN true ELSE false END FROM Product p WHERE p.id = :id")
    Optional<Boolean> isStockAvailable(@Param("id") Long id, @Param("quantity") Integer quantity);
}

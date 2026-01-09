package com.secure.product.service;

import com.secure.product.entity.Product;
import com.secure.product.exception.InsufficientStockException;
import com.secure.product.exception.ProductNotFoundException;
import com.secure.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service métier pour la gestion des produits.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * Récupère tous les produits.
     */
    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        log.info("Récupération de tous les produits");
        return productRepository.findAll();
    }

    /**
     * Récupère un produit par son identifiant.
     */
    @Transactional(readOnly = true)
    public Product getProductById(Long id) {
        log.info("Récupération du produit avec id: {}", id);
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Produit non trouvé avec l'id: " + id));
    }

    /**
     * Crée un nouveau produit.
     */
    public Product createProduct(Product product, String createdBy) {
        log.info("Création d'un nouveau produit: {} par l'utilisateur: {}", product.getName(), createdBy);
        product.setCreatedBy(createdBy);
        Product savedProduct = productRepository.save(product);
        log.info("Produit créé avec succès, id: {}", savedProduct.getId());
        return savedProduct;
    }

    /**
     * Met à jour un produit existant.
     */
    public Product updateProduct(Long id, Product productDetails) {
        log.info("Mise à jour du produit avec id: {}", id);
        Product existingProduct = getProductById(id);

        existingProduct.setName(productDetails.getName());
        existingProduct.setDescription(productDetails.getDescription());
        existingProduct.setPrice(productDetails.getPrice());
        existingProduct.setStockQuantity(productDetails.getStockQuantity());

        Product updatedProduct = productRepository.save(existingProduct);
        log.info("Produit mis à jour avec succès, id: {}", updatedProduct.getId());
        return updatedProduct;
    }

    /**
     * Supprime un produit.
     */
    public void deleteProduct(Long id) {
        log.info("Suppression du produit avec id: {}", id);
        Product product = getProductById(id);
        productRepository.delete(product);
        log.info("Produit supprimé avec succès, id: {}", id);
    }

    /**
     * Recherche des produits par nom.
     */
    @Transactional(readOnly = true)
    public List<Product> searchProductsByName(String name) {
        log.info("Recherche de produits avec le nom contenant: {}", name);
        return productRepository.findByNameContainingIgnoreCase(name);
    }

    /**
     * Vérifie la disponibilité du stock.
     */
    @Transactional(readOnly = true)
    public boolean checkStockAvailability(Long productId, Integer quantity) {
        log.info("Vérification du stock pour le produit {}, quantité demandée: {}", productId, quantity);
        return productRepository.isStockAvailable(productId, quantity)
                .orElseThrow(() -> new ProductNotFoundException("Produit non trouvé avec l'id: " + productId));
    }

    /**
     * Décrémente le stock d'un produit (appelé par le service Commande).
     */
    public void decrementStock(Long productId, Integer quantity) {
        log.info("Décrémentation du stock pour le produit {}, quantité: {}", productId, quantity);
        int updatedRows = productRepository.decrementStock(productId, quantity);
        if (updatedRows == 0) {
            Product product = getProductById(productId);
            throw new InsufficientStockException(
                    String.format("Stock insuffisant pour le produit '%s'. Stock disponible: %d, quantité demandée: %d",
                            product.getName(), product.getStockQuantity(), quantity));
        }
        log.info("Stock décrémenté avec succès pour le produit {}", productId);
    }

    /**
     * Incrémente le stock d'un produit (pour annulation de commande).
     */
    public void incrementStock(Long productId, Integer quantity) {
        log.info("Incrémentation du stock pour le produit {}, quantité: {}", productId, quantity);
        Product product = getProductById(productId);
        product.setStockQuantity(product.getStockQuantity() + quantity);
        productRepository.save(product);
        log.info("Stock incrémenté avec succès pour le produit {}", productId);
    }
}

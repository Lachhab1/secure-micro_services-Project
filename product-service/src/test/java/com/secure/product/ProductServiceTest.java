package com.secure.product;

import com.secure.product.entity.Product;
import com.secure.product.repository.ProductRepository;
import com.secure.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires pour le service Produit.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();

        testProduct = Product.builder()
                .name("Produit Test")
                .description("Description du produit test")
                .price(new BigDecimal("99.99"))
                .stockQuantity(100)
                .build();
    }

    @Test
    @DisplayName("Doit créer un nouveau produit avec succès")
    void shouldCreateProduct() {
        // When
        Product created = productService.createProduct(testProduct, "admin");

        // Then
        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Produit Test");
        assertThat(created.getCreatedBy()).isEqualTo("admin");
    }

    @Test
    @DisplayName("Doit récupérer un produit par son ID")
    void shouldGetProductById() {
        // Given
        Product saved = productRepository.save(testProduct);

        // When
        Product found = productService.getProductById(saved.getId());

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getName()).isEqualTo("Produit Test");
    }

    @Test
    @DisplayName("Doit lister tous les produits")
    void shouldGetAllProducts() {
        // Given
        productRepository.save(testProduct);
        productRepository.save(Product.builder()
                .name("Autre Produit")
                .price(new BigDecimal("49.99"))
                .stockQuantity(50)
                .build());

        // When
        List<Product> products = productService.getAllProducts();

        // Then
        assertThat(products).hasSize(2);
    }

    @Test
    @DisplayName("Doit mettre à jour un produit")
    void shouldUpdateProduct() {
        // Given
        Product saved = productRepository.save(testProduct);
        Product updateData = Product.builder()
                .name("Produit Modifié")
                .description("Nouvelle description")
                .price(new BigDecimal("149.99"))
                .stockQuantity(200)
                .build();

        // When
        Product updated = productService.updateProduct(saved.getId(), updateData);

        // Then
        assertThat(updated.getName()).isEqualTo("Produit Modifié");
        assertThat(updated.getPrice()).isEqualByComparingTo(new BigDecimal("149.99"));
    }

    @Test
    @DisplayName("Doit supprimer un produit")
    void shouldDeleteProduct() {
        // Given
        Product saved = productRepository.save(testProduct);
        Long id = saved.getId();

        // When
        productService.deleteProduct(id);

        // Then
        assertThat(productRepository.findById(id)).isEmpty();
    }

    @Test
    @DisplayName("Doit vérifier la disponibilité du stock")
    void shouldCheckStockAvailability() {
        // Given
        Product saved = productRepository.save(testProduct);

        // When & Then
        assertThat(productService.checkStockAvailability(saved.getId(), 50)).isTrue();
        assertThat(productService.checkStockAvailability(saved.getId(), 100)).isTrue();
        assertThat(productService.checkStockAvailability(saved.getId(), 101)).isFalse();
    }

    @Test
    @DisplayName("Doit décrémenter le stock avec succès")
    void shouldDecrementStock() {
        // Given
        Product saved = productRepository.save(testProduct);

        // When
        productService.decrementStock(saved.getId(), 30);

        // Then
        Product updated = productRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getStockQuantity()).isEqualTo(70);
    }

    @Test
    @DisplayName("Doit lever une exception si stock insuffisant")
    void shouldThrowExceptionWhenInsufficientStock() {
        // Given
        Product saved = productRepository.save(testProduct);

        // When & Then
        assertThatThrownBy(() -> productService.decrementStock(saved.getId(), 150))
                .isInstanceOf(com.secure.product.exception.InsufficientStockException.class);
    }

    @Test
    @DisplayName("Doit rechercher des produits par nom")
    void shouldSearchProductsByName() {
        // Given
        productRepository.save(testProduct);
        productRepository.save(Product.builder()
                .name("Téléphone Test")
                .price(new BigDecimal("599.99"))
                .stockQuantity(20)
                .build());

        // When
        List<Product> results = productService.searchProductsByName("Test");

        // Then
        assertThat(results).hasSize(2);
    }
}

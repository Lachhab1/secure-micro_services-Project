package com.secure.order.service;

import com.secure.order.client.ProductDTO;
import com.secure.order.client.ProductServiceClient;
import com.secure.order.entity.Order;
import com.secure.order.entity.OrderItem;
import com.secure.order.entity.OrderStatus;
import com.secure.order.exception.OrderNotFoundException;
import com.secure.order.exception.ProductNotAvailableException;
import com.secure.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service métier pour la gestion des commandes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductServiceClient productServiceClient;

    /**
     * Crée une nouvelle commande.
     * Vérifie la disponibilité des produits et décrémente le stock.
     */
    public Order createOrder(Order order, String userId, String username, String jwtToken) {
        log.info("Création d'une commande pour l'utilisateur: {}", username);

        order.setUserId(userId);
        order.setUsername(username);
        order.setStatus(OrderStatus.PENDING);

        // Valider chaque item et récupérer les informations du produit
        for (OrderItem item : order.getItems()) {
            Long productId = item.getProductId();
            Integer quantity = item.getQuantity();

            // Récupérer le produit depuis le service Produit
            ProductDTO product = productServiceClient.getProduct(productId, jwtToken)
                    .orElseThrow(() -> new ProductNotAvailableException(
                            "Produit non trouvé avec l'ID: " + productId));

            // Vérifier la disponibilité du stock
            if (!productServiceClient.checkStockAvailability(productId, quantity, jwtToken)) {
                throw new ProductNotAvailableException(
                        String.format("Stock insuffisant pour le produit '%s'. Quantité demandée: %d",
                                product.getName(), quantity));
            }

            // Enrichir l'item avec les informations du produit
            item.setProductName(product.getName());
            item.setPrice(product.getPrice());
            item.setOrder(order);
        }

        // Calculer le montant total
        order.calculateTotalAmount();

        // Sauvegarder la commande
        Order savedOrder = orderRepository.save(order);
        log.info("Commande créée avec succès, ID: {}", savedOrder.getId());

        // Décrémenter le stock pour chaque produit
        for (OrderItem item : savedOrder.getItems()) {
            boolean decremented = productServiceClient.decrementStock(
                    item.getProductId(), item.getQuantity(), jwtToken);
            if (!decremented) {
                log.warn("Échec de la décrémentation du stock pour le produit: {}", item.getProductId());
            }
        }

        // Confirmer la commande
        savedOrder.setStatus(OrderStatus.CONFIRMED);
        return orderRepository.save(savedOrder);
    }

    /**
     * Récupère les commandes d'un utilisateur.
     */
    @Transactional(readOnly = true)
    public List<Order> getOrdersByUserId(String userId) {
        log.info("Récupération des commandes pour l'utilisateur: {}", userId);
        return orderRepository.findByUserIdOrderByOrderDateDesc(userId);
    }

    /**
     * Récupère toutes les commandes (ADMIN).
     */
    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        log.info("Récupération de toutes les commandes");
        return orderRepository.findAll();
    }

    /**
     * Récupère une commande par son ID.
     */
    @Transactional(readOnly = true)
    public Order getOrderById(Long id) {
        log.info("Récupération de la commande ID: {}", id);
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Commande non trouvée avec l'ID: " + id));
    }

    /**
     * Met à jour le statut d'une commande.
     */
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
        log.info("Mise à jour du statut de la commande {} vers {}", orderId, newStatus);
        Order order = getOrderById(orderId);
        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    /**
     * Annule une commande.
     */
    public Order cancelOrder(Long orderId, String jwtToken) {
        log.info("Annulation de la commande ID: {}", orderId);
        Order order = getOrderById(orderId);

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("La commande est déjà annulée");
        }

        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Impossible d'annuler une commande déjà livrée");
        }

        order.setStatus(OrderStatus.CANCELLED);

        // Note: La restauration du stock serait gérée ici si nécessaire
        log.info("Commande {} annulée avec succès", orderId);

        return orderRepository.save(order);
    }
}

package com.secure.order.entity;

/**
 * Statuts possibles d'une commande.
 */
public enum OrderStatus {
    PENDING, // En attente de validation
    CONFIRMED, // Confirmée
    PROCESSING, // En cours de traitement
    SHIPPED, // Expédiée
    DELIVERED, // Livrée
    CANCELLED // Annulée
}

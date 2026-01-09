package com.secure.order.repository;

import com.secure.order.entity.Order;
import com.secure.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository JPA pour les commandes.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Récupère les commandes d'un utilisateur.
     */
    List<Order> findByUserIdOrderByOrderDateDesc(String userId);

    /**
     * Récupère les commandes par statut.
     */
    List<Order> findByStatus(OrderStatus status);

    /**
     * Récupère les commandes dans une période donnée.
     */
    @Query("SELECT o FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate ORDER BY o.orderDate DESC")
    List<Order> findOrdersBetweenDates(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Compte le nombre de commandes d'un utilisateur.
     */
    long countByUserId(String userId);
}

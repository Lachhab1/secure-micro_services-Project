package com.secure.order;

import com.secure.order.entity.Order;
import com.secure.order.entity.OrderItem;
import com.secure.order.entity.OrderStatus;
import com.secure.order.exception.OrderNotFoundException;
import com.secure.order.repository.OrderRepository;
import com.secure.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour le service Commande.
 */
@SpringBootTest
@ActiveProfiles("test")
public class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    @Test
    void testCreateOrder() {
        // Given
        Order order = new Order();
        order.setUserId("user-123");
        order.setUsername("testuser");

        OrderItem item = new OrderItem();
        item.setProductId(1L);
        item.setProductName("Test Product");
        item.setQuantity(2);
        item.setPrice(new BigDecimal("25.00"));
        order.addItem(item);

        // When
        Order savedOrder = orderRepository.save(order);

        // Then
        assertNotNull(savedOrder.getId());
        assertEquals("user-123", savedOrder.getUserId());
        assertEquals("testuser", savedOrder.getUsername());
        assertEquals(OrderStatus.PENDING, savedOrder.getStatus());
        assertEquals(1, savedOrder.getItems().size());
        assertEquals(new BigDecimal("50.00"), savedOrder.getTotalAmount());
    }

    @Test
    void testFindOrdersByUserId() {
        // Given
        Order order1 = createTestOrder("user-1", "client1");
        Order order2 = createTestOrder("user-1", "client1");
        Order order3 = createTestOrder("user-2", "client2");
        orderRepository.saveAll(List.of(order1, order2, order3));

        // When
        List<Order> userOrders = orderRepository.findByUserIdOrderByOrderDateDesc("user-1");

        // Then
        assertEquals(2, userOrders.size());
        assertTrue(userOrders.stream().allMatch(o -> o.getUserId().equals("user-1")));
    }

    @Test
    void testFindOrdersByStatus() {
        // Given
        Order order1 = createTestOrder("user-1", "client1");
        Order order2 = createTestOrder("user-2", "client2");
        order2.setStatus(OrderStatus.CONFIRMED);
        orderRepository.saveAll(List.of(order1, order2));

        // When
        List<Order> pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING);
        List<Order> confirmedOrders = orderRepository.findByStatus(OrderStatus.CONFIRMED);

        // Then
        assertEquals(1, pendingOrders.size());
        assertEquals(1, confirmedOrders.size());
        assertEquals(OrderStatus.PENDING, pendingOrders.get(0).getStatus());
        assertEquals(OrderStatus.CONFIRMED, confirmedOrders.get(0).getStatus());
    }

    @Test
    void testUpdateOrderStatus() {
        // Given
        Order order = createTestOrder("user-1", "client1");
        Order savedOrder = orderRepository.save(order);

        // When
        savedOrder.setStatus(OrderStatus.SHIPPED);
        Order updatedOrder = orderRepository.save(savedOrder);

        // Then
        assertEquals(OrderStatus.SHIPPED, updatedOrder.getStatus());
    }

    @Test
    void testCalculateTotalAmount() {
        // Given
        Order order = new Order();
        order.setUserId("user-1");
        order.setUsername("testuser");

        OrderItem item1 = new OrderItem();
        item1.setProductId(1L);
        item1.setProductName("Product A");
        item1.setQuantity(3);
        item1.setPrice(new BigDecimal("10.00"));

        OrderItem item2 = new OrderItem();
        item2.setProductId(2L);
        item2.setProductName("Product B");
        item2.setQuantity(2);
        item2.setPrice(new BigDecimal("15.50"));

        order.addItem(item1);
        order.addItem(item2);

        // When
        order.calculateTotalAmount();

        // Then
        // 3 * 10.00 + 2 * 15.50 = 30.00 + 31.00 = 61.00
        assertEquals(new BigDecimal("61.00"), order.getTotalAmount());
    }

    @Test
    void testOrderItemSubtotal() {
        // Given
        OrderItem item = new OrderItem();
        item.setQuantity(5);
        item.setPrice(new BigDecimal("12.50"));

        // When
        BigDecimal subtotal = item.getSubtotal();

        // Then
        assertEquals(new BigDecimal("62.50"), subtotal);
    }

    @Test
    void testOrderStatusTransitions() {
        // Given
        Order order = createTestOrder("user-1", "client1");
        Order savedOrder = orderRepository.save(order);
        assertEquals(OrderStatus.PENDING, savedOrder.getStatus());

        // When / Then - Test valid transitions
        savedOrder.setStatus(OrderStatus.CONFIRMED);
        savedOrder = orderRepository.save(savedOrder);
        assertEquals(OrderStatus.CONFIRMED, savedOrder.getStatus());

        savedOrder.setStatus(OrderStatus.PROCESSING);
        savedOrder = orderRepository.save(savedOrder);
        assertEquals(OrderStatus.PROCESSING, savedOrder.getStatus());

        savedOrder.setStatus(OrderStatus.SHIPPED);
        savedOrder = orderRepository.save(savedOrder);
        assertEquals(OrderStatus.SHIPPED, savedOrder.getStatus());

        savedOrder.setStatus(OrderStatus.DELIVERED);
        savedOrder = orderRepository.save(savedOrder);
        assertEquals(OrderStatus.DELIVERED, savedOrder.getStatus());
    }

    @Test
    void testCancelOrder() {
        // Given
        Order order = createTestOrder("user-1", "client1");
        Order savedOrder = orderRepository.save(order);

        // When
        savedOrder.setStatus(OrderStatus.CANCELLED);
        Order cancelledOrder = orderRepository.save(savedOrder);

        // Then
        assertEquals(OrderStatus.CANCELLED, cancelledOrder.getStatus());
    }

    private Order createTestOrder(String userId, String username) {
        Order order = new Order();
        order.setUserId(userId);
        order.setUsername(username);

        OrderItem item = new OrderItem();
        item.setProductId(1L);
        item.setProductName("Test Product");
        item.setQuantity(1);
        item.setPrice(new BigDecimal("10.00"));
        order.addItem(item);

        return order;
    }
}

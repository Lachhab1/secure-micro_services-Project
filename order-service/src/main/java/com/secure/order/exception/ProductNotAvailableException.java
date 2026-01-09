package com.secure.order.exception;

/**
 * Exception levée lorsqu'un produit n'est pas disponible ou le stock est
 * insuffisant.
 */
public class ProductNotAvailableException extends RuntimeException {

    public ProductNotAvailableException(String message) {
        super(message);
    }
}

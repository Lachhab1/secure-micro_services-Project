package com.secure.product.exception;

/**
 * Exception levée lorsqu'un produit n'est pas trouvé.
 */
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String message) {
        super(message);
    }

    public ProductNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

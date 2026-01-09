package com.secure.order.exception;

/**
 * Exception levée lorsqu'une commande n'est pas trouvée.
 */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String message) {
        super(message);
    }
}

package com.deepblue.rescue.exception;

/** El recurso solicitado no existe (se traduce a HTTP 404). */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}

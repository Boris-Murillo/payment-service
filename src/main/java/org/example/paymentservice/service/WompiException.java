package org.example.paymentservice.service;

public class WompiException extends RuntimeException {

    public WompiException(String message) {
        super(message);
    }

    public WompiException(String message, Throwable cause) {
        super(message, cause);
    }
}

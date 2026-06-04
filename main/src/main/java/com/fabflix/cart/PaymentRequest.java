package com.fabflix.cart;

public record PaymentRequest(String firstName, String lastName, String cardNumber, String expiration) {
}

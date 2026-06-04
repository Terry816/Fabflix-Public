package com.fabflix.auth;

public record AuthAttempt<T>(boolean success, String message, T principal) {
    public static <T> AuthAttempt<T> success(String message, T principal) {
        return new AuthAttempt<>(true, message, principal);
    }

    public static <T> AuthAttempt<T> fail(String message) {
        return new AuthAttempt<>(false, message, null);
    }
}

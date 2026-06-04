package com.fabflix.common;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ApiResponses {
    private ApiResponses() {
    }

    public static Map<String, Object> success(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "success");
        body.put("message", message);
        return body;
    }

    public static Map<String, Object> fail(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "fail");
        body.put("message", message);
        return body;
    }
}

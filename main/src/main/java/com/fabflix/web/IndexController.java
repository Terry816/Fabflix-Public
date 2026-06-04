package com.fabflix.web;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/index")
public class IndexController {
    @GetMapping
    public Map<String, Object> index(HttpServletRequest request) {
        Claims claims = (Claims) request.getAttribute("authClaims");
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sessionID", "");
        response.put("lastAccessTime", Instant.now().toString());
        if (claims != null) {
            response.put("firstName", claims.get("firstName", String.class));
        }
        response.put("previousItems", List.of());
        return response;
    }

    @PostMapping
    public Map<String, Object> rememberItem(@RequestParam("item") String item) {
        Map<String, Object> response = new LinkedHashMap<>();
        List<String> items = new ArrayList<>();
        items.add(item);
        response.put("previousItems", items);
        return response;
    }
}

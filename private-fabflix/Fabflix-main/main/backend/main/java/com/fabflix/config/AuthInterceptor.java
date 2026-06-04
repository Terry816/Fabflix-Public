package com.fabflix.config;

import com.fabflix.auth.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Set;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/health",
            "/api/login",
            "/api/logout",
            "/api/employee-login",
            "/api/autocomplete"
    );

    private static final Set<String> EMPLOYEE_PATHS = Set.of(
            "/api/database-metadata",
            "/api/insert-genre",
            "/api/insert-star",
            "/api/insert-movie"
    );

    private final JwtService jwtService;

    public AuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (PUBLIC_PATHS.contains(path)) {
            return true;
        }

        boolean employeeRoute = EMPLOYEE_PATHS.contains(path);
        String cookieName = employeeRoute ? JwtService.EMPLOYEE_COOKIE : JwtService.CUSTOMER_COOKIE;
        Claims claims = jwtService.validateToken(jwtService.getCookieValue(request, cookieName));
        if (claims == null) {
            writeAuthError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized. Invalid or missing token.");
            return false;
        }

        String expectedRole = employeeRoute ? "employee" : "customer";
        if (!expectedRole.equals(claims.get("role", String.class))) {
            writeAuthError(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden. Invalid account type for this endpoint.");
            return false;
        }

        request.setAttribute("authClaims", claims);
        if (!employeeRoute) {
            request.setAttribute("userId", extractUserId(claims));
        }
        return true;
    }

    private Integer extractUserId(Claims claims) {
        Object userId = claims.get("userId");
        if (userId instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private void writeAuthError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"status\":\"fail\",\"message\":\"" + message + "\"}");
    }
}

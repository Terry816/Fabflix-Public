package com.fabflix.auth;

import com.fabflix.common.ApiResponses;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "password", required = false) String password
    ) {
        String resolvedEmail = trimToNull(username) == null ? trimToNull(email) : trimToNull(username);
        String resolvedPassword = trimToNull(password);
        if (resolvedEmail == null || resolvedPassword == null) {
            return ResponseEntity.badRequest().body(ApiResponses.fail("Missing email or password."));
        }

        AuthAttempt<CustomerPrincipal> attempt = authService.authenticateCustomer(resolvedEmail, resolvedPassword);
        if (!attempt.success()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponses.fail(attempt.message()));
        }

        CustomerPrincipal principal = attempt.principal();
        String token = jwtService.generateToken(principal.email(), Map.of(
                "role", "customer",
                "userId", principal.id(),
                "firstName", principal.firstName()
        ));

        Map<String, Object> body = ApiResponses.success(attempt.message());
        body.put("firstName", principal.firstName());
        ResponseCookie cookie = jwtService.createCookie(JwtService.CUSTOMER_COOKIE, token);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(body);
    }

    @PostMapping("/employee-login")
    public ResponseEntity<Map<String, Object>> employeeLogin(
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "password", required = false) String password
    ) {
        String resolvedEmail = trimToNull(email);
        String resolvedPassword = trimToNull(password);
        if (resolvedEmail == null || resolvedPassword == null) {
            return ResponseEntity.badRequest().body(ApiResponses.fail("Missing email or password."));
        }

        AuthAttempt<EmployeePrincipal> attempt = authService.authenticateEmployee(resolvedEmail, resolvedPassword);
        if (!attempt.success()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponses.fail(attempt.message()));
        }

        EmployeePrincipal principal = attempt.principal();
        String token = jwtService.generateToken(principal.email(), Map.of(
                "role", "employee",
                "fullName", principal.fullName()
        ));

        Map<String, Object> body = ApiResponses.success(attempt.message());
        body.put("fullName", principal.fullName());
        ResponseCookie cookie = jwtService.createCookie(JwtService.EMPLOYEE_COOKIE, token);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(body);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        return logoutResponse();
    }

    @GetMapping("/logout")
    public ResponseEntity<Map<String, Object>> logoutFromLegacyLink() {
        return logoutResponse();
    }

    private ResponseEntity<Map<String, Object>> logoutResponse() {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtService.clearCookie(JwtService.CUSTOMER_COOKIE).toString())
                .header(HttpHeaders.SET_COOKIE, jwtService.clearCookie(JwtService.EMPLOYEE_COOKIE).toString())
                .body(ApiResponses.success("Logged out successfully."));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

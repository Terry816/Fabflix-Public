package com.fabflix.auth;

import org.jasypt.util.password.StrongPasswordEncryptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AuthService {
    private final JdbcTemplate jdbcTemplate;
    private final StrongPasswordEncryptor passwordEncryptor = new StrongPasswordEncryptor();

    public AuthService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AuthAttempt<CustomerPrincipal> authenticateCustomer(String email, String password) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, email, firstName, password FROM customers WHERE email = ?",
                email
        );

        if (rows.isEmpty()) {
            return AuthAttempt.fail("User not found.");
        }

        Map<String, Object> row = rows.get(0);
        if (!passwordMatches(password, String.valueOf(row.get("password")))) {
            return AuthAttempt.fail("Incorrect password.");
        }

        CustomerPrincipal principal = new CustomerPrincipal(
                ((Number) row.get("id")).intValue(),
                String.valueOf(row.get("email")),
                String.valueOf(row.get("firstname"))
        );
        return AuthAttempt.success("Login successful.", principal);
    }

    public AuthAttempt<EmployeePrincipal> authenticateEmployee(String email, String password) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT email, password, fullname FROM employees WHERE email = ?",
                email
        );

        if (rows.isEmpty() || !password.equals(String.valueOf(rows.get(0).get("password")))) {
            return AuthAttempt.fail("Invalid email or password.");
        }

        Map<String, Object> row = rows.get(0);
        EmployeePrincipal principal = new EmployeePrincipal(
                String.valueOf(row.get("email")),
                String.valueOf(row.get("fullname"))
        );
        return AuthAttempt.success("Login successful.", principal);
    }

    private boolean passwordMatches(String submittedPassword, String storedPassword) {
        if (submittedPassword.equals(storedPassword)) {
            return true;
        }

        try {
            return passwordEncryptor.checkPassword(submittedPassword, storedPassword);
        } catch (Exception ignored) {
            return false;
        }
    }
}

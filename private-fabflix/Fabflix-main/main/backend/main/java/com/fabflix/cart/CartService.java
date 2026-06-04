package com.fabflix.cart;

import com.fabflix.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CartService {
    private final JdbcTemplate jdbcTemplate;

    public CartService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> cartItems(int customerId) {
        String sql = """
                SELECT c.movieId AS movieId, m.title, m.year, m.director, r.rating, c.quantity
                FROM carts c
                JOIN movies m ON c.movieId = m.id
                LEFT JOIN ratings r ON m.id = r.movieId
                WHERE c.customerId = ?
                ORDER BY m.title ASC
                """;
        return jdbcTemplate.query(sql, ps -> ps.setInt(1, customerId), (rs, rowNum) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("movieId", rs.getString("movieId"));
            item.put("title", rs.getString("title"));
            item.put("year", rs.getInt("year"));
            item.put("director", rs.getString("director"));
            double rating = rs.getDouble("rating");
            item.put("rating", rs.wasNull() ? null : rating);
            item.put("quantity", rs.getInt("quantity"));
            return item;
        });
    }

    public void addToCart(int customerId, String movieId) {
        String resolvedMovieId = trimToNull(movieId);
        if (resolvedMovieId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Missing movieId.");
        }

        jdbcTemplate.update("""
                INSERT INTO carts (customerId, movieId, quantity)
                VALUES (?, ?, 1)
                ON CONFLICT (customerId, movieId)
                DO UPDATE SET quantity = carts.quantity + 1
                """, customerId, resolvedMovieId);
    }

    public void updateCart(int customerId, String movieId, int quantity) {
        String resolvedMovieId = trimToNull(movieId);
        if (resolvedMovieId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Missing movieId.");
        }
        if (quantity < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Quantity cannot be negative.");
        }

        if (quantity == 0) {
            jdbcTemplate.update("DELETE FROM carts WHERE customerId = ? AND movieId = ?", customerId, resolvedMovieId);
            return;
        }

        jdbcTemplate.update("""
                INSERT INTO carts (customerId, movieId, quantity)
                VALUES (?, ?, ?)
                ON CONFLICT (customerId, movieId)
                DO UPDATE SET quantity = EXCLUDED.quantity
                """, customerId, resolvedMovieId, quantity);
    }

    @Transactional
    public void placeOrder(int customerId, PaymentRequest paymentRequest) {
        validatePayment(paymentRequest);

        List<Map<String, Object>> lines = jdbcTemplate.queryForList(
                "SELECT movieId, quantity FROM carts WHERE customerId = ?",
                customerId
        );
        if (lines.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cart is empty.");
        }

        Date saleDate = Date.valueOf(LocalDate.now());
        for (Map<String, Object> line : lines) {
            String movieId = String.valueOf(line.get("movieid"));
            int quantity = ((Number) line.get("quantity")).intValue();
            for (int i = 0; i < quantity; i++) {
                jdbcTemplate.update(
                        "INSERT INTO sales (customerId, movieId, saleDate) VALUES (?, ?, ?)",
                        customerId,
                        movieId,
                        saleDate
                );
            }
        }

        jdbcTemplate.update("DELETE FROM carts WHERE customerId = ?", customerId);
    }

    public List<Map<String, Object>> orderSummary(int customerId) {
        Date latestSaleDate = jdbcTemplate.query(
                "SELECT MAX(saleDate) AS latest_sale_date FROM sales WHERE customerId = ?",
                ps -> ps.setInt(1, customerId),
                rs -> rs.next() ? rs.getDate("latest_sale_date") : null
        );

        if (latestSaleDate == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "No recent sale found.");
        }

        String sql = """
                SELECT s.movieId AS movieId, m.title, COUNT(*) AS quantity
                FROM sales s
                JOIN movies m ON s.movieId = m.id
                WHERE s.customerId = ? AND s.saleDate = ?
                GROUP BY s.movieId, m.title
                ORDER BY m.title ASC
                """;
        return jdbcTemplate.query(sql, ps -> {
            ps.setInt(1, customerId);
            ps.setDate(2, latestSaleDate);
        }, (rs, rowNum) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("movieId", rs.getString("movieId"));
            item.put("title", rs.getString("title"));
            item.put("quantity", rs.getInt("quantity"));
            return item;
        });
    }

    private void validatePayment(PaymentRequest request) {
        if (trimToNull(request.firstName()) == null
                || trimToNull(request.lastName()) == null
                || trimToNull(request.cardNumber()) == null
                || trimToNull(request.expiration()) == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Missing payment information.");
        }

        LocalDate expiration;
        try {
            expiration = LocalDate.parse(request.expiration());
        } catch (DateTimeParseException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid expiration date.");
        }

        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM creditcards
                WHERE id = ? AND firstName = ? AND lastName = ? AND expiration = ?
                """, Integer.class, request.cardNumber(), request.firstName(), request.lastName(), Date.valueOf(expiration));

        if (count == null || count == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Payment information could not be verified.");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

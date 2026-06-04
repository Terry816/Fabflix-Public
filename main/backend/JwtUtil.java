import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

public class JwtUtil {
    public static final String JWT_COOKIE_NAME = "jwtToken";
    private static final SecretKey key = Keys.hmacShaKeyFor(resolveSecret().getBytes(StandardCharsets.UTF_8));
    private static final long EXPIRATION_TIME = 86400000; // 1 day

    private static String resolveSecret() {
        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be set and at least 32 characters long.");
        }
        return secret;
    }

    // Generate a JWT token
    // claims can be used as session to store anything related to the user
    public static String generateToken(String subject, Map<String, Object> claims) {
        return Jwts.builder()
                .setSubject(subject)
                .addClaims(claims) // Add custom claims here
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // Validate a JWT token
    // If the token is not valid, a null pointer will be returned
    public static Claims validateToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null;
        }
    }

    // Set the JWT token in cookie
    // If there already exists a cookie with the name "jwtToken", update that cookie
    // If no such cookie found, create a new cookie
    public static void updateJwtCookie(HttpServletRequest request, HttpServletResponse response, String newJwtToken) {
        // Retrieve existing cookies from the request
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (JWT_COOKIE_NAME.equals(cookie.getName())) {
                    // Update the cookie value
                    cookie.setValue(newJwtToken);
                    cookie.setHttpOnly(true);
                    cookie.setPath("/");
                    cookie.setMaxAge(24 * 60 * 60);
                    // Uncomment next line if HTTPS is enabled.
                    //cookie.setSecure(true);

                    // Add the updated cookie to the response
                    response.addCookie(cookie);
                    return;
                }
            }
        }

        // If the cookie wasn't found, create it
        Cookie newCookie = new Cookie(JWT_COOKIE_NAME, newJwtToken);
        newCookie.setHttpOnly(true);
        newCookie.setPath("/");
        newCookie.setMaxAge(24 * 60 * 60);
        //newCookie.setSecure(true);
        response.addCookie(newCookie);
    }

    // Get the cookie from request
    public static String getCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(cookieName)) {
                    return cookie.getValue();
                }
            }
        }
        return null; // Cookie not found
    }
}

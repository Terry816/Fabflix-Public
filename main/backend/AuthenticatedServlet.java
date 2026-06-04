import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public abstract class AuthenticatedServlet extends BaseServlet {
    protected Integer getAuthenticatedUserId(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String token = JwtUtil.getCookieValue(request, JwtUtil.JWT_COOKIE_NAME);
        if (token == null) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Missing JWT token.");
            return null;
        }

        Claims claims = JwtUtil.validateToken(token);
        if (claims == null || claims.get("userId") == null) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired JWT token.");
            return null;
        }

        Object userId = claims.get("userId");
        if (userId instanceof Integer) {
            return (Integer) userId;
        }
        if (userId instanceof Number) {
            return ((Number) userId).intValue();
        }

        writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT token.");
        return null;
    }
}

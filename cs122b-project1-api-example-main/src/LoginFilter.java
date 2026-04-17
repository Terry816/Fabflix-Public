import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.jsonwebtoken.Claims;

import java.io.IOException;

@WebFilter(filterName = "LoginFilter", urlPatterns = {"/api/*"})
public class LoginFilter implements Filter {
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String uri = httpRequest.getRequestURI();

        // ✅ Allow public pages
        if (uri.endsWith("login.html") || uri.endsWith("login") || uri.endsWith("favicon.ico")) {
            chain.doFilter(request, response);
            return;
        }

        // ✅ Get token from cookie
        String token = JwtUtil.getCookieValue(httpRequest, "jwtToken");
        Claims claims = JwtUtil.validateToken(token);
        if (claims == null) {
            httpResponse.setStatus(401);
            httpResponse.getWriter().write("{\"message\": \"Unauthorized. Invalid or missing token.\"}");
            return;
        }

        // ✅ Token is valid, continue
        chain.doFilter(request, response);
    }
}

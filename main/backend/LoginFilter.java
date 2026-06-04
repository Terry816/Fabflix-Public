import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter(filterName = "LoginFilter", urlPatterns = {"/api/*"})
public class LoginFilter implements Filter {
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String uri = httpRequest.getRequestURI();
        String serviceName = System.getenv().getOrDefault("SERVICE_NAME", "fabflix");
        System.out.printf("[%s] %s %s%n", serviceName, httpRequest.getMethod(), uri);

        if (uri.endsWith("/api/login")
                || uri.endsWith("/api/employee-login")
                || uri.endsWith("/api/autocomplete")) {
            chain.doFilter(request, response);
            return;
        }

        if (JwtUtil.validateToken(JwtUtil.getCookieValue(httpRequest, JwtUtil.JWT_COOKIE_NAME)) == null) {
            httpResponse.setContentType("application/json");
            httpResponse.setStatus(401);
            httpResponse.getWriter().write("{\"message\": \"Unauthorized. Invalid or missing token.\"}");
            return;
        }

        // ✅ Token is valid, continue
        chain.doFilter(request, response);
    }
}

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.jsonwebtoken.Claims;

import java.io.IOException;

@WebServlet(name = "LogoutServlet", urlPatterns = "/api/logout")
public class LogoutServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // ✅ Invalidate JWT cookie by setting Max-Age to 0
        Cookie cookie = new Cookie("jwtToken", "");
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0); // delete immediately
        response.addCookie(cookie);

        response.setStatus(200);
        response.getWriter().write("{\"message\": \"Logged out successfully.\"}");
    }
}

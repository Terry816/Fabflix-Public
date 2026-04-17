import com.google.gson.JsonObject;
import io.jsonwebtoken.Claims;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;

@WebServlet(name = "UpdateCartServlet", urlPatterns = "/api/update-cart")
public class UpdateCartServlet extends HttpServlet {
    private DataSource dataSource;

    public void init() {
        try {
            InitialContext ctx = new InitialContext();
            dataSource = (DataSource) ctx.lookup("java:comp/env/jdbc/writeDB");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        JsonObject responseJsonObject = new JsonObject();

        // Extract JWT token from cookies
        String jwtToken = JwtUtil.getCookieValue(request, "jwtToken");
        if (jwtToken == null) {
            response.setStatus(401);
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Missing JWT token.");
            out.write(responseJsonObject.toString());
            return;
        }

        Claims claims = JwtUtil.validateToken(jwtToken);
        if (claims == null) {
            response.setStatus(401);
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Invalid or expired JWT token.");
            out.write(responseJsonObject.toString());
            return;
        }

        int customerId = (int) claims.get("userId");

        String movieId = request.getParameter("movieId");
        int quantity;

        try {
            quantity = Integer.parseInt(request.getParameter("quantity"));
        } catch (NumberFormatException e) {
            response.setStatus(400);
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Invalid quantity.");
            out.write(responseJsonObject.toString());
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            if (quantity == 0) {
                String deleteQuery = "DELETE FROM carts WHERE customerId = ? AND movieId = ?";
                PreparedStatement stmt = conn.prepareStatement(deleteQuery);
                stmt.setInt(1, customerId);
                stmt.setString(2, movieId);
                stmt.executeUpdate();
                stmt.close();
            } else {
                String updateQuery = "INSERT INTO carts (customerId, movieId, quantity) VALUES (?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE quantity = ?";
                PreparedStatement stmt = conn.prepareStatement(updateQuery);
                stmt.setInt(1, customerId);
                stmt.setString(2, movieId);
                stmt.setInt(3, quantity);
                stmt.setInt(4, quantity);
                stmt.executeUpdate();
                stmt.close();
            }

            responseJsonObject.addProperty("status", "success");
            responseJsonObject.addProperty("message", "Cart updated.");
            out.write(responseJsonObject.toString());
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Server error: " + e.getMessage());
            out.write(responseJsonObject.toString());
        } finally {
            out.close();
        }
    }
}

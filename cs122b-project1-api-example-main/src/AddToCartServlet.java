import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.jsonwebtoken.Claims;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;

@WebServlet(name = "AddToCartServlet", urlPatterns = "/api/add-to-cart")
public class AddToCartServlet extends HttpServlet {
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

        // Step 1: Extract JWT token and validate
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
            responseJsonObject.addProperty("message", "Invalid JWT token.");
            out.write(responseJsonObject.toString());
            return;
        }

        int customerId = (int) claims.get("userId");
        String movieId = request.getParameter("movieId");

        if (movieId == null || movieId.trim().isEmpty()) {
            response.setStatus(400);
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Missing movieId.");
            out.write(responseJsonObject.toString());
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            String insertQuery = "INSERT INTO carts (customerId, movieId, quantity) VALUES (?, ?, 1) " +
                    "ON DUPLICATE KEY UPDATE quantity = quantity + 1";

            PreparedStatement stmt = conn.prepareStatement(insertQuery);
            stmt.setInt(1, customerId);
            stmt.setString(2, movieId);
            int rowsUpdated = stmt.executeUpdate();
            stmt.close();

            responseJsonObject.addProperty("status", "success");
            responseJsonObject.addProperty("message", "Movie added to cart.");
            out.write(responseJsonObject.toString());
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Database error: " + e.getMessage());
            out.write(responseJsonObject.toString());
        } finally {
            out.close();
        }
    }
}

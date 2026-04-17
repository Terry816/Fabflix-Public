import com.google.gson.JsonArray;
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
import java.sql.ResultSet;

@WebServlet(name = "ShoppingCartServlet", urlPatterns = "/api/shopping-cart")
public class ShoppingCartServlet extends HttpServlet {
    private DataSource dataSource;

    public void init() {
        try {
            InitialContext ctx = new InitialContext();
            dataSource = (DataSource) ctx.lookup("java:comp/env/jdbc/readDB");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        JsonObject responseJsonObject = new JsonObject();

        // Step 1: Get token from cookie
        String jwtToken = JwtUtil.getCookieValue(request, "jwtToken");
        if (jwtToken == null) {
            response.setStatus(401);
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Missing JWT token.");
            out.write(responseJsonObject.toString());
            return;
        }

        // Step 2: Validate and parse claims
        Claims claims = JwtUtil.validateToken(jwtToken);
        if (claims == null) {
            response.setStatus(401);
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Invalid or expired JWT token.");
            out.write(responseJsonObject.toString());
            return;
        }

        int customerId = (int) claims.get("userId");

        try (Connection conn = dataSource.getConnection()) {
            String query = "SELECT c.movieId, m.title, m.year, m.director, r.rating, c.quantity " +
                    "FROM carts c " +
                    "JOIN movies m ON c.movieId = m.id " +
                    "LEFT JOIN ratings r ON m.id = r.movieId " +
                    "WHERE c.customerId = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, customerId);
            ResultSet rs = stmt.executeQuery();

            JsonArray cartItems = new JsonArray();

            while (rs.next()) {
                JsonObject item = new JsonObject();
                item.addProperty("movieId", rs.getString("movieId"));
                item.addProperty("title", rs.getString("title"));
                item.addProperty("year", rs.getInt("year"));
                item.addProperty("director", rs.getString("director"));
                item.addProperty("rating", rs.getFloat("rating"));
                item.addProperty("quantity", rs.getInt("quantity"));
                cartItems.add(item);
            }

            rs.close();
            stmt.close();

            responseJsonObject.addProperty("status", "success");
            responseJsonObject.add("cartItems", cartItems);
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

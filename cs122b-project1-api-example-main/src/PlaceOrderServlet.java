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
import java.sql.*;
import java.util.Date;

@WebServlet(name = "PlaceOrderServlet", urlPatterns = "/api/place-order")
public class PlaceOrderServlet extends HttpServlet {
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

        // ✅ Extract JWT token from cookies
        String jwtToken = JwtUtil.getCookieValue(request, "jwtToken");
        if (jwtToken == null) {
            response.setStatus(401);
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Missing JWT token.");
            out.write(responseJsonObject.toString());
            return;
        }

        // ✅ Validate token
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
            conn.setAutoCommit(false);

            // Get cart items
            String getCartQuery = "SELECT movieId, quantity FROM carts WHERE customerId = ?";
            PreparedStatement getCartStmt = conn.prepareStatement(getCartQuery);
            getCartStmt.setInt(1, customerId);
            ResultSet cartRs = getCartStmt.executeQuery();

            boolean hasItems = false;
            while (cartRs.next()) {
                hasItems = true;
                String movieId = cartRs.getString("movieId");
                int quantity = cartRs.getInt("quantity");

                String insertSale = "INSERT INTO sales (customerId, movieId, saleDate) VALUES (?, ?, ?)";
                PreparedStatement saleStmt = conn.prepareStatement(insertSale);
                for (int i = 0; i < quantity; i++) {
                    saleStmt.setInt(1, customerId);
                    saleStmt.setString(2, movieId);
                    saleStmt.setDate(3, new java.sql.Date(System.currentTimeMillis())); // ✅ fixed line
                    saleStmt.addBatch();
                }
                saleStmt.executeBatch();
                saleStmt.close();
            }

            cartRs.close();
            getCartStmt.close();

            if (!hasItems) {
                response.setStatus(400);
                responseJsonObject.addProperty("status", "fail");
                responseJsonObject.addProperty("message", "Cart is empty.");
                out.write(responseJsonObject.toString());
                return;
            }

            // Clear cart
            String clearCart = "DELETE FROM carts WHERE customerId = ?";
            PreparedStatement clearStmt = conn.prepareStatement(clearCart);
            clearStmt.setInt(1, customerId);
            clearStmt.executeUpdate();
            clearStmt.close();

            conn.commit();

            responseJsonObject.addProperty("status", "success");
            responseJsonObject.addProperty("message", "Order placed successfully.");

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Server error: " + e.getMessage());
        }

        out.write(responseJsonObject.toString());
        out.close();
    }
}

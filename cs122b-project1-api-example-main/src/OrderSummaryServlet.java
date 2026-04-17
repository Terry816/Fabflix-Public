import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.jsonwebtoken.Claims;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
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

@WebServlet(name = "OrderSummaryServlet", urlPatterns = "/api/order-summary")
public class OrderSummaryServlet extends HttpServlet {
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
        JsonObject responseJson = new JsonObject();

        try {
            // 🔐 Read JWT from cookie
            String token = JwtUtil.getCookieValue(request, "jwtToken");
            Claims claims = JwtUtil.validateToken(token);
            if (claims == null) {
                responseJson.addProperty("status", "fail");
                responseJson.addProperty("message", "Invalid or expired token.");
                out.write(responseJson.toString());
                return;
            }

            int userId = (int) claims.get("userId");

            try (Connection conn = dataSource.getConnection()) {
                // 🔍 Get latest saleId for this user
                String getLatestSale = "SELECT MAX(id) AS latest_id FROM sales WHERE customerId = ?";
                PreparedStatement stmt = conn.prepareStatement(getLatestSale);
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();

                int latestSaleId = -1;
                if (rs.next()) {
                    latestSaleId = rs.getInt("latest_id");
                }
                rs.close();
                stmt.close();

                // ⛔ No sale found
                if (latestSaleId == -1) {
                    responseJson.addProperty("status", "fail");
                    responseJson.addProperty("message", "No recent sale found.");
                    out.write(responseJson.toString());
                    return;
                }

                // 🛒 Get sale details
                String saleDetailsQuery = "SELECT s.movieId, m.title, s.quantity " +
                        "FROM sales s JOIN movies m ON s.movieId = m.id " +
                        "WHERE s.customerId = ? AND s.id = ?";
                stmt = conn.prepareStatement(saleDetailsQuery);
                stmt.setInt(1, userId);
                stmt.setInt(2, latestSaleId);
                rs = stmt.executeQuery();

                JsonArray items = new JsonArray();
                while (rs.next()) {
                    JsonObject item = new JsonObject();
                    item.addProperty("movieId", rs.getString("movieId"));
                    item.addProperty("title", rs.getString("title"));
                    item.addProperty("quantity", rs.getInt("quantity"));
                    items.add(item);
                }

                responseJson.addProperty("status", "success");
                responseJson.add("cartItems", items);

                rs.close();
                stmt.close();
            }
        } catch (Exception e) {
            responseJson.addProperty("status", "fail");
            responseJson.addProperty("message", "Error loading order summary.");
        }

        out.write(responseJson.toString());
        out.close();
    }
}

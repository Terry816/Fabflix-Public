import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "OrderSummaryServlet", urlPatterns = "/api/order-summary")
public class OrderSummaryServlet extends AuthenticatedServlet {
    private javax.sql.DataSource dataSource;

    public void init() throws jakarta.servlet.ServletException {
        dataSource = lookupDataSource(READ_DB_JNDI);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Integer userId = getAuthenticatedUserId(request, response);
        if (userId == null) {
            return;
        }

        JsonObject responseJson = new JsonObject();
        try (Connection conn = dataSource.getConnection()) {
            String latestSaleDateQuery = "SELECT MAX(saleDate) AS latest_sale_date FROM sales WHERE customerId = ?";
            java.sql.Date latestSaleDate;
            try (PreparedStatement stmt = conn.prepareStatement(latestSaleDateQuery)) {
                stmt.setInt(1, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next() || rs.getDate("latest_sale_date") == null) {
                        responseJson.addProperty("status", "fail");
                        responseJson.addProperty("message", "No recent sale found.");
                        writeJson(response, responseJson);
                        return;
                    }
                    latestSaleDate = rs.getDate("latest_sale_date");
                }
            }

            String saleDetailsQuery = "SELECT s.movieId AS movieId, m.title, COUNT(*) AS quantity " +
                    "FROM sales s JOIN movies m ON s.movieId = m.id " +
                    "WHERE s.customerId = ? AND s.saleDate = ? " +
                    "GROUP BY s.movieId, m.title ORDER BY m.title ASC";
            JsonArray items = new JsonArray();
            try (PreparedStatement stmt = conn.prepareStatement(saleDetailsQuery)) {
                stmt.setInt(1, userId);
                stmt.setDate(2, latestSaleDate);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        JsonObject item = new JsonObject();
                        item.addProperty("movieId", rs.getString("movieId"));
                        item.addProperty("title", rs.getString("title"));
                        item.addProperty("quantity", rs.getInt("quantity"));
                        items.add(item);
                    }
                }
            }

            responseJson.addProperty("status", "success");
            responseJson.add("cartItems", items);
            writeJson(response, responseJson);
        } catch (Exception e) {
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error loading order summary.");
        }
    }
}

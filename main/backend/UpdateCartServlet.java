import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

@WebServlet(name = "UpdateCartServlet", urlPatterns = "/api/update-cart")
public class UpdateCartServlet extends AuthenticatedServlet {
    private javax.sql.DataSource dataSource;

    public void init() throws jakarta.servlet.ServletException {
        dataSource = lookupDataSource(WRITE_DB_JNDI);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Integer customerId = getAuthenticatedUserId(request, response);
        if (customerId == null) {
            return;
        }

        String movieId = trimToNull(request.getParameter("movieId"));
        if (movieId == null) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing movieId.");
            return;
        }
        int quantity;

        try {
            quantity = Integer.parseInt(request.getParameter("quantity"));
        } catch (NumberFormatException e) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid quantity.");
            return;
        }

        if (quantity < 0) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Quantity cannot be negative.");
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            if (quantity == 0) {
                String deleteQuery = "DELETE FROM carts WHERE customerId = ? AND movieId = ?";
                try (PreparedStatement stmt = conn.prepareStatement(deleteQuery)) {
                    stmt.setInt(1, customerId);
                    stmt.setString(2, movieId);
                    stmt.executeUpdate();
                }
            } else {
                String updateQuery = "INSERT INTO carts (customerId, movieId, quantity) VALUES (?, ?, ?) " +
                        "ON CONFLICT (customerId, movieId) DO UPDATE SET quantity = EXCLUDED.quantity";
                try (PreparedStatement stmt = conn.prepareStatement(updateQuery)) {
                    stmt.setInt(1, customerId);
                    stmt.setString(2, movieId);
                    stmt.setInt(3, quantity);
                    stmt.executeUpdate();
                }
            }

            JsonObject responseJsonObject = new JsonObject();
            responseJsonObject.addProperty("status", "success");
            responseJsonObject.addProperty("message", "Cart updated.");
            responseJsonObject.addProperty("movieId", movieId);
            responseJsonObject.addProperty("quantity", quantity);
            writeJson(response, responseJsonObject);
        } catch (Exception e) {
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
        }
    }
}

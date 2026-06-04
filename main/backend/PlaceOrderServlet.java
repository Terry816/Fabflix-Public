import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebServlet(name = "PlaceOrderServlet", urlPatterns = "/api/place-order")
public class PlaceOrderServlet extends AuthenticatedServlet {
    private javax.sql.DataSource dataSource;

    public void init() throws jakarta.servlet.ServletException {
        dataSource = lookupDataSource(WRITE_DB_JNDI);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Integer customerId = getAuthenticatedUserId(request, response);
        if (customerId == null) {
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try {
                boolean hasItems = false;
                String getCartQuery = "SELECT movieId, quantity FROM carts WHERE customerId = ?";
                try (PreparedStatement getCartStmt = conn.prepareStatement(getCartQuery)) {
                    getCartStmt.setInt(1, customerId);
                    try (ResultSet cartRs = getCartStmt.executeQuery()) {
                        while (cartRs.next()) {
                            hasItems = true;
                            String movieId = cartRs.getString("movieId");
                            int quantity = cartRs.getInt("quantity");

                            String insertSale = "INSERT INTO sales (customerId, movieId, saleDate) VALUES (?, ?, ?)";
                            try (PreparedStatement saleStmt = conn.prepareStatement(insertSale)) {
                                for (int i = 0; i < quantity; i++) {
                                    saleStmt.setInt(1, customerId);
                                    saleStmt.setString(2, movieId);
                                    saleStmt.setDate(3, new java.sql.Date(System.currentTimeMillis()));
                                    saleStmt.addBatch();
                                }
                                saleStmt.executeBatch();
                            }
                        }
                    }
                }

                if (!hasItems) {
                    conn.rollback();
                    writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Cart is empty.");
                    return;
                }

                String clearCart = "DELETE FROM carts WHERE customerId = ?";
                try (PreparedStatement clearStmt = conn.prepareStatement(clearCart)) {
                    clearStmt.setInt(1, customerId);
                    clearStmt.executeUpdate();
                }

                conn.commit();

                JsonObject responseJsonObject = new JsonObject();
                responseJsonObject.addProperty("status", "success");
                responseJsonObject.addProperty("message", "Order placed successfully.");
                writeJson(response, responseJsonObject);
            } catch (Exception e) {
                rollbackQuietly(conn);
                throw e;
            }
        } catch (Exception e) {
            JsonObject responseJsonObject = new JsonObject();
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Server error: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeJson(response, responseJsonObject);
        }
    }

    private void rollbackQuietly(Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException ignored) {
        }
    }
}

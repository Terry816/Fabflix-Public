import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

@WebServlet(name = "AddToCartServlet", urlPatterns = "/api/add-to-cart")
public class AddToCartServlet extends AuthenticatedServlet {
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

        try (Connection conn = dataSource.getConnection()) {
            String insertQuery = "INSERT INTO carts (customerId, movieId, quantity) VALUES (?, ?, 1) " +
                    "ON CONFLICT (customerId, movieId) DO UPDATE SET quantity = carts.quantity + 1";

            JsonObject body = new JsonObject();
            body.addProperty("status", "success");
            body.addProperty("message", "Movie added to cart.");
            body.addProperty("movieId", movieId);

            try (PreparedStatement stmt = conn.prepareStatement(insertQuery)) {
                stmt.setInt(1, customerId);
                stmt.setString(2, movieId);
                stmt.executeUpdate();
            }

            writeJson(response, body);
        } catch (Exception e) {
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }
}

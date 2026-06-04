import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "ShoppingCartServlet", urlPatterns = "/api/shopping-cart")
public class ShoppingCartServlet extends AuthenticatedServlet {
    private javax.sql.DataSource dataSource;

    public void init() throws jakarta.servlet.ServletException {
        dataSource = lookupDataSource(READ_DB_JNDI);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Integer customerId = getAuthenticatedUserId(request, response);
        if (customerId == null) {
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            String query = "SELECT c.movieId AS movieId, m.title, m.year, m.director, r.rating, c.quantity " +
                    "FROM carts c " +
                    "JOIN movies m ON c.movieId = m.id " +
                    "LEFT JOIN ratings r ON m.id = r.movieId " +
                    "WHERE c.customerId = ?";
            JsonArray cartItems = new JsonArray();
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, customerId);
                try (ResultSet rs = stmt.executeQuery()) {
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
                }
            }

            JsonObject responseJsonObject = new JsonObject();
            responseJsonObject.addProperty("status", "success");
            responseJsonObject.add("cartItems", cartItems);
            writeJson(response, responseJsonObject);
        } catch (Exception e) {
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
        }
    }
}

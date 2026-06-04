import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "AutocompleteServlet", urlPatterns = "/api/autocomplete")
public class AutocompleteServlet extends BaseServlet {
    private javax.sql.DataSource dataSource;

    public void init() throws jakarta.servlet.ServletException {
        dataSource = lookupDataSource(READ_DB_JNDI);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String query = trimToNull(request.getParameter("query"));
        JsonArray suggestions = new JsonArray();

        if (query == null || query.length() < 3) {
            writeJson(response, suggestions);
            return;
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, title FROM movies WHERE title ILIKE ? ORDER BY title LIMIT 10")) {
            ps.setString(1, "%" + query + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JsonObject suggestion = new JsonObject();
                    suggestion.addProperty("value", rs.getString("title"));

                    JsonObject data = new JsonObject();
                    data.addProperty("movieId", rs.getString("id"));
                    suggestion.add("data", data);

                    suggestions.add(suggestion);
                }
            }
        } catch (Exception e) {
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            return;
        }

        writeJson(response, suggestions);
    }
}

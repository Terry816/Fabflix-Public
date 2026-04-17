import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "AutocompleteServlet", urlPatterns = "/api/autocomplete")
public class AutocompleteServlet extends HttpServlet {
    private DataSource dataSource;

    public void init() {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/readDB");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        String query = request.getParameter("query");

        JsonArray suggestions = new JsonArray();

        if (query == null || query.trim().length() < 3) {
            System.out.println("❌ Query is null or too short: '" + query + "'");
            response.getWriter().write(suggestions.toString());
            return;
        }

        System.out.println("🔍 Autocomplete query received: " + query);
        int suggestionCount = 0;

        try (Connection conn = dataSource.getConnection()) {

            // Option A: Use MATCH (Full-Text Search)
            String[] tokens = query.trim().split("\\s+");
            StringBuilder matchQuery = new StringBuilder();
            for (String token : tokens) {
                matchQuery.append("+").append(token).append("* ");
            }
            String finalMatchQuery = matchQuery.toString().trim();

            // Log the query
            System.out.println("🧠 Using MATCH AGAINST query: " + finalMatchQuery);

            String sql = "SELECT id, title FROM movies WHERE MATCH(title) AGAINST(? IN BOOLEAN MODE) LIMIT 10";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, finalMatchQuery);

            // 👉 UNCOMMENT this block below to temporarily switch to LIKE for debugging:
            /*
            System.out.println("🧪 Fallback to LIKE query: %" + query.trim() + "%");
            String sql = "SELECT id, title FROM movies WHERE title LIKE ? LIMIT 10";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, "%" + query.trim() + "%");
            */

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String title = rs.getString("title");
                String movieId = rs.getString("id");

                if (title != null && movieId != null) {
                    JsonObject suggestion = new JsonObject();
                    suggestion.addProperty("value", title);

                    JsonObject data = new JsonObject();
                    data.addProperty("movieId", movieId);
                    suggestion.add("data", data);

                    suggestions.add(suggestion);
                    suggestionCount++;
                }
            }

            System.out.println("✅ Suggestions returned: " + suggestionCount);

            rs.close();
            ps.close();
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("errorMessage", e.getMessage());
            suggestions.add(error);
            System.err.println("❌ Error in AutocompleteServlet: " + e.getMessage());
        }

        response.getWriter().write(suggestions.toString());
    }
}

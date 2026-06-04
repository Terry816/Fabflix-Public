import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "SingleStarServlet", urlPatterns = "/api/single-star")
public class SingleStarServlet extends BaseServlet {
    private javax.sql.DataSource dataSource;

    public void init() throws jakarta.servlet.ServletException {
        dataSource = lookupDataSource(READ_DB_JNDI);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        String id = trimToNull(request.getParameter("id"));
        if (id == null) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing star id.");
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            String query =
                    "SELECT s.name AS star_name, s.birthYear, m.id AS movie_id, m.title, m.year, m.director " +
                            "FROM stars s " +
                            "JOIN stars_in_movies sim ON s.id = sim.starId " +
                            "JOIN movies m ON sim.movieId = m.id " +
                            "WHERE s.id = ? " +
                            "ORDER BY m.year DESC, m.title ASC";

            JsonArray jsonArray = new JsonArray();
            try (PreparedStatement statement = conn.prepareStatement(query)) {
                statement.setString(1, id);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        JsonObject obj = new JsonObject();
                        obj.addProperty("star_name", rs.getString("star_name"));
                        obj.addProperty("birthYear", rs.getObject("birthYear") == null ? null : rs.getInt("birthYear"));
                        obj.addProperty("movie_id", rs.getString("movie_id"));
                        obj.addProperty("movie_title", rs.getString("title"));
                        obj.addProperty("movie_year", rs.getInt("year"));
                        obj.addProperty("director", rs.getString("director"));
                        jsonArray.add(obj);
                    }
                }
            }

            writeJson(response, jsonArray);
            response.setStatus(200);
        } catch (Exception e) {
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}

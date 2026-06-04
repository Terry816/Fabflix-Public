import com.google.gson.JsonArray;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "SingleMovieServlet", urlPatterns = "/api/single-movie")
public class SingleMovieServlet extends BaseServlet {
    private javax.sql.DataSource dataSource;

    public void init() throws jakarta.servlet.ServletException {
        dataSource = lookupDataSource(READ_DB_JNDI);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        disableCaching(response);
        String id = trimToNull(request.getParameter("id"));
        if (id == null) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing movie id.");
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            String query =
                    "SELECT m.id AS movie_id, m.title, m.year, m.director, r.rating, " +
                            "STRING_AGG(DISTINCT g.name, ', ' ORDER BY g.name ASC) AS genres, " +
                            "STRING_AGG(DISTINCT s.name, ', ' ORDER BY s.name ASC) AS stars, " +
                            "STRING_AGG(DISTINCT s.id, ',' ORDER BY s.id ASC) AS star_ids " +
                            "FROM movies m " +
                            "JOIN ratings r ON m.id = r.movieId " +
                            "LEFT JOIN genres_in_movies gm ON m.id = gm.movieId " +
                            "LEFT JOIN genres g ON gm.genreId = g.id " +
                            "LEFT JOIN stars_in_movies sm ON m.id = sm.movieId " +
                            "LEFT JOIN ( " +
                            "   SELECT s.id, s.name, COUNT(sim.movieId) AS star_count " +
                            "   FROM stars s " +
                            "   JOIN stars_in_movies sim ON s.id = sim.starId " +
                            "   GROUP BY s.id, s.name " +
                            ") AS s ON sm.starId = s.id " +
                            "WHERE m.id = ? " +
                            "GROUP BY m.id, m.title, m.year, m.director, r.rating";

            JsonArray jsonArray = new JsonArray();
            try (PreparedStatement statement = conn.prepareStatement(query)) {
                statement.setString(1, id);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        jsonArray.add(MovieJsonMapper.fromResultSet(rs));
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

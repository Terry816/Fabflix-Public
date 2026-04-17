import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "SingleMovieServlet", urlPatterns = "/api/single-movie")
public class SingleMovieServlet extends HttpServlet {
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/writeDB");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        String id = request.getParameter("id");

        try (Connection conn = dataSource.getConnection()) {
            String query =
                    "SELECT m.id AS movie_id, m.title, m.year, m.director, r.rating, " +
                            "GROUP_CONCAT(DISTINCT g.name ORDER BY g.name ASC SEPARATOR ', ') AS genres, " +
                            "GROUP_CONCAT(DISTINCT s.name ORDER BY star_count DESC, s.name ASC SEPARATOR ', ') AS stars, " +
                            "GROUP_CONCAT(DISTINCT s.id ORDER BY star_count DESC, s.name ASC SEPARATOR ',') AS star_ids " +
                            "FROM movies m " +
                            "JOIN ratings r ON m.id = r.movieId " +
                            "LEFT JOIN genres_in_movies gm ON m.id = gm.movieId " +
                            "LEFT JOIN genres g ON gm.genreId = g.id " +
                            "LEFT JOIN stars_in_movies sm ON m.id = sm.movieId " +
                            "LEFT JOIN ( " +
                            "   SELECT s.id, s.name, COUNT(sim.movieId) AS star_count " +
                            "   FROM stars s " +
                            "   JOIN stars_in_movies sim ON s.id = sim.starId " +
                            "   GROUP BY s.id " +
                            ") AS s ON sm.starId = s.id " +
                            "WHERE m.id = ? " +
                            "GROUP BY m.id";

            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, id);
            ResultSet rs = statement.executeQuery();

            JsonArray jsonArray = new JsonArray();
            while (rs.next()) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("movie_id", rs.getString("movie_id"));
                jsonObject.addProperty("title", rs.getString("title"));
                jsonObject.addProperty("year", rs.getInt("year"));
                jsonObject.addProperty("director", rs.getString("director"));
                jsonObject.addProperty("rating", rs.getFloat("rating"));
                jsonObject.addProperty("genres", rs.getString("genres"));
                jsonObject.addProperty("stars", rs.getString("stars"));
                jsonObject.addProperty("star_ids", rs.getString("star_ids"));
                jsonArray.add(jsonObject);
            }

            rs.close();
            statement.close();
            out.write(jsonArray.toString());
            response.setStatus(200);
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("errorMessage", e.getMessage());
            out.write(error.toString());
            response.setStatus(500);
        } finally {
            out.close();
        }
    }
}

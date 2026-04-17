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
import java.util.*;

@WebServlet(name = "SearchServlet", urlPatterns = "/api/search")
public class SearchServlet extends HttpServlet {
    private DataSource dataSource;

    public void init() {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/writeDB");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        boolean isTop20 = "true".equals(request.getParameter("top20"));
        String title = request.getParameter("title");
        String year = request.getParameter("year");
        String director = request.getParameter("director");
        String star = request.getParameter("star");
        String genre = request.getParameter("genre");
        String startsWith = request.getParameter("startsWith");

        String sort1 = request.getParameter("sort1");
        String dir1 = request.getParameter("dir1");
        String sort2 = request.getParameter("sort2");
        String dir2 = request.getParameter("dir2");

        int page = Integer.parseInt(Optional.ofNullable(request.getParameter("page")).orElse("1"));
        int pageSize = Integer.parseInt(Optional.ofNullable(request.getParameter("pageSize")).orElse("10"));
        int offset = (page - 1) * pageSize;

        if (sort1 == null || dir1 == null || sort2 == null || dir2 == null) {
            sort1 = "title";
            dir1 = "asc";
            sort2 = "rating";
            dir2 = "asc";
        }

        List<String> validSortFields = List.of("title", "rating");
        List<String> validSortDirs = List.of("asc", "desc");

        if (!validSortFields.contains(sort1)) sort1 = "title";
        if (!validSortFields.contains(sort2)) sort2 = "rating";
        if (!validSortDirs.contains(dir1)) dir1 = "asc";
        if (!validSortDirs.contains(dir2)) dir2 = "asc";

        JsonArray resultArray = new JsonArray();

        try (Connection conn = dataSource.getConnection()) {
            StringBuilder query = new StringBuilder(
                    "WITH star_popularity AS (" +
                            "    SELECT s.id, s.name, COUNT(sim.movieId) AS star_count " +
                            "    FROM stars s " +
                            "    JOIN stars_in_movies sim ON s.id = sim.starId " +
                            "    GROUP BY s.id " +
                            "), top_stars AS (" +
                            "    SELECT sim.movieId, " +
                            "           GROUP_CONCAT(sp.name ORDER BY sp.star_count DESC, sp.name ASC SEPARATOR ', ') AS stars, " +
                            "           GROUP_CONCAT(sp.id ORDER BY sp.star_count DESC, sp.name ASC SEPARATOR ',') AS star_ids " +
                            "    FROM stars_in_movies sim " +
                            "    JOIN star_popularity sp ON sim.starId = sp.id " +
                            "    GROUP BY sim.movieId " +
                            ") " +
                            "SELECT m.id AS movie_id, m.title, m.year, m.director, r.rating, " +
                            "       GROUP_CONCAT(DISTINCT g.name ORDER BY g.name ASC SEPARATOR ', ') AS genres, " +
                            "       ts.stars, ts.star_ids " +
                            "FROM movies m " +
                            "LEFT JOIN ratings r ON m.id = r.movieId " +
                            "LEFT JOIN genres_in_movies gm ON m.id = gm.movieId " +
                            "LEFT JOIN genres g ON gm.genreId = g.id " +
                            "LEFT JOIN top_stars ts ON m.id = ts.movieId " +
                            "WHERE 1=1 ");

            ArrayList<String> params = new ArrayList<>();

            if (!isTop20) {
                if (title != null && !title.isEmpty()) {
                    query.append("AND m.title LIKE ? ");
                    params.add("%" + title + "%");
                }
                if (year != null && !year.isEmpty()) {
                    query.append("AND m.year = ? ");
                    params.add(year);
                }
                if (director != null && !director.isEmpty()) {
                    query.append("AND m.director LIKE ? ");
                    params.add("%" + director + "%");
                }
                if (star != null && !star.isEmpty()) {
                    query.append("AND EXISTS (SELECT 1 FROM stars_in_movies sim JOIN stars s ON sim.starId = s.id WHERE sim.movieId = m.id AND s.name LIKE ?) ");
                    params.add("%" + star + "%");
                }
                if (genre != null && !genre.isEmpty()) {
                    query.append("AND EXISTS (SELECT 1 FROM genres_in_movies gm2 JOIN genres g2 ON gm2.genreId = g2.id WHERE gm2.movieId = m.id AND g2.name = ?) ");
                    params.add(genre);
                }
                if (startsWith != null && !startsWith.isEmpty()) {
                    if (startsWith.equals("*")) {
                        query.append("AND m.title REGEXP '^[^a-zA-Z0-9]' ");
                    } else {
                        query.append("AND m.title LIKE ? ");
                        params.add(startsWith + "%");
                    }
                }
            }

            query.append("GROUP BY m.id ");
            if (isTop20) {
                query.append("ORDER BY r.rating DESC LIMIT 20");
            } else {
                query.append("ORDER BY " + sort1 + " " + dir1 + ", " + sort2 + " " + dir2 + " LIMIT ? OFFSET ? ");
            }

            PreparedStatement statement = conn.prepareStatement(query.toString());
            int index = 1;
            for (String param : params) {
                statement.setString(index++, param);
            }
            if (!isTop20) {
                statement.setInt(index++, pageSize);
                statement.setInt(index, offset);
            }

            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                JsonObject movie = new JsonObject();
                movie.addProperty("movie_id", rs.getString("movie_id"));
                movie.addProperty("title", rs.getString("title"));
                movie.addProperty("year", rs.getInt("year"));
                movie.addProperty("director", rs.getString("director"));
                movie.addProperty("rating", rs.getFloat("rating"));
                movie.addProperty("genres", rs.getString("genres"));
                movie.addProperty("stars", rs.getString("stars"));
                movie.addProperty("star_ids", rs.getString("star_ids"));
                resultArray.add(movie);
            }

            rs.close();
            statement.close();
            response.getWriter().write(resultArray.toString());

        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("errorMessage", e.getMessage());
            response.getWriter().write(error.toString());
            response.setStatus(500);
        }
    }
}
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
import java.io.PrintWriter;
import java.sql.*;

@WebServlet(name = "FullTextSearchServlet", urlPatterns = "/api/fulltext-search")
public class FullTextSearchServlet extends HttpServlet {
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
        PrintWriter out = response.getWriter();

        String rawQuery = request.getParameter("q");
        if (rawQuery == null || rawQuery.trim().isEmpty()) {
            out.write("[]");
            return;
        }

        String[] tokens = rawQuery.trim().split("\\s+");
        StringBuilder matchQuery = new StringBuilder();
        for (String token : tokens) {
            matchQuery.append("+").append(token).append("* ");
        }

        int page = Integer.parseInt(request.getParameter("page"));
        int pageSize = Integer.parseInt(request.getParameter("pageSize"));
        int offset = (page - 1) * pageSize;

        String sort1 = request.getParameter("sort1");
        String dir1 = request.getParameter("dir1");
        String sort2 = request.getParameter("sort2");
        String dir2 = request.getParameter("dir2");

        // Safety: fallback to valid values
        sort1 = sanitizeSortField(sort1);
        dir1 = sanitizeSortDirection(dir1);
        sort2 = sanitizeSortField(sort2);
        dir2 = sanitizeSortDirection(dir2);

        String orderClause = "ORDER BY " + sort1 + " " + dir1 + ", " + sort2 + " " + dir2;

        String sql = "SELECT m.id AS movie_id, m.title, m.year, m.director, r.rating, " +
                "GROUP_CONCAT(DISTINCT g.name ORDER BY g.name SEPARATOR ', ') AS genres, " +
                "GROUP_CONCAT(DISTINCT s.name ORDER BY s.name SEPARATOR ', ') AS stars, " +
                "GROUP_CONCAT(DISTINCT s.id ORDER BY s.name SEPARATOR ',') AS star_ids " +
                "FROM movies m " +
                "JOIN ratings r ON m.id = r.movieId " +
                "LEFT JOIN genres_in_movies gim ON m.id = gim.movieId " +
                "LEFT JOIN genres g ON gim.genreId = g.id " +
                "LEFT JOIN stars_in_movies sim ON m.id = sim.movieId " +
                "LEFT JOIN stars s ON sim.starId = s.id " +
                "WHERE MATCH(m.title) AGAINST(? IN BOOLEAN MODE) " +
                "GROUP BY m.id, m.title, m.year, m.director, r.rating " +
                orderClause + " LIMIT ? OFFSET ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, matchQuery.toString().trim());
            ps.setInt(2, pageSize);
            ps.setInt(3, offset);

            ResultSet rs = ps.executeQuery();
            JsonArray results = new JsonArray();

            while (rs.next()) {
                JsonObject movie = new JsonObject();
                movie.addProperty("movie_id", rs.getString("movie_id"));
                movie.addProperty("title", rs.getString("title"));
                movie.addProperty("year", rs.getInt("year"));
                movie.addProperty("director", rs.getString("director"));
                movie.addProperty("rating", rs.getFloat("rating"));

                String genres = rs.getString("genres");
                movie.addProperty("genres", genres != null ? genres : "");

                String stars = rs.getString("stars");
                movie.addProperty("stars", stars != null ? stars : "");

                String starIds = rs.getString("star_ids");
                movie.addProperty("star_ids", starIds != null ? starIds : "");

                results.add(movie);
            }

            out.write(results.toString());
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("errorMessage", e.getMessage());
            out.write(error.toString());
            e.printStackTrace();
        } finally {
            out.close();
        }
    }

    private String sanitizeSortField(String field) {
        if (field == null) return "title";
        switch (field.toLowerCase()) {
            case "title": case "rating": return field;
            default: return "title";
        }
    }

    private String sanitizeSortDirection(String dir) {
        if (dir == null) return "asc";
        return dir.equalsIgnoreCase("desc") ? "desc" : "asc";
    }
}
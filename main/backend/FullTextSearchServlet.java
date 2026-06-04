import com.google.gson.JsonArray;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "FullTextSearchServlet", urlPatterns = "/api/fulltext-search")
public class FullTextSearchServlet extends BaseServlet {
    private javax.sql.DataSource dataSource;

    public void init() throws jakarta.servlet.ServletException {
        dataSource = lookupDataSource(READ_DB_JNDI);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        String rawQuery = trimToNull(request.getParameter("q"));
        if (rawQuery == null) {
            writeJson(response, new JsonArray());
            return;
        }

        int page = Math.max(1, getIntParameter(request.getParameter("page"), 1));
        int pageSize = Math.max(1, getIntParameter(request.getParameter("pageSize"), 10));
        int offset = (page - 1) * pageSize;

        String sort1 = MovieSearchSupport.sanitizeSortField(request.getParameter("sort1"), "title");
        String dir1 = MovieSearchSupport.sanitizeSortDirection(request.getParameter("dir1"), "asc");
        String sort2 = MovieSearchSupport.sanitizeSortField(request.getParameter("sort2"), "rating");
        String dir2 = MovieSearchSupport.sanitizeSortDirection(request.getParameter("dir2"), "asc");
        String orderClause = MovieSearchSupport.buildOrderClause(sort1, dir1, sort2, dir2);

        String sql = "SELECT m.id AS movie_id, m.title, m.year, m.director, r.rating, " +
                "STRING_AGG(DISTINCT g.name, ', ' ORDER BY g.name) AS genres, " +
                "STRING_AGG(DISTINCT s.name, ', ' ORDER BY s.name) AS stars, " +
                "STRING_AGG(DISTINCT s.id, ',' ORDER BY s.id) AS star_ids " +
                "FROM movies m " +
                "JOIN ratings r ON m.id = r.movieId " +
                "LEFT JOIN genres_in_movies gim ON m.id = gim.movieId " +
                "LEFT JOIN genres g ON gim.genreId = g.id " +
                "LEFT JOIN stars_in_movies sim ON m.id = sim.movieId " +
                "LEFT JOIN stars s ON sim.starId = s.id " +
                "WHERE m.title ILIKE ? " +
                "GROUP BY m.id, m.title, m.year, m.director, r.rating " +
                orderClause + " LIMIT ? OFFSET ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + rawQuery + "%");
            ps.setInt(2, pageSize);
            ps.setInt(3, offset);

            JsonArray results = new JsonArray();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(MovieJsonMapper.fromResultSet(rs));
                }
            }

            writeJson(response, results);
        } catch (Exception e) {
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}

import com.google.gson.JsonArray;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

@WebServlet(name = "SearchServlet", urlPatterns = "/api/search")
public class SearchServlet extends BaseServlet {
    private javax.sql.DataSource dataSource;

    public void init() throws jakarta.servlet.ServletException {
        dataSource = lookupDataSource(READ_DB_JNDI);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        boolean isTop20 = "true".equals(request.getParameter("top20"));
        String title = trimToNull(request.getParameter("title"));
        String year = trimToNull(request.getParameter("year"));
        String director = trimToNull(request.getParameter("director"));
        String star = trimToNull(request.getParameter("star"));
        String genre = trimToNull(request.getParameter("genre"));
        String startsWith = trimToNull(request.getParameter("startsWith"));

        String sort1 = MovieSearchSupport.sanitizeSortField(request.getParameter("sort1"), "title");
        String dir1 = MovieSearchSupport.sanitizeSortDirection(request.getParameter("dir1"), "asc");
        String sort2 = MovieSearchSupport.sanitizeSortField(request.getParameter("sort2"), "rating");
        String dir2 = MovieSearchSupport.sanitizeSortDirection(request.getParameter("dir2"), "asc");

        int page = Math.max(1, getIntParameter(request.getParameter("page"), 1));
        int pageSize = Math.max(1, getIntParameter(request.getParameter("pageSize"), 10));
        int offset = (page - 1) * pageSize;

        JsonArray resultArray = new JsonArray();

        try (Connection conn = dataSource.getConnection()) {
            StringBuilder query = new StringBuilder(
                    "WITH star_popularity AS (" +
                            "    SELECT s.id, s.name, COUNT(sim.movieId) AS star_count " +
                            "    FROM stars s " +
                            "    JOIN stars_in_movies sim ON s.id = sim.starId " +
                            "    GROUP BY s.id, s.name " +
                            "), top_stars AS (" +
                            "    SELECT sim.movieId, " +
                            "           STRING_AGG(sp.name, ', ' ORDER BY sp.star_count DESC, sp.name ASC) AS stars, " +
                            "           STRING_AGG(sp.id, ',' ORDER BY sp.star_count DESC, sp.name ASC) AS star_ids " +
                            "    FROM stars_in_movies sim " +
                            "    JOIN star_popularity sp ON sim.starId = sp.id " +
                            "    GROUP BY sim.movieId " +
                            ") " +
                            "SELECT m.id AS movie_id, m.title, m.year, m.director, r.rating, " +
                            "       STRING_AGG(DISTINCT g.name, ', ' ORDER BY g.name ASC) AS genres, " +
                            "       ts.stars, ts.star_ids " +
                            "FROM movies m " +
                            "LEFT JOIN ratings r ON m.id = r.movieId " +
                            "LEFT JOIN genres_in_movies gm ON m.id = gm.movieId " +
                            "LEFT JOIN genres g ON gm.genreId = g.id " +
                            "LEFT JOIN top_stars ts ON m.id = ts.movieId " +
                            "WHERE 1=1 ");

            ArrayList<String> params = new ArrayList<>();

            if (!isTop20) {
                if (title != null) {
                    query.append("AND m.title ILIKE ? ");
                    params.add("%" + title + "%");
                }
                if (year != null) {
                    query.append("AND m.year = ? ");
                    params.add(year);
                }
                if (director != null) {
                    query.append("AND m.director ILIKE ? ");
                    params.add("%" + director + "%");
                }
                if (star != null) {
                    query.append("AND EXISTS (SELECT 1 FROM stars_in_movies sim JOIN stars s ON sim.starId = s.id WHERE sim.movieId = m.id AND s.name ILIKE ?) ");
                    params.add("%" + star + "%");
                }
                if (genre != null) {
                    query.append("AND EXISTS (SELECT 1 FROM genres_in_movies gm2 JOIN genres g2 ON gm2.genreId = g2.id WHERE gm2.movieId = m.id AND g2.name = ?) ");
                    params.add(genre);
                }
                if (startsWith != null) {
                    if (startsWith.equals("*")) {
                        query.append("AND m.title ~ '^[^a-zA-Z0-9]' ");
                    } else {
                        query.append("AND m.title ILIKE ? ");
                        params.add(startsWith + "%");
                    }
                }
            }

            query.append("GROUP BY m.id, m.title, m.year, m.director, r.rating, ts.stars, ts.star_ids ");
            if (isTop20) {
                query.append("ORDER BY r.rating DESC LIMIT 20");
            } else {
                query.append(MovieSearchSupport.buildOrderClause(sort1, dir1, sort2, dir2))
                        .append("LIMIT ? OFFSET ? ");
            }

            try (PreparedStatement statement = conn.prepareStatement(query.toString())) {
                int index = 1;
                for (String param : params) {
                    statement.setString(index++, param);
                }
                if (!isTop20) {
                    statement.setInt(index++, pageSize);
                    statement.setInt(index, offset);
                }

                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        resultArray.add(MovieJsonMapper.fromResultSet(rs));
                    }
                }
            }

            writeJson(response, resultArray);
        } catch (Exception e) {
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}

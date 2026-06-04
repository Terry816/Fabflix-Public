import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@WebServlet(name = "InsertMovieServlet", urlPatterns = "/api/insert-movie")
public class InsertMovieServlet extends BaseServlet {
    private javax.sql.DataSource dataSource;

    public void init() throws jakarta.servlet.ServletException {
        dataSource = lookupDataSource(WRITE_DB_JNDI);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject responseJson = new JsonObject();

        String title = trimToNull(request.getParameter("movie_title"));
        String year = trimToNull(request.getParameter("movie_year"));
        String director = trimToNull(request.getParameter("movie_director"));
        String starName = trimToNull(request.getParameter("star_name"));
        String genreName = trimToNull(request.getParameter("genre_name"));

        if (title == null || year == null || director == null || starName == null || genreName == null) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing required movie fields.");
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (movieExists(conn, title, Integer.parseInt(year), director)) {
                    conn.rollback();
                    responseJson.addProperty("status", "fail");
                    responseJson.addProperty("message", "Duplicate movie detected. Movie was not added.");
                    writeJson(response, responseJson);
                    return;
                }

                String movieId = nextPrefixedId(conn, "movies", "K");
                insertMovie(conn, movieId, title, Integer.parseInt(year), director);
                insertRating(conn, movieId);

                String starId = findStarId(conn, starName);
                if (starId == null) {
                    starId = nextPrefixedId(conn, "stars", "S");
                    insertStar(conn, starId, starName);
                }

                int genreId = findOrInsertGenre(conn, genreName);
                insertMovieStarLink(conn, starId, movieId);
                insertMovieGenreLink(conn, genreId, movieId);
                conn.commit();

                responseJson.addProperty("status", "success");
                responseJson.addProperty("message", "Movie added successfully!");
                responseJson.addProperty("movie_id", movieId);
                responseJson.addProperty("star_id", starId);
                responseJson.addProperty("genre_id", genreId);
            } catch (Exception e) {
                rollbackQuietly(conn);
                throw e;
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            responseJson.addProperty("status", "fail");
            responseJson.addProperty("message", "Internal server error: " + e.getMessage());
        }

        writeJson(response, responseJson);
    }

    private boolean movieExists(Connection conn, String title, int year, String director) throws SQLException {
        String sql = "SELECT 1 FROM movies WHERE title = ? AND year = ? AND director = ? LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, title);
            stmt.setInt(2, year);
            stmt.setString(3, director);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private String nextPrefixedId(Connection conn, String table, String prefix) throws SQLException {
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(id FROM 2) AS INTEGER)), 0) + 1 AS next_id " +
                "FROM " + table + " WHERE id LIKE ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, prefix + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return prefix + rs.getInt("next_id");
            }
        }
    }

    private void insertMovie(Connection conn, String movieId, String title, int year, String director) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO movies (id, title, year, director) VALUES (?, ?, ?, ?)")) {
            stmt.setString(1, movieId);
            stmt.setString(2, title);
            stmt.setInt(3, year);
            stmt.setString(4, director);
            stmt.executeUpdate();
        }
    }

    private void insertRating(Connection conn, String movieId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO ratings (movieId, rating, numVotes) VALUES (?, ?, 0)")) {
            stmt.setString(1, movieId);
            stmt.setFloat(2, Math.round(Math.random() * 100) / 10.0f);
            stmt.executeUpdate();
        }
    }

    private String findStarId(Connection conn, String starName) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT id FROM stars WHERE name = ? LIMIT 1")) {
            stmt.setString(1, starName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("id") : null;
            }
        }
    }

    private void insertStar(Connection conn, String starId, String starName) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO stars (id, name) VALUES (?, ?)")) {
            stmt.setString(1, starId);
            stmt.setString(2, starName);
            stmt.executeUpdate();
        }
    }

    private int findOrInsertGenre(Connection conn, String genreName) throws SQLException {
        try (PreparedStatement findStmt = conn.prepareStatement("SELECT id FROM genres WHERE name = ? LIMIT 1")) {
            findStmt.setString(1, genreName);
            try (ResultSet rs = findStmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }

        try (PreparedStatement insertStmt = conn.prepareStatement(
                "INSERT INTO genres (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            insertStmt.setString(1, genreName);
            insertStmt.executeUpdate();
            try (ResultSet keys = insertStmt.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private void insertMovieStarLink(Connection conn, String starId, String movieId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO stars_in_movies (starId, movieId) VALUES (?, ?) ON CONFLICT DO NOTHING")) {
            stmt.setString(1, starId);
            stmt.setString(2, movieId);
            stmt.executeUpdate();
        }
    }

    private void insertMovieGenreLink(Connection conn, int genreId, String movieId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO genres_in_movies (genreId, movieId) VALUES (?, ?) ON CONFLICT DO NOTHING")) {
            stmt.setInt(1, genreId);
            stmt.setString(2, movieId);
            stmt.executeUpdate();
        }
    }

    private void rollbackQuietly(Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException ignored) {
        }
    }
}

import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "InsertGenreServlet", urlPatterns = "/api/insert-genre")
public class InsertGenreServlet extends BaseServlet {
    private javax.sql.DataSource dataSource;

    public void init() throws jakarta.servlet.ServletException {
        dataSource = lookupDataSource(WRITE_DB_JNDI);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject responseJson = new JsonObject();

        String genreName = trimToNull(request.getParameter("genre_name"));
        if (genreName == null) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing genre name.");
            return;
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement("SELECT id FROM genres WHERE name = ?")) {
            checkStmt.setString(1, genreName);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    responseJson.addProperty("status", "success");
                    responseJson.addProperty("message", "Genre already exists: " + genreName);
                } else {
                    try (PreparedStatement insertStmt = conn.prepareStatement("INSERT INTO genres (name) VALUES (?)")) {
                        insertStmt.setString(1, genreName);
                        if (insertStmt.executeUpdate() > 0) {
                            responseJson.addProperty("status", "success");
                            responseJson.addProperty("message", "Successfully added genre: " + genreName);
                        } else {
                            responseJson.addProperty("status", "fail");
                            responseJson.addProperty("message", "Failed to add genre.");
                        }
                    }
                }
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            responseJson.addProperty("status", "fail");
            responseJson.addProperty("message", "Internal server error: " + e.getMessage());
        }

        writeJson(response, responseJson);
    }
}

import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "InsertStarServlet", urlPatterns = "/api/insert-star")
public class InsertStarServlet extends BaseServlet {
    private javax.sql.DataSource dataSource;

    public void init() throws jakarta.servlet.ServletException {
        dataSource = lookupDataSource(WRITE_DB_JNDI);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject responseJson = new JsonObject();

        String starName = trimToNull(request.getParameter("star_name"));
        String birthYear = trimToNull(request.getParameter("birth_year"));
        if (starName == null) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing star name.");
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            String newId = "nm0000001";
            try (PreparedStatement maxIdStmt = conn.prepareStatement("SELECT MAX(id) AS max_id FROM stars");
                 ResultSet rs = maxIdStmt.executeQuery()) {
                if (rs.next() && rs.getString("max_id") != null) {
                    int num = Integer.parseInt(rs.getString("max_id").substring(2)) + 1;
                    newId = String.format("nm%07d", num);
                }
            }

            try (PreparedStatement insertStmt = conn.prepareStatement("INSERT INTO stars (id, name, birthYear) VALUES (?, ?, ?)")) {
                insertStmt.setString(1, newId);
                insertStmt.setString(2, starName);
                if (birthYear == null) {
                    insertStmt.setNull(3, java.sql.Types.INTEGER);
                } else {
                    insertStmt.setInt(3, Integer.parseInt(birthYear));
                }

                if (insertStmt.executeUpdate() > 0) {
                    responseJson.addProperty("status", "success");
                    responseJson.addProperty("message", "Successfully added star: " + starName);
                    responseJson.addProperty("star_id", newId);
                } else {
                    responseJson.addProperty("status", "fail");
                    responseJson.addProperty("message", "Failed to add star.");
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

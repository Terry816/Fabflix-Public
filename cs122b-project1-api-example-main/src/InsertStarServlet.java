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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "InsertStarServlet", urlPatterns = "/api/insert-star")
public class InsertStarServlet extends HttpServlet {
    private DataSource dataSource;

    public void init() {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/readDB");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        JsonObject responseJson = new JsonObject();

        String starName = request.getParameter("star_name");
        String birthYear = request.getParameter("birth_year");

        try (Connection conn = dataSource.getConnection()) {
            // Generate a new ID for the star
            String maxIdQuery = "SELECT MAX(id) AS max_id FROM stars";
            PreparedStatement maxIdStmt = conn.prepareStatement(maxIdQuery);
            ResultSet rs = maxIdStmt.executeQuery();

            String newId = "nm0000001";
            if (rs.next()) {
                String maxId = rs.getString("max_id"); // e.g., nm1234567
                int num = Integer.parseInt(maxId.substring(2)) + 1;
                newId = String.format("nm%07d", num);
            }
            rs.close();
            maxIdStmt.close();

            String insertQuery = "INSERT INTO stars (id, name, birthYear) VALUES (?, ?, ?)";
            PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
            insertStmt.setString(1, newId);
            insertStmt.setString(2, starName);

            if (birthYear == null || birthYear.isEmpty()) {
                insertStmt.setNull(3, java.sql.Types.INTEGER);
            } else {
                insertStmt.setInt(3, Integer.parseInt(birthYear));
            }

            int rowsInserted = insertStmt.executeUpdate();
            if (rowsInserted > 0) {
                responseJson.addProperty("message", "Successfully added star: " + starName);
            } else {
                responseJson.addProperty("message", "Failed to add star.");
            }

            insertStmt.close();
        } catch (Exception e) {
            responseJson.addProperty("message", "Internal server error: " + e.getMessage());
            response.setStatus(500);
        }

        out.write(responseJson.toString());
        out.close();
    }
}

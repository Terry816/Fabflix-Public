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

@WebServlet(name = "InsertGenreServlet", urlPatterns = "/api/insert-genre")
public class InsertGenreServlet extends HttpServlet {
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

        String genreName = request.getParameter("genre_name");

        try (Connection conn = dataSource.getConnection()) {
            // Check if genre already exists
            String checkQuery = "SELECT id FROM genres WHERE name = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
            checkStmt.setString(1, genreName);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                responseJson.addProperty("message", "Genre already exists: " + genreName);
            } else {
                // Insert new genre
                String insertQuery = "INSERT INTO genres (name) VALUES (?)";
                PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
                insertStmt.setString(1, genreName);

                int rowsInserted = insertStmt.executeUpdate();
                if (rowsInserted > 0) {
                    responseJson.addProperty("message", "Successfully added genre: " + genreName);
                } else {
                    responseJson.addProperty("message", "Failed to add genre.");
                }

                insertStmt.close();
            }

            rs.close();
            checkStmt.close();
        } catch (Exception e) {
            responseJson.addProperty("message", "Internal server error: " + e.getMessage());
            response.setStatus(500);
        }

        out.write(responseJson.toString());
        out.close();
    }
}

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

@WebServlet(name = "GenreServlet", urlPatterns = "/api/genres")
public class GenreServlet extends HttpServlet {
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

        JsonArray genreArray = new JsonArray();

        try (Connection conn = dataSource.getConnection()) {
            String query = "SELECT name FROM genres ORDER BY name ASC";
            PreparedStatement statement = conn.prepareStatement(query);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                JsonObject genre = new JsonObject();
                genre.addProperty("name", rs.getString("name"));
                genreArray.add(genre);
            }

            rs.close();
            statement.close();
            response.getWriter().write(genreArray.toString());

        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("errorMessage", e.getMessage());
            response.getWriter().write(error.toString());
            response.setStatus(500);
        }
    }
}

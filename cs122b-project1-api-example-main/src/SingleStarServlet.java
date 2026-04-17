import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "SingleStarServlet", urlPatterns = "/api/single-star")
public class SingleStarServlet extends HttpServlet {
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/writeDB");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        String id = request.getParameter("id");
        PrintWriter out = response.getWriter();

        try (Connection conn = dataSource.getConnection()) {
            String query =
                    "SELECT s.name AS star_name, s.birthYear, m.id AS movie_id, m.title, m.year, m.director " +
                            "FROM stars s " +
                            "JOIN stars_in_movies sim ON s.id = sim.starId " +
                            "JOIN movies m ON sim.movieId = m.id " +
                            "WHERE s.id = ? " +
                            "ORDER BY m.year DESC, m.title ASC";

            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, id);
            ResultSet rs = statement.executeQuery();

            JsonArray jsonArray = new JsonArray();
            while (rs.next()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("star_name", rs.getString("star_name"));
                obj.addProperty("birthYear", rs.getInt("birthYear"));
                obj.addProperty("movie_id", rs.getString("movie_id"));
                obj.addProperty("movie_title", rs.getString("title"));
                obj.addProperty("movie_year", rs.getInt("year"));
                obj.addProperty("director", rs.getString("director"));
                jsonArray.add(obj);
            }

            rs.close();
            statement.close();
            out.write(jsonArray.toString());
            response.setStatus(200);
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("errorMessage", e.getMessage());
            out.write(error.toString());
            response.setStatus(500);
        } finally {
            out.close();
        }
    }
}

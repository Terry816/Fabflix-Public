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
import java.sql.CallableStatement;
import java.sql.Connection;

@WebServlet(name = "InsertMovieServlet", urlPatterns = "/api/insert-movie")
public class InsertMovieServlet extends HttpServlet {
    private DataSource dataSource;

    public void init() {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/readDB"); // moviedb as you said!
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        JsonObject responseJson = new JsonObject();

        String title = request.getParameter("movie_title");
        String year = request.getParameter("movie_year");
        String director = request.getParameter("movie_director");
        String starName = request.getParameter("star_name");
        String genreName = request.getParameter("genre_name");

        try (Connection conn = dataSource.getConnection()) {
            CallableStatement cs = conn.prepareCall("{CALL add_movie(?, ?, ?, ?, ?, ?, ?, ?, ?)}");

            cs.setString(1, title);
            cs.setInt(2, Integer.parseInt(year));
            cs.setString(3, director);
            cs.setString(4, starName);
            cs.setString(5, genreName);

            cs.registerOutParameter(6, java.sql.Types.VARCHAR); // movie_id
            cs.registerOutParameter(7, java.sql.Types.VARCHAR); // star_id
            cs.registerOutParameter(8, java.sql.Types.INTEGER); // genre_id
            cs.registerOutParameter(9, java.sql.Types.BOOLEAN); // already_exists

            cs.execute();

            boolean alreadyExists = cs.getBoolean(9);

            if (alreadyExists) {
                responseJson.addProperty("status", "fail");
                responseJson.addProperty("message", "Duplicate movie detected. Movie was not added.");
            } else {
                responseJson.addProperty("status", "success");
                responseJson.addProperty("message", "Movie added successfully!");
                responseJson.addProperty("movie_id", cs.getString(6));
                responseJson.addProperty("star_id", cs.getString(7));
                responseJson.addProperty("genre_id", cs.getInt(8));
            }

            cs.close();
        } catch (Exception e) {
            responseJson.addProperty("status", "fail");
            responseJson.addProperty("message", "Internal server error: " + e.getMessage());
            response.setStatus(500);
        }

        out.write(responseJson.toString());
        out.close();
    }
}

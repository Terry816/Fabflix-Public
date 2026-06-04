import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jasypt.util.password.StrongPasswordEncryptor;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "LoginServlet", urlPatterns = "/api/login")
public class LoginServlet extends BaseServlet {
    private javax.sql.DataSource dataSource;

    public void init() throws jakarta.servlet.ServletException {
        dataSource = lookupDataSource(READ_DB_JNDI);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String email = trimToNull(request.getParameter("username"));
        String password = trimToNull(request.getParameter("password"));
        if (email == null || password == null) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing email or password.");
            return;
        }

        JsonObject responseJsonObject = new JsonObject();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT id, firstName, password FROM customers WHERE email = ?")) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    if (passwordMatches(password, rs.getString("password"))) {
                        int userId = rs.getInt("id");
                        String firstName = rs.getString("firstName");

                        Map<String, Object> claims = new HashMap<>();
                        claims.put("userId", userId);
                        claims.put("firstName", firstName);

                        String jwt = JwtUtil.generateToken(email, claims);
                        JwtUtil.updateJwtCookie(request, response, jwt);

                        responseJsonObject.addProperty("status", "success");
                        responseJsonObject.addProperty("message", "Login successful.");
                    } else {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        responseJsonObject.addProperty("status", "fail");
                        responseJsonObject.addProperty("message", "Incorrect password.");
                    }
                } else {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    responseJsonObject.addProperty("status", "fail");
                    responseJsonObject.addProperty("message", "User not found.");
                }
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Internal server error.");
        }

        writeJson(response, responseJsonObject);
    }

    private boolean passwordMatches(String submittedPassword, String storedPassword) {
        if (submittedPassword.equals(storedPassword)) {
            return true;
        }

        try {
            return new StrongPasswordEncryptor().checkPassword(submittedPassword, storedPassword);
        } catch (Exception ignored) {
            return false;
        }
    }
}

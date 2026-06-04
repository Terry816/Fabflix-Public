import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "EmployeeLoginServlet", urlPatterns = "/api/employee-login")
public class EmployeeLoginServlet extends BaseServlet {
    private javax.sql.DataSource dataSource;

    public void init() throws jakarta.servlet.ServletException {
        dataSource = lookupDataSource(READ_DB_JNDI);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String email = trimToNull(request.getParameter("email"));
        String password = trimToNull(request.getParameter("password"));
        if (email == null || password == null) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing email or password.");
            return;
        }

        JsonObject responseJsonObject = new JsonObject();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement statement = conn.prepareStatement("SELECT password FROM employees WHERE email = ?")) {
            statement.setString(1, email);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next() && password.equals(rs.getString("password"))) {
                    request.getSession().setAttribute("employeeUser", email);
                    responseJsonObject.addProperty("status", "success");
                    responseJsonObject.addProperty("message", "success");
                } else {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    responseJsonObject.addProperty("status", "fail");
                    responseJsonObject.addProperty("message", "Invalid email or password.");
                }
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Internal server error.");
        }

        writeJson(response, responseJsonObject);
    }
}

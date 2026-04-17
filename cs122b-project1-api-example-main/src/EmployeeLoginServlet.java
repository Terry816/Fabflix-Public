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

@WebServlet(name = "EmployeeLoginServlet", urlPatterns = "/api/employee-login")
public class EmployeeLoginServlet extends HttpServlet {
    private DataSource dataSource;

    public void init() {
        try {
            System.out.println("[DEBUG] EmployeeLoginServlet init called.");
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/readDB");
            if (dataSource == null) {
                System.out.println("[ERROR] DataSource is NULL after lookup in init!");
            } else {
                System.out.println("[INFO] DataSource initialized successfully in init.");
            }
        } catch (NamingException e) {
            System.out.println("[ERROR] NamingException during datasource lookup: " + e.getMessage());
            e.printStackTrace();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        System.out.println("[DEBUG] EmployeeLoginServlet doPost called.");
        JsonObject responseJsonObject = new JsonObject();
        PrintWriter out = response.getWriter();

        try (Connection conn = dataSource.getConnection()) {
            String email = request.getParameter("email");
            String password = request.getParameter("password");

            System.out.println("[DEBUG] email entered = " + email);
            System.out.println("[DEBUG] password entered = " + password);

            String query = "SELECT password FROM employees WHERE email = ?";
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, email);

            ResultSet rs = statement.executeQuery();
            System.out.println("[DEBUG] Query executed.");

            if (rs.next()) {
                String dbPassword = rs.getString("password");
                System.out.println("[DEBUG] DB Password fetched = " + dbPassword);

                if (password.equals(dbPassword)) {
                    System.out.println("[DEBUG] Password matched. Employee login success.");
                    request.getSession().setAttribute("employeeUser", email);
                    responseJsonObject.addProperty("status", "success");
                    responseJsonObject.addProperty("message", "success");
                } else {
                    System.out.println("[DEBUG] Password mismatch.");
                    responseJsonObject.addProperty("status", "fail");
                    responseJsonObject.addProperty("message", "Invalid email or password.");
                }
            } else {
                System.out.println("[DEBUG] No employee found with that email.");
                responseJsonObject.addProperty("status", "fail");
                responseJsonObject.addProperty("message", "Invalid email or password.");
            }

            rs.close();
            statement.close();

        } catch (Exception e) {
            System.out.println("[ERROR] Exception in EmployeeLoginServlet: " + e.getMessage());
            e.printStackTrace();
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Internal server error: " + e.getMessage());
        }

        out.write(responseJsonObject.toString());
        out.close();
    }
}

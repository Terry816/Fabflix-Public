import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jasypt.util.password.StrongPasswordEncryptor;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "LoginServlet", urlPatterns = "/api/login")
public class LoginServlet extends HttpServlet {
    private DataSource dataSource;

    public void init() {
        try {
            InitialContext ctx = new InitialContext();
            dataSource = (DataSource) ctx.lookup("java:comp/env/jdbc/readDB");
            System.out.println("[DEBUG] LoginServlet initialized. DataSource lookup success.");
        } catch (NamingException e) {
            System.out.println("[ERROR] JNDI lookup failed.");
            e.printStackTrace();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        JsonObject responseJsonObject = new JsonObject();

        String email = request.getParameter("username");
        String password = request.getParameter("password");

        System.out.println("[DEBUG] Received login attempt:");
        System.out.println("  → username (email): " + email);
        System.out.println("  → password: " + (password != null ? "[REDACTED]" : "null"));

        if (email == null || password == null || email.trim().isEmpty() || password.trim().isEmpty()) {
            System.out.println("[WARN] Missing email or password.");
            response.setStatus(400);
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Missing email or password.");
            out.write(responseJsonObject.toString());
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            System.out.println("[DEBUG] Connected to DB.");
            String query = "SELECT id, firstName, password FROM customers WHERE email = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String encryptedPassword = rs.getString("password");
                boolean passwordMatch = new StrongPasswordEncryptor().checkPassword(password, encryptedPassword);
                System.out.println("[DEBUG] Password match: " + passwordMatch);

                if (passwordMatch) {
                    int userId = rs.getInt("id");
                    String firstName = rs.getString("firstName");

                    Map<String, Object> claims = new HashMap<>();
                    claims.put("userId", userId);
                    claims.put("firstName", firstName);

                    String jwt = JwtUtil.generateToken(email, claims);
                    JwtUtil.updateJwtCookie(request, response, jwt);

                    System.out.println("[DEBUG] JWT generated and cookie set for user: " + email);

                    responseJsonObject.addProperty("status", "success");
                    responseJsonObject.addProperty("message", "Login successful.");
                } else {
                    System.out.println("[WARN] Incorrect password.");
                    response.setStatus(401);
                    responseJsonObject.addProperty("status", "fail");
                    responseJsonObject.addProperty("message", "Incorrect password.");
                }
            } else {
                System.out.println("[WARN] User not found in database.");
                response.setStatus(401);
                responseJsonObject.addProperty("status", "fail");
                responseJsonObject.addProperty("message", "User not found.");
            }

            rs.close();
            stmt.close();
        } catch (Exception e) {
            System.out.println("[ERROR] Exception during login: " + e.getMessage());
            response.setStatus(500);
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Internal server error.");
        } finally {
            out.write(responseJsonObject.toString());
            out.close();
        }
    }
}

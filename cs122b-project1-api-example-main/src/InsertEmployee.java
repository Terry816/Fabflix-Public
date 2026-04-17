import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import org.jasypt.util.password.StrongPasswordEncryptor;

public class InsertEmployee {

    public static void main(String[] args) throws Exception {

        String loginUser = "mytestuser";
        String loginPasswd = "My6$Password";
        String loginUrl = "jdbc:mysql://localhost:3306/moviedb";

        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection connection = DriverManager.getConnection(loginUrl, loginUser, loginPasswd);

        // 2. Employee Info
        String email = "classta@email.edu";
        String plainPassword = "classta";
        String fullName = "TA CS122B";

        // 3. Encrypt the password
        StrongPasswordEncryptor passwordEncryptor = new StrongPasswordEncryptor();
        String encryptedPassword = passwordEncryptor.encryptPassword(plainPassword);

        // 4. Insert employee
        String insertQuery = "INSERT INTO employees (email, password, fullname) VALUES (?, ?, ?)";

        PreparedStatement statement = connection.prepareStatement(insertQuery);
        statement.setString(1, email);
        statement.setString(2, encryptedPassword);
        statement.setString(3, fullName);

        int rowsAffected = statement.executeUpdate();
        if (rowsAffected > 0) {
            System.out.println("Employee inserted successfully!");
        } else {
            System.out.println("Failed to insert employee.");
        }

        statement.close();
        connection.close();
    }
}

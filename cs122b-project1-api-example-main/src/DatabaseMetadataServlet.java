import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
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
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

@WebServlet(name = "DatabaseMetadataServlet", urlPatterns = "/api/database-metadata")
public class DatabaseMetadataServlet extends HttpServlet {
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
        PrintWriter out = response.getWriter();

        JsonObject responseJson = new JsonObject();

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();

            String[] types = {"TABLE"};

            // ✅ Only look inside the "moviedb" database
            ResultSet tables = meta.getTables("moviedb", null, "%", types);

            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                JsonArray columnsArray = new JsonArray();

                ResultSet columns = meta.getColumns("moviedb", null, tableName, "%");
                while (columns.next()) {
                    JsonObject columnObj = new JsonObject();
                    columnObj.addProperty("column_name", columns.getString("COLUMN_NAME"));
                    columnObj.addProperty("column_type", columns.getString("TYPE_NAME"));
                    columnsArray.add(columnObj);
                }
                columns.close();

                responseJson.add(tableName, columnsArray);
            }

            tables.close();
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("errorMessage", e.getMessage());
            out.write(error.toString());
            response.setStatus(500);
            return;
        }

        out.write(responseJson.toString());
        out.close();
    }

}

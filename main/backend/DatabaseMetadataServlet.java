import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

@WebServlet(name = "DatabaseMetadataServlet", urlPatterns = "/api/database-metadata")
public class DatabaseMetadataServlet extends BaseServlet {
    private javax.sql.DataSource dataSource;

    public void init() throws jakarta.servlet.ServletException {
        dataSource = lookupDataSource(READ_DB_JNDI);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject responseJson = new JsonObject();

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet tables = meta.getTables("moviedb", null, "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    JsonArray columnsArray = new JsonArray();

                    try (ResultSet columns = meta.getColumns("moviedb", null, tableName, "%")) {
                        while (columns.next()) {
                            JsonObject columnObj = new JsonObject();
                            columnObj.addProperty("column_name", columns.getString("COLUMN_NAME"));
                            columnObj.addProperty("column_type", columns.getString("TYPE_NAME"));
                            columnsArray.add(columnObj);
                        }
                    }

                    responseJson.add(tableName, columnsArray);
                }
            }
        } catch (Exception e) {
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            return;
        }

        writeJson(response, responseJson);
    }
}

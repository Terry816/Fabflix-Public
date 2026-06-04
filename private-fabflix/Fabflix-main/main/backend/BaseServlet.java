import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletResponse;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;

public abstract class BaseServlet extends HttpServlet {
    protected static final String READ_DB_JNDI = "java:comp/env/jdbc/readDB";
    protected static final String WRITE_DB_JNDI = "java:comp/env/jdbc/writeDB";

    protected DataSource lookupDataSource(String jndiName) throws ServletException {
        try {
            return (DataSource) new InitialContext().lookup(jndiName);
        } catch (NamingException e) {
            throw new ServletException("Unable to initialize datasource: " + jndiName, e);
        }
    }

    protected void writeJson(HttpServletResponse response, JsonElement payload) throws IOException {
        response.setContentType("application/json");
        response.getWriter().write(payload.toString());
    }

    protected void writeError(HttpServletResponse response, int statusCode, String message) throws IOException {
        response.setStatus(statusCode);
        JsonObject error = new JsonObject();
        error.addProperty("status", "fail");
        error.addProperty("message", message);
        writeJson(response, error);
    }

    protected void writeSuccess(HttpServletResponse response, String message) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("status", "success");
        body.addProperty("message", message);
        writeJson(response, body);
    }

    protected int getIntParameter(String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    protected String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    protected void disableCaching(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
    }
}

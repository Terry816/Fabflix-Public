import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

@WebServlet(name = "IndexServlet", urlPatterns = "/api/index")
public class IndexServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        response.setContentType("application/json");

        HttpSession session = request.getSession(false);
        JsonObject responseJsonObject = new JsonObject();

        if (session != null && session.getAttribute("user") != null) {
            String sessionId = session.getId();
            long lastAccessTime = session.getLastAccessedTime();

            responseJsonObject.addProperty("sessionID", sessionId);
            responseJsonObject.addProperty("lastAccessTime", new Date(lastAccessTime).toString());

            User user = (User) session.getAttribute("user");
            responseJsonObject.addProperty("firstName", user.getFirstName());

            ArrayList<String> previousItems = (ArrayList<String>) session.getAttribute("previousItems");
            if (previousItems == null) {
                previousItems = new ArrayList<>();
            }

            JsonArray previousItemsJsonArray = new JsonArray();
            previousItems.forEach(previousItemsJsonArray::add);
            responseJsonObject.add("previousItems", previousItemsJsonArray);
        } else {
            responseJsonObject.addProperty("sessionID", "");
        }

        response.getWriter().write(responseJsonObject.toString());
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String item = request.getParameter("item");
        HttpSession session = request.getSession();

        ArrayList<String> previousItems = (ArrayList<String>) session.getAttribute("previousItems");
        if (previousItems == null) {
            previousItems = new ArrayList<>();
            previousItems.add(item);
            session.setAttribute("previousItems", previousItems);
        } else {
            synchronized (previousItems) {
                previousItems.add(item);
            }
        }

        JsonObject responseJsonObject = new JsonObject();
        JsonArray previousItemsJsonArray = new JsonArray();
        previousItems.forEach(previousItemsJsonArray::add);
        responseJsonObject.add("previousItems", previousItemsJsonArray);

        response.getWriter().write(responseJsonObject.toString());
    }
}

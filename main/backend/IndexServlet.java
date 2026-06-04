import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@WebServlet(name = "IndexServlet", urlPatterns = "/api/index")
public class IndexServlet extends BaseServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        disableCaching(response);

        HttpSession session = request.getSession(false);
        JsonObject responseJsonObject = new JsonObject();

        if (session != null && session.getAttribute("user") != null) {
            responseJsonObject.addProperty("sessionID", session.getId());
            responseJsonObject.addProperty("lastAccessTime", new Date(session.getLastAccessedTime()).toString());

            User user = (User) session.getAttribute("user");
            responseJsonObject.addProperty("firstName", user.getFirstName());
            responseJsonObject.add("previousItems", toJsonArray(getPreviousItems(session)));
        } else {
            responseJsonObject.addProperty("sessionID", "");
        }

        writeJson(response, responseJsonObject);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String item = trimToNull(request.getParameter("item"));
        if (item == null) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing item.");
            return;
        }

        HttpSession session = request.getSession();
        List<String> previousItems = getPreviousItems(session);

        synchronized (previousItems) {
            previousItems.add(item);
        }
        session.setAttribute("previousItems", previousItems);

        JsonObject responseJsonObject = new JsonObject();
        responseJsonObject.add("previousItems", toJsonArray(previousItems));
        writeJson(response, responseJsonObject);
    }

    private List<String> getPreviousItems(HttpSession session) {
        List<String> previousItems = (List<String>) session.getAttribute("previousItems");
        return previousItems == null ? new ArrayList<>() : previousItems;
    }

    private JsonArray toJsonArray(List<String> items) {
        JsonArray array = new JsonArray();
        items.forEach(array::add);
        return array;
    }
}

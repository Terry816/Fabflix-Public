import com.google.gson.JsonObject;

import java.sql.ResultSet;
import java.sql.SQLException;

public final class MovieJsonMapper {
    private MovieJsonMapper() {
    }

    public static JsonObject fromResultSet(ResultSet rs) throws SQLException {
        JsonObject movie = new JsonObject();
        movie.addProperty("movie_id", rs.getString("movie_id"));
        movie.addProperty("title", rs.getString("title"));
        movie.addProperty("year", rs.getInt("year"));
        movie.addProperty("director", rs.getString("director"));
        movie.addProperty("rating", rs.getFloat("rating"));
        movie.addProperty("genres", nullToEmpty(rs.getString("genres")));
        movie.addProperty("stars", nullToEmpty(rs.getString("stars")));
        movie.addProperty("star_ids", nullToEmpty(rs.getString("star_ids")));
        return movie;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}

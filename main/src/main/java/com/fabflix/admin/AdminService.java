package com.fabflix.admin;

import com.fabflix.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {
    private final JdbcTemplate jdbcTemplate;

    public AdminService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, List<Map<String, String>>> databaseMetadata() {
        return jdbcTemplate.execute((ConnectionCallback<Map<String, List<Map<String, String>>>>) connection -> {
            Map<String, List<Map<String, String>>> metadata = new LinkedHashMap<>();
            DatabaseMetaData databaseMetaData = connection.getMetaData();

            try (ResultSet tables = databaseMetaData.getTables(null, "public", "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    List<Map<String, String>> columns = new java.util.ArrayList<>();

                    try (ResultSet resultSet = databaseMetaData.getColumns(null, "public", tableName, "%")) {
                        while (resultSet.next()) {
                            Map<String, String> column = new LinkedHashMap<>();
                            column.put("column_name", resultSet.getString("COLUMN_NAME"));
                            column.put("column_type", resultSet.getString("TYPE_NAME"));
                            columns.add(column);
                        }
                    }

                    metadata.put(tableName, columns);
                }
            }

            return metadata;
        });
    }

    public String insertGenre(String rawGenreName) {
        String genreName = requireValue(rawGenreName, "Missing genre name.");
        Integer existingId = queryOptionalInteger("SELECT id FROM genres WHERE name = ? LIMIT 1", genreName);
        if (existingId != null) {
            return "Genre already exists: " + genreName;
        }

        int rows = jdbcTemplate.update("INSERT INTO genres (name) VALUES (?)", genreName);
        if (rows == 0) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to add genre.");
        }
        return "Successfully added genre: " + genreName;
    }

    public Map<String, Object> insertStar(String rawStarName, String rawBirthYear) {
        String starName = requireValue(rawStarName, "Missing star name.");
        Integer birthYear = parseOptionalYear(rawBirthYear);
        String starId = nextNmStarId();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO stars (id, name, birthYear) VALUES (?, ?, ?)"
            );
            statement.setString(1, starId);
            statement.setString(2, starName);
            if (birthYear == null) {
                statement.setNull(3, Types.INTEGER);
            } else {
                statement.setInt(3, birthYear);
            }
            return statement;
        });

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Successfully added star: " + starName);
        result.put("star_id", starId);
        return result;
    }

    @Transactional
    public Map<String, Object> insertMovie(
            String rawTitle,
            String rawYear,
            String rawDirector,
            String rawStarName,
            String rawGenreName
    ) {
        String title = requireValue(rawTitle, "Missing required movie fields.");
        String director = requireValue(rawDirector, "Missing required movie fields.");
        String starName = requireValue(rawStarName, "Missing required movie fields.");
        String genreName = requireValue(rawGenreName, "Missing required movie fields.");
        int year = parseRequiredYear(rawYear);

        if (movieExists(title, year, director)) {
            throw new ApiException(HttpStatus.CONFLICT, "Duplicate movie detected. Movie was not added.");
        }

        String movieId = nextPrefixedId("movies", "K");
        jdbcTemplate.update(
                "INSERT INTO movies (id, title, year, director) VALUES (?, ?, ?, ?)",
                movieId,
                title,
                year,
                director
        );
        jdbcTemplate.update(
                "INSERT INTO ratings (movieId, rating, numVotes) VALUES (?, ?, 0)",
                movieId,
                Math.round(Math.random() * 100) / 10.0
        );

        String starId = findStarId(starName);
        if (starId == null) {
            starId = nextPrefixedId("stars", "S");
            jdbcTemplate.update("INSERT INTO stars (id, name) VALUES (?, ?)", starId, starName);
        }

        int genreId = findOrInsertGenre(genreName);
        jdbcTemplate.update(
                "INSERT INTO stars_in_movies (starId, movieId) VALUES (?, ?) ON CONFLICT DO NOTHING",
                starId,
                movieId
        );
        jdbcTemplate.update(
                "INSERT INTO genres_in_movies (genreId, movieId) VALUES (?, ?) ON CONFLICT DO NOTHING",
                genreId,
                movieId
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Movie added successfully!");
        result.put("movie_id", movieId);
        result.put("star_id", starId);
        result.put("genre_id", genreId);
        return result;
    }

    private boolean movieExists(String title, int year, String director) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM movies WHERE title = ? AND year = ? AND director = ?",
                Integer.class,
                title,
                year,
                director
        );
        return count != null && count > 0;
    }

    private String findStarId(String starName) {
        List<String> ids = jdbcTemplate.query(
                "SELECT id FROM stars WHERE name = ? LIMIT 1",
                ps -> ps.setString(1, starName),
                (rs, rowNum) -> rs.getString("id")
        );
        return ids.isEmpty() ? null : ids.get(0);
    }

    private int findOrInsertGenre(String genreName) {
        Integer existing = queryOptionalInteger("SELECT id FROM genres WHERE name = ? LIMIT 1", genreName);
        if (existing != null) {
            return existing;
        }
        Integer inserted = jdbcTemplate.queryForObject(
                "INSERT INTO genres (name) VALUES (?) RETURNING id",
                Integer.class,
                genreName
        );
        if (inserted == null) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to add genre.");
        }
        return inserted;
    }

    private String nextNmStarId() {
        Integer next = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(CAST(SUBSTRING(id FROM 3) AS INTEGER)), 0) + 1
                FROM stars
                WHERE id LIKE 'nm%'
                """, Integer.class);
        return String.format("nm%07d", next == null ? 1 : next);
    }

    private String nextPrefixedId(String table, String prefix) {
        if (!"movies".equals(table) && !"stars".equals(table)) {
            throw new IllegalArgumentException("Unsupported id table: " + table);
        }

        int substringStart = prefix.length() + 1;
        Integer next = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(CAST(SUBSTRING(id FROM " + substringStart + ") AS INTEGER)), 0) + 1 " +
                        "FROM " + table + " WHERE id LIKE ?",
                Integer.class,
                prefix + "%"
        );
        return prefix + (next == null ? 1 : next);
    }

    private Integer queryOptionalInteger(String sql, Object... params) {
        List<Integer> values = jdbcTemplate.query(sql, ps -> {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
        }, (rs, rowNum) -> rs.getInt(1));
        return values.isEmpty() ? null : values.get(0);
    }

    private int parseRequiredYear(String rawYear) {
        String year = requireValue(rawYear, "Missing required movie fields.");
        try {
            return Integer.parseInt(year);
        } catch (NumberFormatException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid movie year.");
        }
    }

    private Integer parseOptionalYear(String rawYear) {
        String year = trimToNull(rawYear);
        if (year == null) {
            return null;
        }
        try {
            return Integer.parseInt(year);
        } catch (NumberFormatException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid birth year.");
        }
    }

    private String requireValue(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, message);
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

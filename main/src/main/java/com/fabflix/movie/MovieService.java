package com.fabflix.movie;

import com.fabflix.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MovieService {
    private final JdbcTemplate jdbcTemplate;

    public MovieService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> searchMovies(MovieSearchRequest request) {
        boolean top20 = request.top20();
        String sort1 = MovieSearchSupport.sanitizeSortField(request.sort1(), "title");
        String dir1 = MovieSearchSupport.sanitizeSortDirection(request.dir1(), "asc");
        String sort2 = MovieSearchSupport.sanitizeSortField(request.sort2(), "rating");
        String dir2 = MovieSearchSupport.sanitizeSortDirection(request.dir2(), "asc");

        int page = Math.max(1, request.page());
        int pageSize = Math.min(100, Math.max(1, request.pageSize()));
        int offset = (page - 1) * pageSize;

        StringBuilder sql = new StringBuilder("""
                WITH star_popularity AS (
                    SELECT s.id, s.name, COUNT(sim.movieId) AS star_count
                    FROM stars s
                    JOIN stars_in_movies sim ON s.id = sim.starId
                    GROUP BY s.id, s.name
                ), top_stars AS (
                    SELECT sim.movieId,
                           STRING_AGG(sp.name, ', ' ORDER BY sp.star_count DESC, sp.name ASC, sp.id ASC) AS stars,
                           STRING_AGG(sp.id, ',' ORDER BY sp.star_count DESC, sp.name ASC, sp.id ASC) AS star_ids
                    FROM stars_in_movies sim
                    JOIN star_popularity sp ON sim.starId = sp.id
                    GROUP BY sim.movieId
                )
                SELECT m.id AS movie_id, m.title, m.year, m.director, r.rating,
                       STRING_AGG(DISTINCT g.name, ', ' ORDER BY g.name ASC) AS genres,
                       ts.stars, ts.star_ids
                FROM movies m
                LEFT JOIN ratings r ON m.id = r.movieId
                LEFT JOIN genres_in_movies gm ON m.id = gm.movieId
                LEFT JOIN genres g ON gm.genreId = g.id
                LEFT JOIN top_stars ts ON m.id = ts.movieId
                WHERE 1 = 1
                """);
        List<Object> params = new ArrayList<>();

        if (top20) {
            sql.append(" AND m.year >= ? AND r.rating IS NOT NULL ");
            params.add(parseOptionalYear(request.minYear(), 2015));
        } else {
            appendSearchFilters(sql, params, request);
        }

        sql.append(" GROUP BY m.id, m.title, m.year, m.director, r.rating, r.numVotes, ts.stars, ts.star_ids ");
        if (top20) {
            sql.append(MovieSearchSupport.buildModernRelevanceOrderClause()).append(" LIMIT 20");
        } else {
            sql.append(MovieSearchSupport.buildOrderClause(sort1, dir1, sort2, dir2));
            sql.append(" LIMIT ? OFFSET ? ");
            params.add(pageSize);
            params.add(offset);
        }

        return queryMovies(sql.toString(), params);
    }

    public List<Map<String, Object>> fullTextSearch(
            String rawQuery,
            String minYear,
            int page,
            int pageSize,
            String sort1,
            String dir1,
            String sort2,
            String dir2
    ) {
        String query = trimToNull(rawQuery);
        if (query == null) {
            return List.of();
        }

        int resolvedPage = Math.max(1, page);
        int resolvedPageSize = Math.min(100, Math.max(1, pageSize));
        int offset = (resolvedPage - 1) * resolvedPageSize;

        String safeSort1 = MovieSearchSupport.sanitizeSortField(sort1, "title");
        String safeDir1 = MovieSearchSupport.sanitizeSortDirection(dir1, "asc");
        String safeSort2 = MovieSearchSupport.sanitizeSortField(sort2, "rating");
        String safeDir2 = MovieSearchSupport.sanitizeSortDirection(dir2, "asc");
        Integer resolvedMinYear = parseOptionalYear(minYear);
        List<Object> params = new ArrayList<>();
        params.add("%" + query + "%");

        String sql = """
                WITH movie_genres AS (
                    SELECT gm.movieId,
                           STRING_AGG(g.name, ', ' ORDER BY g.name ASC) AS genres
                    FROM genres_in_movies gm
                    JOIN genres g ON gm.genreId = g.id
                    GROUP BY gm.movieId
                ), movie_stars AS (
                    SELECT sim.movieId,
                           STRING_AGG(s.name, ', ' ORDER BY s.name ASC, s.id ASC) AS stars,
                           STRING_AGG(s.id, ',' ORDER BY s.name ASC, s.id ASC) AS star_ids
                    FROM stars_in_movies sim
                    JOIN stars s ON sim.starId = s.id
                    GROUP BY sim.movieId
                )
                SELECT m.id AS movie_id, m.title, m.year, m.director, r.rating,
                       mg.genres, ms.stars, ms.star_ids
                FROM movies m
                JOIN ratings r ON m.id = r.movieId
                LEFT JOIN movie_genres mg ON m.id = mg.movieId
                LEFT JOIN movie_stars ms ON m.id = ms.movieId
                WHERE m.title ILIKE ?
                """;

        if (resolvedMinYear != null) {
            sql += " AND m.year >= ? ";
            params.add(resolvedMinYear);
        }

        sql += MovieSearchSupport.buildOrderClause(safeSort1, safeDir1, safeSort2, safeDir2) + " LIMIT ? OFFSET ?";
        params.add(resolvedPageSize);
        params.add(offset);

        return queryMovies(sql, params);
    }

    public List<Map<String, Object>> findMovieById(String id) {
        String movieId = trimToNull(id);
        if (movieId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Missing movie id.");
        }

        String sql = """
                WITH movie_genres AS (
                    SELECT gm.movieId,
                           STRING_AGG(g.name, ', ' ORDER BY g.name ASC) AS genres
                    FROM genres_in_movies gm
                    JOIN genres g ON gm.genreId = g.id
                    GROUP BY gm.movieId
                ), movie_stars AS (
                    SELECT sm.movieId,
                           STRING_AGG(s.name, ', ' ORDER BY s.name ASC, s.id ASC) AS stars,
                           STRING_AGG(s.id, ',' ORDER BY s.name ASC, s.id ASC) AS star_ids
                    FROM stars_in_movies sm
                    JOIN stars s ON sm.starId = s.id
                    GROUP BY sm.movieId
                )
                SELECT m.id AS movie_id, m.title, m.year, m.director, r.rating,
                       mg.genres, ms.stars, ms.star_ids
                FROM movies m
                JOIN ratings r ON m.id = r.movieId
                LEFT JOIN movie_genres mg ON m.id = mg.movieId
                LEFT JOIN movie_stars ms ON m.id = ms.movieId
                WHERE m.id = ?
                """;
        return queryMovies(sql, List.of(movieId));
    }

    public List<Map<String, Object>> findStarById(String id) {
        String starId = trimToNull(id);
        if (starId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Missing star id.");
        }

        String sql = """
                SELECT s.name AS star_name, s.birthYear, m.id AS movie_id, m.title, m.year, m.director
                FROM stars s
                JOIN stars_in_movies sim ON s.id = sim.starId
                JOIN movies m ON sim.movieId = m.id
                WHERE s.id = ?
                ORDER BY m.year DESC, m.title ASC
                """;

        return jdbcTemplate.query(sql, ps -> ps.setString(1, starId), (rs, rowNum) -> {
            Map<String, Object> starMovie = new LinkedHashMap<>();
            starMovie.put("star_name", rs.getString("star_name"));
            starMovie.put("birthYear", nullableInteger(rs, "birthYear"));
            starMovie.put("movie_id", rs.getString("movie_id"));
            starMovie.put("movie_title", rs.getString("title"));
            starMovie.put("movie_year", rs.getInt("year"));
            starMovie.put("director", rs.getString("director"));
            return starMovie;
        });
    }

    public List<Map<String, Object>> autocomplete(String rawQuery) {
        String query = trimToNull(rawQuery);
        if (query == null || query.length() < 3) {
            return List.of();
        }

        String sql = "SELECT id, title FROM movies WHERE title ILIKE ? ORDER BY title LIMIT 10";
        return jdbcTemplate.query(sql, ps -> ps.setString(1, "%" + query + "%"), (rs, rowNum) -> {
            Map<String, Object> suggestion = new LinkedHashMap<>();
            suggestion.put("value", rs.getString("title"));
            suggestion.put("data", Map.of("movieId", rs.getString("id")));
            return suggestion;
        });
    }

    public List<Map<String, Object>> genres() {
        return jdbcTemplate.query(
                "SELECT name FROM genres ORDER BY name ASC",
                (rs, rowNum) -> {
                    Map<String, Object> genre = new LinkedHashMap<>();
                    genre.put("name", rs.getString("name"));
                    return genre;
                }
        );
    }

    private void appendSearchFilters(StringBuilder sql, List<Object> params, MovieSearchRequest request) {
        String title = trimToNull(request.title());
        if (title != null) {
            sql.append(" AND m.title ILIKE ? ");
            params.add("%" + title + "%");
        }

        String year = trimToNull(request.year());
        if (year != null) {
            sql.append(" AND m.year = ? ");
            params.add(parseYear(year));
        } else {
            Integer minYear = parseOptionalYear(request.minYear());
            if (minYear != null) {
                sql.append(" AND m.year >= ? ");
                params.add(minYear);
            }
        }

        String director = trimToNull(request.director());
        if (director != null) {
            sql.append(" AND m.director ILIKE ? ");
            params.add("%" + director + "%");
        }

        String star = trimToNull(request.star());
        if (star != null) {
            sql.append("""
                     AND EXISTS (
                         SELECT 1
                         FROM stars_in_movies sim
                         JOIN stars s ON sim.starId = s.id
                         WHERE sim.movieId = m.id AND s.name ILIKE ?
                     )
                    """);
            params.add("%" + star + "%");
        }

        String genre = trimToNull(request.genre());
        if (genre != null) {
            sql.append("""
                     AND EXISTS (
                         SELECT 1
                         FROM genres_in_movies gm2
                         JOIN genres g2 ON gm2.genreId = g2.id
                         WHERE gm2.movieId = m.id AND g2.name = ?
                     )
                    """);
            params.add(genre);
        }

        String startsWith = trimToNull(request.startsWith());
        if (startsWith != null) {
            if ("*".equals(startsWith)) {
                sql.append(" AND m.title ~ '^[^a-zA-Z0-9]' ");
            } else {
                sql.append(" AND m.title ILIKE ? ");
                params.add(startsWith + "%");
            }
        }
    }

    private int parseYear(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid year.");
        }
    }

    private Integer parseOptionalYear(String value) {
        String year = trimToNull(value);
        return year == null ? null : parseYear(year);
    }

    private int parseOptionalYear(String value, int defaultValue) {
        Integer year = parseOptionalYear(value);
        return year == null ? defaultValue : year;
    }

    private List<Map<String, Object>> queryMovies(String sql, List<Object> params) {
        return jdbcTemplate.query(sql, ps -> {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
        }, this::mapMovie);
    }

    private Map<String, Object> mapMovie(ResultSet rs, int rowNum) throws SQLException {
        Map<String, Object> movie = new LinkedHashMap<>();
        movie.put("movie_id", rs.getString("movie_id"));
        movie.put("title", rs.getString("title"));
        movie.put("year", rs.getInt("year"));
        movie.put("director", rs.getString("director"));
        movie.put("rating", nullableDouble(rs, "rating"));
        movie.put("genres", rs.getString("genres"));
        movie.put("stars", rs.getString("stars"));
        movie.put("star_ids", rs.getString("star_ids"));
        return movie;
    }

    private Double nullableDouble(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    private Integer nullableInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

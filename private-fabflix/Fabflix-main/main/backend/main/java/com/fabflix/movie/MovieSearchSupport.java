package com.fabflix.movie;

import java.util.Map;

final class MovieSearchSupport {
    private static final String RELEVANCE_SCORE =
            "(COALESCE(r.rating, 0) * LN(COALESCE(r.numVotes, 0) + 10) + (m.year * 0.35))";
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "title", "m.title",
            "rating", "r.rating",
            "year", "m.year",
            "relevance", RELEVANCE_SCORE
    );

    private MovieSearchSupport() {
    }

    static String sanitizeSortField(String value, String defaultValue) {
        return SORT_FIELDS.containsKey(value) ? value : defaultValue;
    }

    static String sanitizeSortDirection(String value, String defaultValue) {
        if ("asc".equalsIgnoreCase(value) || "desc".equalsIgnoreCase(value)) {
            return value.toUpperCase();
        }
        return defaultValue.toUpperCase();
    }

    static String buildOrderClause(String sort1, String dir1, String sort2, String dir2) {
        String field1 = SORT_FIELDS.getOrDefault(sort1, SORT_FIELDS.get("title"));
        String field2 = SORT_FIELDS.getOrDefault(sort2, SORT_FIELDS.get("rating"));
        return " ORDER BY " + field1 + " " + dir1 + " NULLS LAST, " + field2 + " " + dir2 + " NULLS LAST ";
    }

    static String buildModernRelevanceOrderClause() {
        return " ORDER BY " + RELEVANCE_SCORE + " DESC NULLS LAST, m.year DESC NULLS LAST ";
    }
}

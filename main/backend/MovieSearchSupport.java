public final class MovieSearchSupport {
    private MovieSearchSupport() {
    }

    public static String sanitizeSortField(String field, String fallback) {
        if ("title".equalsIgnoreCase(field) || "rating".equalsIgnoreCase(field)) {
            return field.toLowerCase();
        }
        return fallback;
    }

    public static String sanitizeSortDirection(String direction, String fallback) {
        if ("desc".equalsIgnoreCase(direction)) {
            return "desc";
        }
        if ("asc".equalsIgnoreCase(direction)) {
            return "asc";
        }
        return fallback;
    }

    public static String buildOrderClause(String sort1, String dir1, String sort2, String dir2) {
        return " ORDER BY " + sort1 + " " + dir1 + ", " + sort2 + " " + dir2 + " ";
    }

    public static String buildBooleanMatchQuery(String rawQuery) {
        String[] tokens = rawQuery.trim().split("\\s+");
        StringBuilder matchQuery = new StringBuilder();
        for (String token : tokens) {
            matchQuery.append("+").append(token).append("* ");
        }
        return matchQuery.toString().trim();
    }
}

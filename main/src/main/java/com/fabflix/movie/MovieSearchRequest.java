package com.fabflix.movie;

public record MovieSearchRequest(
        boolean top20,
        String title,
        String year,
        String director,
        String star,
        String genre,
        String startsWith,
        String minYear,
        String sort1,
        String dir1,
        String sort2,
        String dir2,
        int page,
        int pageSize
) {
}

package com.fabflix.movie;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MovieController {
    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping("/search")
    public List<Map<String, Object>> search(
            @RequestParam(value = "top20", defaultValue = "false") boolean top20,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "year", required = false) String year,
            @RequestParam(value = "director", required = false) String director,
            @RequestParam(value = "star", required = false) String star,
            @RequestParam(value = "genre", required = false) String genre,
            @RequestParam(value = "startsWith", required = false) String startsWith,
            @RequestParam(value = "minYear", required = false) String minYear,
            @RequestParam(value = "sort1", defaultValue = "title") String sort1,
            @RequestParam(value = "dir1", defaultValue = "asc") String dir1,
            @RequestParam(value = "sort2", defaultValue = "rating") String sort2,
            @RequestParam(value = "dir2", defaultValue = "asc") String dir2,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize
    ) {
        return movieService.searchMovies(new MovieSearchRequest(
                top20, title, year, director, star, genre, startsWith, minYear, sort1, dir1, sort2, dir2, page, pageSize
        ));
    }

    @GetMapping("/fulltext-search")
    public List<Map<String, Object>> fullTextSearch(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "minYear", required = false) String minYear,
            @RequestParam(value = "sort1", defaultValue = "title") String sort1,
            @RequestParam(value = "dir1", defaultValue = "asc") String dir1,
            @RequestParam(value = "sort2", defaultValue = "rating") String sort2,
            @RequestParam(value = "dir2", defaultValue = "asc") String dir2,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize
    ) {
        return movieService.fullTextSearch(query, minYear, page, pageSize, sort1, dir1, sort2, dir2);
    }

    @GetMapping("/single-movie")
    public List<Map<String, Object>> singleMovie(@RequestParam("id") String id) {
        return movieService.findMovieById(id);
    }

    @GetMapping("/single-star")
    public List<Map<String, Object>> singleStar(@RequestParam("id") String id) {
        return movieService.findStarById(id);
    }

    @GetMapping("/autocomplete")
    public List<Map<String, Object>> autocomplete(@RequestParam(value = "query", required = false) String query) {
        return movieService.autocomplete(query);
    }

    @GetMapping("/genres")
    public List<Map<String, Object>> genres() {
        return movieService.genres();
    }
}

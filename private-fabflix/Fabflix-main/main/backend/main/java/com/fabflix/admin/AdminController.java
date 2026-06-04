package com.fabflix.admin;

import com.fabflix.common.ApiResponses;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/database-metadata")
    public Map<String, List<Map<String, String>>> databaseMetadata() {
        return adminService.databaseMetadata();
    }

    @PostMapping("/insert-genre")
    public Map<String, Object> insertGenre(@RequestParam("genre_name") String genreName) {
        return ApiResponses.success(adminService.insertGenre(genreName));
    }

    @PostMapping("/insert-star")
    public Map<String, Object> insertStar(
            @RequestParam("star_name") String starName,
            @RequestParam(value = "birth_year", required = false) String birthYear
    ) {
        Map<String, Object> result = adminService.insertStar(starName, birthYear);
        Map<String, Object> response = ApiResponses.success(String.valueOf(result.get("message")));
        response.put("star_id", result.get("star_id"));
        return response;
    }

    @PostMapping("/insert-movie")
    public Map<String, Object> insertMovie(
            @RequestParam("movie_title") String title,
            @RequestParam("movie_year") String year,
            @RequestParam("movie_director") String director,
            @RequestParam("star_name") String starName,
            @RequestParam("genre_name") String genreName
    ) {
        Map<String, Object> result = adminService.insertMovie(title, year, director, starName, genreName);
        Map<String, Object> response = ApiResponses.success(String.valueOf(result.get("message")));
        response.put("movie_id", result.get("movie_id"));
        response.put("star_id", result.get("star_id"));
        response.put("genre_id", result.get("genre_id"));
        return response;
    }
}

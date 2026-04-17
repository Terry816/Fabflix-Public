/**
 * index.js – for Fabflix landing page (index.html)
 * -----------------------------------------------
 * This script logs activity and is ready for future enhancements like:
 * - Displaying featured movies
 * - Showing user login state
 * - Rendering dynamic homepage content
 */

document.addEventListener("DOMContentLoaded", () => {
    console.log("Fabflix Home Page Loaded");

    // Example of future homepage features:
    // fetchFeaturedMovies();
});

function fetchFeaturedMovies() {
    jQuery.ajax({
        dataType: "json",
        method: "GET",
        url: "api/featured-movies",
        success: (data) => {
            console.log("Featured movies:", data);
        },
        error: (xhr, status, error) => {
            console.error("Failed to fetch featured movies:", error);
        }
    });
}

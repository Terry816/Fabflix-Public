function handleMovieData(resultData) {
    const movie = resultData[0];
    if (!movie) {
        jQuery("#movie_info").html("<p>Movie not found.</p>");
        return;
    }

    const infoSection = jQuery("#movie_info");
    infoSection.append(`
        <div class="movie-title">${movie["title"]}</div>
        <div class="metadata mt-2">
            <p><strong>Year:</strong> ${movie["year"]}</p>
            <p><strong>Director:</strong> ${movie["director"]}</p>
            <p><strong>Rating:</strong> ${movie["rating"]}</p>
        </div>
    `);

    const genreLinks = buildGenreLinks(movie["genres"], Number.MAX_SAFE_INTEGER);
    const starLinks = buildStarLinks(movie["stars"], movie["star_ids"], Number.MAX_SAFE_INTEGER);

    let rowHTML = "<tr>";
    rowHTML += "<td>" + genreLinks + "</td>";
    rowHTML += "<td>" + starLinks + "</td>";
    rowHTML += "</tr>";

    jQuery("#movie_detail_table").append(rowHTML);

    jQuery("#add-to-cart-btn").on("click", function () {
        addMovieToCart(movie["movie_id"])
            .done((response) => alert(response.message))
            .fail(() => alert("Failed to add movie to cart."));
    });
}

const movieId = getUrlParam("id");

jQuery.ajax({
    dataType: "json",
    method: "GET",
    url: `api/single-movie?id=${movieId}`,
    success: (data) => handleMovieData(data),
    error: () => jQuery("#movie_info").html("<p>Failed to load movie details.</p>")
});
